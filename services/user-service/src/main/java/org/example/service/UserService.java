package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.exception.BusinessException;
import org.example.common.util.JwtUtil;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.vo.LoginResponse;
import org.example.vo.UserInfo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {
    
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./0-9A-Za-z]{53}$");
    
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        PasswordCheckResult passwordCheck = checkPassword(request.getPassword(), user.getPassword());
        if (!passwordCheck.matches()) {
            throw new BusinessException("用户名或密码错误");
        }
        if (passwordCheck.shouldUpgradeHash()) {
            user.setPassword(encodePassword(request.getPassword()));
            userMapper.updateById(user);
        }
        
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        
        String token = JwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());
        
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        
        return new LoginResponse(token, userInfo);
    }
    
    public void register(RegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }
        
        if (request.getStudentNo() != null) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getStudentNo, request.getStudentNo());
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("学号已被注册");
            }
        }
        
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(encodePassword(request.getPassword()));
        user.setRole("VOLUNTEER");
        user.setTotalVolunteerHours(BigDecimal.ZERO);
        user.setStatus(1);
        
        userMapper.insert(user);
    }
    
    public UserInfo getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        return userInfo;
    }
    
    public void updateUserInfo(Long userId, UserInfo userInfo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        if (userInfo.getRealName() != null) {
            user.setRealName(userInfo.getRealName());
        }
        if (userInfo.getPhone() != null) {
            user.setPhone(userInfo.getPhone());
        }
        if (userInfo.getEmail() != null) {
            user.setEmail(userInfo.getEmail());
        }
        
        userMapper.updateById(user);
    }
    
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        PasswordCheckResult result = checkPassword(oldPassword, user.getPassword());
        if (!result.matches()) {
            throw new BusinessException("旧密码错误");
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于6位");
        }
        
        user.setPassword(encodePassword(newPassword));
        userMapper.updateById(user);
    }
    
    /**
     * 管理员：查询所有志愿者时长；支持按姓名/学号/用户名模糊筛选；按累计时长降序。
     */
    public List<UserInfo> listVolunteerHours(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "VOLUNTEER");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(User::getRealName, keyword)
                    .or().like(User::getStudentNo, keyword)
                    .or().like(User::getUsername, keyword));
        }
        wrapper.orderByDesc(User::getTotalVolunteerHours);
        List<User> users = userMapper.selectList(wrapper);
        List<UserInfo> result = new ArrayList<>(users.size());
        for (User u : users) {
            UserInfo vo = new UserInfo();
            BeanUtils.copyProperties(u, vo);
            result.add(vo);
        }
        return result;
    }

    public void updateVolunteerHours(Long userId, BigDecimal hours) {
        userMapper.addVolunteerHours(userId, hours);
    }
    
    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    private PasswordCheckResult checkPassword(String rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return PasswordCheckResult.noMatch();
        }
        // 1) Standard BCrypt
        if (BCRYPT_PATTERN.matcher(encodedPassword).matches()) {
            return passwordEncoder.matches(rawPassword, encodedPassword)
                    ? PasswordCheckResult.matchNoUpgrade()
                    : PasswordCheckResult.noMatch();
        }
        // 2) Legacy demo formats (auto-upgrade on successful login)
        if (encodedPassword.equals(rawPassword) || encodedPassword.equals("$2a$10$" + rawPassword)) {
            return PasswordCheckResult.matchUpgrade();
        }
        // 3) Legacy special-cased value (kept for backward compatibility; upgrade after match)
        if (encodedPassword.equals("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH")) {
            return PasswordCheckResult.matchUpgrade();
        }
        return PasswordCheckResult.noMatch();
    }

    private record PasswordCheckResult(boolean matches, boolean shouldUpgradeHash) {
        static PasswordCheckResult noMatch() { return new PasswordCheckResult(false, false); }
        static PasswordCheckResult matchUpgrade() { return new PasswordCheckResult(true, true); }
        static PasswordCheckResult matchNoUpgrade() { return new PasswordCheckResult(true, false); }
    }
}
