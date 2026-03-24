package org.example.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.example.common.result.Result;
import org.example.dto.ActivityCreateRequest;
import org.example.dto.AIGenerateRequest;
import org.example.service.AIService;
import org.example.service.ActivityService;
import org.example.vo.ActivityVO;
import org.example.vo.RegistrationVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/activity")
public class ActivityController {
    
    private final ActivityService activityService;
    private final AIService aiService;
    
    public ActivityController(ActivityService activityService, AIService aiService) {
        this.activityService = activityService;
        this.aiService = aiService;
    }
    
    @GetMapping("/list")
    public Result<IPage<ActivityVO>> listActivities(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String recruitmentPhase,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        IPage<ActivityVO> activities = activityService.listActivities(page, size, status, category, recruitmentPhase, userId);
        return Result.success(activities);
    }
    
    @PostMapping("/create")
    public Result<Void> createActivity(
            @RequestBody ActivityCreateRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("只有管理员才能创建活动");
        }
        
        activityService.createActivity(request, userId);
        return Result.success();
    }
    
    @PostMapping("/register/{activityId}")
    public Result<Void> registerActivity(
            @PathVariable Long activityId,
            @RequestHeader("X-User-Id") Long userId) {
        
        activityService.registerActivity(activityId, userId);
        return Result.success();
    }
    
    @GetMapping("/myRegistrations")
    public Result<List<RegistrationVO>> getMyRegistrations(
            @RequestHeader("X-User-Id") Long userId) {
        
        List<RegistrationVO> registrations = activityService.getUserRegistrations(userId);
        return Result.success(registrations);
    }

    /**
     * 管理员：全部报名列表；可选 activityId 筛选某一活动。
     * 须放在 /{id} 之前，且路径字面量 admin 不可被当作 Long 解析。
     */
    @GetMapping("/admin/registrations")
    public Result<List<RegistrationVO>> listAdminRegistrations(
            @RequestParam(required = false) Long activityId,
            @RequestHeader("X-User-Role") String role) {

        if (!"ADMIN".equals(role)) {
            return Result.forbidden("只有管理员才能查看报名列表");
        }
        return Result.success(activityService.listRegistrationsForAdmin(activityId));
    }

    /**
     * 管理员：指定活动的报名列表（与 API 文档路径一致）。
     */
    @GetMapping("/{activityId}/registrations")
    public Result<List<RegistrationVO>> listRegistrationsByActivity(
            @PathVariable Long activityId,
            @RequestHeader("X-User-Role") String role) {

        if (!"ADMIN".equals(role)) {
            return Result.forbidden("只有管理员才能查看报名列表");
        }
        return Result.success(activityService.listRegistrationsForAdmin(activityId));
    }
    
    @PostMapping("/confirmHours/{registrationId}")
    public Result<Void> confirmHours(
            @PathVariable Long registrationId,
            @RequestHeader("X-User-Role") String role) {
        
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("只有管理员才能核销时长");
        }
        
        activityService.confirmHours(registrationId);
        return Result.success();
    }
    
    @PostMapping("/ai/generate")
    public Result<String> generateDescription(
            @RequestBody AIGenerateRequest request,
            @RequestHeader("X-User-Role") String role) {
        
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("只有管理员才能使用AI生成功能");
        }
        
        String description = aiService.generateActivityDescription(request);
        return Result.success(description);
    }

    /**
     * 活动详情（须放在固定路径与 /{id}/registrations 之后，避免路径被错误匹配）。
     */
    @GetMapping("/{id}")
    public Result<ActivityVO> getActivityDetail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        ActivityVO activity = activityService.getActivityDetail(id, userId);
        return Result.success(activity);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteActivity(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {

        if (!"ADMIN".equals(role)) {
            return Result.forbidden("只有管理员才能删除活动");
        }
        activityService.deleteActivity(id);
        return Result.success();
    }
}
