package org.example.service;

import org.example.common.exception.BusinessException;
import org.example.dto.AdminUserPasswordResetRequest;
import org.example.dto.AdminUserRoleUpdateRequest;
import org.example.dto.AdminUserStatusUpdateRequest;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.vo.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void loginShouldUpgradeLegacyPasswordHash() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student01");
        user.setPassword("password123");
        user.setRole("VOLUNTEER");
        user.setStatus(1);

        LoginRequest request = new LoginRequest();
        request.setUsername("student01");
        request.setPassword("password123");

        when(userMapper.selectOne(any())).thenReturn(user);

        LoginResponse response = userService.login(request);

        assertNotNull(response.getToken());
        assertEquals("student01", response.getUserInfo().getUsername());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertNotEquals("password123", userCaptor.getValue().getPassword());
        assertEquals("VOLUNTEER", userCaptor.getValue().getRole());
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing-user");
        request.setPassword("password123");

        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.register(request));

        assertEquals("Username already exists", exception.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerShouldCreateVolunteerWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new-user");
        request.setPassword("password123");
        request.setRealName("New User");
        request.setStudentNo("2024001");

        when(userMapper.selectCount(any())).thenReturn(0L, 0L);

        userService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).insert(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("new-user", savedUser.getUsername());
        assertEquals("VOLUNTEER", savedUser.getRole());
        assertEquals(BigDecimal.ZERO, savedUser.getTotalVolunteerHours());
        assertNotNull(savedUser.getPassword());
        assertNotEquals("password123", savedUser.getPassword());
    }

    @Test
    void adminPasswordResetShouldEncodePassword() {
        User admin = user(1L, "admin", "ADMIN", 1);
        User target = user(2L, "student01", "VOLUNTEER", 1);
        AdminUserPasswordResetRequest request = new AdminUserPasswordResetRequest();
        request.setNewPassword("newPassword123");

        when(userMapper.selectById(1L)).thenReturn(admin);
        when(userMapper.selectById(2L)).thenReturn(target);

        userService.resetAdminUserPassword(1L, 2L, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(2L, userCaptor.getValue().getId());
        assertNotNull(userCaptor.getValue().getPassword());
        assertNotEquals("newPassword123", userCaptor.getValue().getPassword());
    }

    @Test
    void nonAdminShouldNotUpdateUserRole() {
        User operator = user(2L, "student01", "VOLUNTEER", 1);
        AdminUserRoleUpdateRequest request = new AdminUserRoleUpdateRequest();
        request.setRole("ADMIN");

        when(userMapper.selectById(2L)).thenReturn(operator);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateAdminUserRole(2L, 3L, request)
        );

        assertEquals(403, exception.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void adminRoleUpdateShouldBlockRemovingOwnAdminRole() {
        User admin = user(1L, "admin", "ADMIN", 1);
        AdminUserRoleUpdateRequest request = new AdminUserRoleUpdateRequest();
        request.setRole("VOLUNTEER");

        when(userMapper.selectById(1L)).thenReturn(admin, admin);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateAdminUserRole(1L, 1L, request)
        );

        assertEquals(403, exception.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void adminStatusUpdateShouldBlockDisablingOwnAccount() {
        User admin = user(1L, "admin", "ADMIN", 1);
        AdminUserStatusUpdateRequest request = new AdminUserStatusUpdateRequest();
        request.setStatus(0);

        when(userMapper.selectById(1L)).thenReturn(admin, admin);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateAdminUserStatus(1L, 1L, request)
        );

        assertEquals(403, exception.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    private User user(Long id, String username, String role, Integer status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("$2a$10$0QNp9cTcnPH3.VA0nRZSMOOUXv2B.cLrH.YLufbmm4iSHh.VIqoqu");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
