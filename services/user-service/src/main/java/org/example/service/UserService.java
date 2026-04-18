package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.exception.BusinessException;
import org.example.common.util.JwtUtil;
import org.example.dto.InternalUserSummary;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.vo.LoginResponse;
import org.example.vo.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./0-9A-Za-z]{53}$");

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            log.warn("login failed because user does not exist username={}", request.getUsername());
            throw new BusinessException("Username or password is incorrect");
        }

        PasswordCheckResult passwordCheck = checkPassword(request.getPassword(), user.getPassword());
        if (!passwordCheck.matches()) {
            log.warn("login failed because password mismatch username={} userId={}", user.getUsername(), user.getId());
            throw new BusinessException("Username or password is incorrect");
        }
        if (passwordCheck.shouldUpgradeHash()) {
            user.setPassword(encodePassword(request.getPassword()));
            userMapper.updateById(user);
            log.info("upgraded legacy password hash userId={}", user.getId());
        }

        if (user.getStatus() == 0) {
            log.warn("login blocked because account disabled userId={} username={}", user.getId(), user.getUsername());
            throw new BusinessException("Account disabled");
        }

        String token = JwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole());

        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(user, userInfo);

        log.info("login succeeded userId={} username={} role={}", user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, userInfo);
    }

    public void register(RegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            log.warn("register failed because username already exists username={}", request.getUsername());
            throw new BusinessException("Username already exists");
        }

        if (request.getStudentNo() != null) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getStudentNo, request.getStudentNo());
            if (userMapper.selectCount(wrapper) > 0) {
                log.warn("register failed because studentNo already exists studentNo={}", request.getStudentNo());
                throw new BusinessException("Student number already registered");
            }
        }

        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(encodePassword(request.getPassword()));
        user.setRole("VOLUNTEER");
        user.setTotalVolunteerHours(BigDecimal.ZERO);
        user.setStatus(1);

        userMapper.insert(user);
        log.info("registered new user userId={} username={} role={}", user.getId(), user.getUsername(), user.getRole());
    }

    public UserInfo getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        return userInfo;
    }

    public void updateUserInfo(Long userId, UserInfo userInfo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
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
        log.info("updated user profile userId={}", userId);
    }

    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        PasswordCheckResult result = checkPassword(oldPassword, user.getPassword());
        if (!result.matches()) {
            throw new BusinessException("Old password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("New password must be at least 6 characters");
        }

        user.setPassword(encodePassword(newPassword));
        userMapper.updateById(user);
        log.info("updated user password userId={}", userId);
    }

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
        log.info("queried volunteer hours keyword={} resultCount={}", keyword, result.size());
        return result;
    }

    public List<InternalUserSummary> listUserSummariesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> normalizedIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = new LinkedHashMap<>();
        for (User user : userMapper.selectBatchIds(normalizedIds)) {
            if (user != null && user.getId() != null) {
                userMap.put(user.getId(), user);
            }
        }

        List<InternalUserSummary> summaries = new ArrayList<>(userMap.size());
        for (Long id : new LinkedHashSet<>(normalizedIds)) {
            User user = userMap.get(id);
            if (user == null) {
                continue;
            }
            InternalUserSummary summary = new InternalUserSummary();
            summary.setId(user.getId());
            summary.setUsername(user.getUsername());
            summary.setRealName(user.getRealName());
            summary.setStudentNo(user.getStudentNo());
            summary.setPhone(user.getPhone());
            summaries.add(summary);
        }
        log.info("queried internal user summaries requestCount={} resultCount={}", normalizedIds.size(), summaries.size());
        return summaries;
    }

    public void updateVolunteerHours(Long userId, BigDecimal hours) {
        userMapper.addVolunteerHours(userId, hours);
        log.info("updated volunteer hours userId={} hours={}", userId, hours);
    }

    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    private PasswordCheckResult checkPassword(String rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return PasswordCheckResult.noMatch();
        }
        if (BCRYPT_PATTERN.matcher(encodedPassword).matches()) {
            return passwordEncoder.matches(rawPassword, encodedPassword)
                    ? PasswordCheckResult.matchNoUpgrade()
                    : PasswordCheckResult.noMatch();
        }
        if (encodedPassword.equals(rawPassword) || encodedPassword.equals("$2a$10$" + rawPassword)) {
            return PasswordCheckResult.matchUpgrade();
        }
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
