package org.example.controller;

import org.example.common.result.Result;
import org.example.dto.AdminUserPasswordResetRequest;
import org.example.dto.AdminUserProfileUpdateRequest;
import org.example.dto.AdminUserRoleUpdateRequest;
import org.example.dto.AdminUserStatusUpdateRequest;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.service.UserService;
import org.example.vo.AdminUserInfo;
import org.example.vo.AdminUserPage;
import org.example.vo.LoginResponse;
import org.example.vo.UserInfo;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

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
            @RequestHeader("X-User-Id") Long operatorId) {
        return Result.success(userService.listVolunteerHours(operatorId, keyword));
    }

    @GetMapping("/admin/users")
    public Result<AdminUserPage> listAdminUsers(
            @RequestHeader("X-User-Id") Long operatorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return Result.success(userService.listAdminUsers(operatorId, keyword, role, status, page, size));
    }

    @GetMapping("/admin/users/{targetUserId}")
    public Result<AdminUserInfo> getAdminUserDetail(
            @RequestHeader("X-User-Id") Long operatorId,
            @PathVariable Long targetUserId) {
        return Result.success(userService.getAdminUserDetail(operatorId, targetUserId));
    }

    @PutMapping("/admin/users/{targetUserId}/profile")
    public Result<AdminUserInfo> updateAdminUserProfile(
            @RequestHeader("X-User-Id") Long operatorId,
            @PathVariable Long targetUserId,
            @RequestBody AdminUserProfileUpdateRequest request) {
        return Result.success(userService.updateAdminUserProfile(operatorId, targetUserId, request));
    }

    @PutMapping("/admin/users/{targetUserId}/password")
    public Result<Void> resetAdminUserPassword(
            @RequestHeader("X-User-Id") Long operatorId,
            @PathVariable Long targetUserId,
            @RequestBody AdminUserPasswordResetRequest request) {
        userService.resetAdminUserPassword(operatorId, targetUserId, request);
        return Result.success();
    }

    @PutMapping("/admin/users/{targetUserId}/role")
    public Result<AdminUserInfo> updateAdminUserRole(
            @RequestHeader("X-User-Id") Long operatorId,
            @PathVariable Long targetUserId,
            @RequestBody AdminUserRoleUpdateRequest request) {
        return Result.success(userService.updateAdminUserRole(operatorId, targetUserId, request));
    }

    @PutMapping("/admin/users/{targetUserId}/status")
    public Result<AdminUserInfo> updateAdminUserStatus(
            @RequestHeader("X-User-Id") Long operatorId,
            @PathVariable Long targetUserId,
            @RequestBody AdminUserStatusUpdateRequest request) {
        return Result.success(userService.updateAdminUserStatus(operatorId, targetUserId, request));
    }
}
