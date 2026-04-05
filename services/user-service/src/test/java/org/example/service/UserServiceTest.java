package org.example.service;

import org.example.common.exception.BusinessException;
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
}
