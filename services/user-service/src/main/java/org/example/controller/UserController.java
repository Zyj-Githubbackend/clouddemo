package org.example.controller;

import org.example.common.result.Result;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.service.UserService;
import org.example.vo.LoginResponse;
import org.example.vo.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<UserInfo> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        UserInfo userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    @PutMapping("/update")
    public Result<Void> updateUserInfo(
            @RequestBody UserInfo userInfo,
            @RequestHeader("X-User-Id") Long userId) {
        userService.updateUserInfo(userId, userInfo);
        return Result.success();
    }

    @PutMapping("/updatePassword")
    public Result<Void> updatePassword(
            @RequestBody Map<String, String> params,
            @RequestHeader("X-User-Id") Long userId) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        userService.updatePassword(userId, oldPassword, newPassword);
        return Result.success();
    }

    @GetMapping("/admin/hours")
    public Result<List<UserInfo>> listVolunteerHours(
            @RequestParam(required = false) String keyword,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) {
            log.warn("rejected volunteer hours query for non-admin role={}", role);
            return Result.forbidden("Only admin can view volunteer hours");
        }
        return Result.success(userService.listVolunteerHours(keyword));
    }
}
