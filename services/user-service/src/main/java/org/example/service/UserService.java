package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.exception.BusinessException;
import org.example.common.util.JwtUtil;
import org.example.dto.AdminUserPasswordResetRequest;
import org.example.dto.AdminUserProfileUpdateRequest;
import org.example.dto.AdminUserRoleUpdateRequest;
import org.example.dto.AdminUserStatusUpdateRequest;
import org.example.dto.InternalUserSummary;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.vo.AdminUserInfo;
import org.example.vo.AdminUserPage;
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
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_VOLUNTEER = "VOLUNTEER";
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

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
        user.setRole(ROLE_VOLUNTEER);
        user.setTotalVolunteerHours(BigDecimal.ZERO);
        user.setStatus(STATUS_ENABLED);

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

    public List<UserInfo> listVolunteerHours(Long operatorId, String keyword) {
        requireAdmin(operatorId);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, ROLE_VOLUNTEER);
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

    public AdminUserPage listAdminUsers(Long operatorId,
                                        String keyword,
                                        String role,
                                        Integer status,
                                        Integer page,
                                        Integer size) {
        requireAdmin(operatorId);

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        String normalizedRole = normalizeOptionalRole(role);
        validateOptionalStatus(status);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w
                    .like(User::getUsername, value)
                    .or().like(User::getRealName, value)
                    .or().like(User::getStudentNo, value)
                    .or().like(User::getPhone, value)
                    .or().like(User::getEmail, value));
        }
        if (normalizedRole != null) {
            wrapper.eq(User::getRole, normalizedRole);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);

        List<User> users = userMapper.selectList(wrapper);
        int total = users.size();
        int fromIndex = Math.min((normalizedPage - 1) * normalizedSize, total);
        int toIndex = Math.min(fromIndex + normalizedSize, total);
        List<AdminUserInfo> records = users.subList(fromIndex, toIndex)
                .stream()
                .map(this::toAdminUserInfo)
                .toList();

        log.info("admin listed users operatorId={} keyword={} role={} status={} page={} size={} total={}",
                operatorId, keyword, normalizedRole, status, normalizedPage, normalizedSize, total);
        return new AdminUserPage(records, total, normalizedPage, normalizedSize);
    }

    public AdminUserInfo getAdminUserDetail(Long operatorId, Long targetUserId) {
        requireAdmin(operatorId);
        User target = requireUser(targetUserId);
        return toAdminUserInfo(target);
    }

    public AdminUserInfo updateAdminUserProfile(Long operatorId,
                                                Long targetUserId,
                                                AdminUserProfileUpdateRequest request) {
        requireAdmin(operatorId);
        User target = requireUser(targetUserId);
        if (request == null) {
            throw new BusinessException("Request body is required");
        }

        if (request.getUsername() != null) {
            String username = requireText(request.getUsername(), "Username");
            if (!username.equals(target.getUsername())) {
                ensureUsernameAvailable(username, target.getId());
                target.setUsername(username);
            }
        }
        if (request.getRealName() != null) {
            target.setRealName(requireText(request.getRealName(), "Real name"));
        }
        if (request.getStudentNo() != null) {
            String studentNo = normalizeNullableText(request.getStudentNo());
            if (studentNo != null && !studentNo.equals(target.getStudentNo())) {
                ensureStudentNoAvailable(studentNo, target.getId());
            }
            target.setStudentNo(studentNo);
        }
        if (request.getPhone() != null) {
            target.setPhone(normalizeNullableText(request.getPhone()));
        }
        if (request.getEmail() != null) {
            target.setEmail(normalizeNullableText(request.getEmail()));
        }

        userMapper.updateById(target);
        log.info("admin updated user profile operatorId={} targetUserId={}", operatorId, targetUserId);
        return toAdminUserInfo(target);
    }

    public void resetAdminUserPassword(Long operatorId,
                                       Long targetUserId,
                                       AdminUserPasswordResetRequest request) {
        requireAdmin(operatorId);
        User target = requireUser(targetUserId);
        if (request == null) {
            throw new BusinessException("Request body is required");
        }
        validatePassword(request.getNewPassword(), "New password");

        target.setPassword(encodePassword(request.getNewPassword()));
        userMapper.updateById(target);
        log.info("admin reset user password operatorId={} targetUserId={}", operatorId, targetUserId);
    }

    public AdminUserInfo updateAdminUserRole(Long operatorId,
                                             Long targetUserId,
                                             AdminUserRoleUpdateRequest request) {
        requireAdmin(operatorId);
        User target = requireUser(targetUserId);
        if (request == null) {
            throw new BusinessException("Request body is required");
        }
        String role = normalizeRequiredRole(request.getRole());
        if (operatorId.equals(targetUserId) && !ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "Cannot remove your own admin role");
        }
        if (ROLE_ADMIN.equals(target.getRole()) && !ROLE_ADMIN.equals(role)) {
            ensureAnotherEnabledAdminExists(targetUserId, "Cannot remove the last enabled admin");
        }

        target.setRole(role);
        userMapper.updateById(target);
        log.info("admin updated user role operatorId={} targetUserId={} role={}", operatorId, targetUserId, role);
        return toAdminUserInfo(target);
    }

    public AdminUserInfo updateAdminUserStatus(Long operatorId,
                                               Long targetUserId,
                                               AdminUserStatusUpdateRequest request) {
        requireAdmin(operatorId);
        User target = requireUser(targetUserId);
        if (request == null) {
            throw new BusinessException("Request body is required");
        }
        Integer status = request.getStatus();
        validateRequiredStatus(status);
        if (operatorId.equals(targetUserId) && status == STATUS_DISABLED) {
            throw new BusinessException(403, "Cannot disable your own account");
        }
        if (ROLE_ADMIN.equals(target.getRole())
                && target.getStatus() != null
                && target.getStatus() == STATUS_ENABLED
                && status == STATUS_DISABLED) {
            ensureAnotherEnabledAdminExists(targetUserId, "Cannot disable the last enabled admin");
        }

        target.setStatus(status);
        userMapper.updateById(target);
        log.info("admin updated user status operatorId={} targetUserId={} status={}", operatorId, targetUserId, status);
        return toAdminUserInfo(target);
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

    private User requireAdmin(Long operatorId) {
        User operator = requireUser(operatorId);
        if (operator.getStatus() == null || operator.getStatus() != STATUS_ENABLED || !ROLE_ADMIN.equals(operator.getRole())) {
            log.warn("admin operation rejected operatorId={} role={} status={}",
                    operatorId, operator.getRole(), operator.getStatus());
            throw new BusinessException(403, "Only enabled admin can perform this operation");
        }
        return operator;
    }

    private User requireUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("User not found");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }
        return user;
    }

    private AdminUserInfo toAdminUserInfo(User user) {
        AdminUserInfo info = new AdminUserInfo();
        BeanUtils.copyProperties(user, info);
        return info;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeRequiredRole(String role) {
        String value = requireText(role, "Role").toUpperCase();
        if (!ROLE_ADMIN.equals(value) && !ROLE_VOLUNTEER.equals(value)) {
            throw new BusinessException("Role must be ADMIN or VOLUNTEER");
        }
        return value;
    }

    private String normalizeOptionalRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }
        return normalizeRequiredRole(role);
    }

    private void validateRequiredStatus(Integer status) {
        if (status == null || (status != STATUS_DISABLED && status != STATUS_ENABLED)) {
            throw new BusinessException("Status must be 0 or 1");
        }
    }

    private void validateOptionalStatus(Integer status) {
        if (status != null) {
            validateRequiredStatus(status);
        }
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validatePassword(String password, String fieldName) {
        if (password == null || password.length() < 6) {
            throw new BusinessException(fieldName + " must be at least 6 characters");
        }
    }

    private void ensureUsernameAvailable(String username, Long currentUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (currentUserId != null) {
            wrapper.ne(User::getId, currentUserId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("Username already exists");
        }
    }

    private void ensureStudentNoAvailable(String studentNo, Long currentUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStudentNo, studentNo);
        if (currentUserId != null) {
            wrapper.ne(User::getId, currentUserId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("Student number already registered");
        }
    }

    private void ensureAnotherEnabledAdminExists(Long targetUserId, String message) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, ROLE_ADMIN)
                .eq(User::getStatus, STATUS_ENABLED)
                .ne(User::getId, targetUserId);
        if (userMapper.selectCount(wrapper) <= 0) {
            throw new BusinessException(403, message);
        }
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
