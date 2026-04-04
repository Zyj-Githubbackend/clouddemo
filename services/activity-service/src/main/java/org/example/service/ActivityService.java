package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.constant.RedisKeyConstant;
import org.example.common.exception.BusinessException;
import org.example.dto.ActivityCreateRequest;
import org.example.dto.ActivityRegisteredCount;
import org.example.entity.Activity;
import org.example.entity.Registration;
import org.example.feign.UserServiceClient;
import org.example.mapper.ActivityMapper;
import org.example.mapper.RegistrationMapper;
import org.example.vo.ActivityVO;
import org.example.vo.RegistrationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ActivityService {
    
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final StringRedisTemplate redisTemplate;
    private final UserServiceClient userServiceClient;
    private final MinioStorageService minioStorageService;
    
    public ActivityService(ActivityMapper activityMapper, 
                          RegistrationMapper registrationMapper,
                          StringRedisTemplate redisTemplate,
                          UserServiceClient userServiceClient,
                          MinioStorageService minioStorageService) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.redisTemplate = redisTemplate;
        this.userServiceClient = userServiceClient;
        this.minioStorageService = minioStorageService;
    }
    
    public IPage<ActivityVO> listActivities(Integer page, Integer size, String status, String category,
                                            String recruitmentPhase, Long userId) {
        QueryWrapper<Activity> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<Activity> wrapper = queryWrapper.lambda();

        if (StringUtils.hasText(status)) {
            wrapper.eq(Activity::getStatus, status);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Activity::getCategory, category);
        }
        if (StringUtils.hasText(recruitmentPhase)) {
            LocalDateTime now = LocalDateTime.now();
            List<String> terminal = Arrays.asList("COMPLETED", "CANCELLED");
            switch (recruitmentPhase) {
                case "NOT_STARTED" ->
                    wrapper.gt(Activity::getRegistrationStartTime, now).notIn(Activity::getStatus, terminal);
                case "RECRUITING" ->
                    wrapper.le(Activity::getRegistrationStartTime, now)
                            .ge(Activity::getRegistrationDeadline, now)
                            .notIn(Activity::getStatus, terminal);
                case "ENDED" ->
                    wrapper.and(w -> w.lt(Activity::getRegistrationDeadline, now)
                            .or()
                            .in(Activity::getStatus, terminal));
                default -> { /* ignore unknown */ }
            }
        }

        applyActivityListOrdering(queryWrapper, status);

        // MyBatis-Plus 3.5.9 将 PaginationInnerInterceptor 移至独立的 jsqlparser 模块，
        // 本项目未引入该模块，故通过 selectCount + last(LIMIT/OFFSET) 手动实现分页，
        // 等效于开启分页插件后的行为。
        long total = activityMapper.selectCount(queryWrapper);

        int offset = (page - 1) * size;
        queryWrapper.last("LIMIT " + size + " OFFSET " + offset);
        List<Activity> records = activityMapper.selectList(queryWrapper);

        Page<Activity> activityPage = new Page<>(page, size, total);
        activityPage.setRecords(records);

        syncParticipantCountsWithRegistrations(activityPage.getRecords());

        return activityPage.convert(activity -> {
            ActivityVO vo = toActivityVO(activity);

            if (userId != null) {
                LambdaQueryWrapper<Registration> regWrapper = new LambdaQueryWrapper<>();
                regWrapper.eq(Registration::getUserId, userId)
                         .eq(Registration::getActivityId, activity.getId())
                         .eq(Registration::getStatus, "REGISTERED");
                vo.setIsRegistered(registrationMapper.selectCount(regWrapper) > 0);
            }

            return vo;
        });
    }
    
    public ActivityVO getActivityDetail(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        reconcileActivityRegistrationStats(activity, countRegisteredForActivity(activityId));

        ActivityVO vo = toActivityVO(activity);
        
        if (userId != null) {
            LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Registration::getUserId, userId)
                   .eq(Registration::getActivityId, activityId)
                   .eq(Registration::getStatus, "REGISTERED");
            vo.setIsRegistered(registrationMapper.selectCount(wrapper) > 0);
        }
        
        return vo;
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void createActivity(ActivityCreateRequest request, Long creatorId) {
        ActivityScheduleValidator.validate(request);
        Activity activity = new Activity();
        BeanUtils.copyProperties(request, activity);
        activity.setCreatorId(creatorId);
        activity.setCurrentParticipants(0);
        activity.setStatus("RECRUITING");
        
        activityMapper.insert(activity);
        
        String stockKey = RedisKeyConstant.getActivityStockKey(activity.getId());
        redisTemplate.opsForValue().set(stockKey, 
            String.valueOf(activity.getMaxParticipants()), 
            7, TimeUnit.DAYS);
    }

    /**
     * 管理员更新活动（未结项且未取消）。请求体字段与创建活动一致。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateActivity(Long activityId, ActivityCreateRequest request) {
        Activity existing = activityMapper.selectById(activityId);
        if (existing == null) {
            throw new BusinessException("活动不存在");
        }
        if (isTerminalOrCancelled(existing.getStatus())) {
            throw new BusinessException("已结项或已取消的活动不可编辑");
        }
        ActivityScheduleValidator.validate(request);
        if (request.getMaxParticipants() < existing.getCurrentParticipants()) {
            throw new BusinessException("招募人数不能小于当前报名人数");
        }
        Long creatorId = existing.getCreatorId();
        String oldImageKey = existing.getImageKey();
        BeanUtils.copyProperties(request, existing);
        existing.setId(activityId);
        existing.setCreatorId(creatorId);
        activityMapper.updateById(existing);
        reconcileActivityRegistrationStats(existing, countRegisteredForActivity(activityId));
        if (StringUtils.hasText(oldImageKey) && !oldImageKey.equals(existing.getImageKey())) {
            minioStorageService.deleteObjectQuietly(oldImageKey);
        }
    }

    /**
     * 取消活动：删除全部报名流水，状态置为 CANCELLED，人数清零并重算 Redis。
     * 若存在已核销记录，用户累计志愿时长不会回滚（与物理删活动一致）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelActivity(Long activityId) {
        Activity existing = activityMapper.selectById(activityId);
        if (existing == null) {
            throw new BusinessException("活动不存在");
        }
        if ("COMPLETED".equals(existing.getStatus())) {
            throw new BusinessException("已结项的活动无法取消");
        }
        if ("CANCELLED".equals(existing.getStatus())) {
            throw new BusinessException("活动已取消");
        }
        LambdaQueryWrapper<Registration> regWrapper = new LambdaQueryWrapper<>();
        regWrapper.eq(Registration::getActivityId, activityId);
        registrationMapper.delete(regWrapper);
        existing.setStatus("CANCELLED");
        existing.setCurrentParticipants(0);
        activityMapper.updateById(existing);
        reconcileActivityRegistrationStats(existing, 0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeActivity(Long activityId) {
        Activity existing = activityMapper.selectById(activityId);
        if (existing == null) {
            throw new BusinessException("活动不存在");
        }
        if ("COMPLETED".equals(existing.getStatus())) {
            throw new BusinessException("活动已结项");
        }
        if ("CANCELLED".equals(existing.getStatus())) {
            throw new BusinessException("已取消的活动无法结项");
        }
        existing.setStatus("COMPLETED");
        activityMapper.updateById(existing);
        int n = countRegisteredForActivity(activityId);
        reconcileActivityRegistrationStats(existing, n);
    }

    private static boolean isTerminalOrCancelled(String status) {
        return "COMPLETED".equals(status) || "CANCELLED".equals(status);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void registerActivity(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        
        if (!"RECRUITING".equals(activity.getStatus())) {
            throw new BusinessException("活动当前不在招募状态");
        }
        
        if (activity.getRegistrationStartTime() != null
                && LocalDateTime.now().isBefore(activity.getRegistrationStartTime())) {
            throw new BusinessException("志愿招募尚未开始");
        }
        if (LocalDateTime.now().isAfter(activity.getRegistrationDeadline())) {
            throw new BusinessException("报名已截止");
        }
        
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
               .eq(Registration::getActivityId, activityId)
               .eq(Registration::getStatus, "REGISTERED");
        if (registrationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("您已报名该活动");
        }
        
        String stockKey = RedisKeyConstant.getActivityStockKey(activityId);
        Long stock = redisTemplate.opsForValue().decrement(stockKey);
        
        if (stock == null || stock < 0) {
            redisTemplate.opsForValue().increment(stockKey);
            throw new BusinessException("活动名额已满");
        }
        
        Registration registration = new Registration();
        registration.setUserId(userId);
        registration.setActivityId(activityId);
        registration.setRegistrationTime(LocalDateTime.now());
        registration.setCheckInStatus(0);
        registration.setHoursConfirmed(0);
        registration.setStatus("REGISTERED");
        
        registrationMapper.insert(registration);
        activityMapper.incrementParticipants(activityId);
    }
    
    public List<RegistrationVO> getUserRegistrations(Long userId) {
        return registrationMapper.selectUserRegistrations(userId);
    }

    /**
     * 管理员查看报名记录；activityId 为空则返回全部（有效报名 status=REGISTERED）。
     */
    public List<RegistrationVO> listRegistrationsForAdmin(Long activityId) {
        if (activityId != null) {
            return registrationMapper.selectRegistrationsForAdminByActivityId(activityId);
        }
        return registrationMapper.selectAllRegistrationsForAdmin();
    }

    /**
     * 时长核销：已结束（end_time &lt; 当前时间）且未取消的活动，供管理员选择。
     */
    public List<ActivityVO> listEndedActivitiesForAdmin() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Activity> w = new LambdaQueryWrapper<>();
        w.lt(Activity::getEndTime, now)
                .ne(Activity::getStatus, "CANCELLED")
                .ne(Activity::getStatus, "COMPLETED")
                .orderByDesc(Activity::getEndTime);
        List<Activity> list = activityMapper.selectList(w);
        List<ActivityVO> result = new ArrayList<>(list.size());
        for (Activity a : list) {
            result.add(toActivityVO(a));
        }
        return result;
    }

    /**
     * 活动签到：仅「已开始且未结束」的活动（start_time &lt;= 当前时间 &lt;= end_time，边界按库内时间），且未取消。
     */
    public List<ActivityVO> listCheckInActivitiesForAdmin() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Activity> w = new LambdaQueryWrapper<>();
        w.le(Activity::getStartTime, now)
                .ge(Activity::getEndTime, now)
                .ne(Activity::getStatus, "CANCELLED")
                .orderByAsc(Activity::getStartTime);
        List<Activity> list = activityMapper.selectList(w);
        List<ActivityVO> result = new ArrayList<>(list.size());
        for (Activity a : list) {
            result.add(toActivityVO(a));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void checkInRegistration(Long registrationId) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException("报名记录不存在");
        }
        if (!"REGISTERED".equals(registration.getStatus())) {
            throw new BusinessException("报名记录无效");
        }
        if (registration.getCheckInStatus() != null && registration.getCheckInStatus() == 1) {
            throw new BusinessException("该志愿者已签到");
        }
        Activity activity = activityMapper.selectById(registration.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if ("CANCELLED".equals(activity.getStatus())) {
            throw new BusinessException("活动已取消，无法签到");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new BusinessException("活动尚未开始，无法签到");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException("活动已结束，无法签到");
        }
        registration.setCheckInStatus(1);
        registration.setCheckInTime(now);
        registrationMapper.updateById(registration);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void confirmHours(Long registrationId) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException("报名记录不存在");
        }
        
        if (registration.getHoursConfirmed() == 1) {
            throw new BusinessException("时长已核销");
        }
        if (registration.getCheckInStatus() == null || registration.getCheckInStatus() != 1) {
            throw new BusinessException("仅已签到人员可核销时长");
        }
        
        Activity activity = activityMapper.selectById(registration.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (activity.getEndTime() == null) {
            throw new BusinessException("活动信息异常");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(activity.getEndTime())) {
            throw new BusinessException("活动尚未结束，无法核销时长");
        }
        if ("CANCELLED".equals(activity.getStatus())) {
            throw new BusinessException("已取消的活动无法核销");
        }
        
        registration.setHoursConfirmed(1);
        registration.setConfirmTime(LocalDateTime.now());
        registrationMapper.updateById(registration);
        
        userServiceClient.updateVolunteerHours(registration.getUserId(), activity.getVolunteerHours());
    }

    /**
     * 管理员删除活动：先删报名记录（外键依赖），再删活动，并清理 Redis 库存键。
     * 注意：已核销产生的用户累计时长不会回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        LambdaQueryWrapper<Registration> regWrapper = new LambdaQueryWrapper<>();
        regWrapper.eq(Registration::getActivityId, activityId);
        registrationMapper.delete(regWrapper);
        activityMapper.deleteById(activityId);
        redisTemplate.delete(RedisKeyConstant.getActivityStockKey(activityId));
        minioStorageService.deleteObjectQuietly(activity.getImageKey());
    }

    private int countRegisteredForActivity(Long activityId) {
        LambdaQueryWrapper<Registration> w = new LambdaQueryWrapper<>();
        w.eq(Registration::getActivityId, activityId).eq(Registration::getStatus, "REGISTERED");
        return Math.toIntExact(registrationMapper.selectCount(w));
    }

    /**
     * 以 vol_registration（REGISTERED）为权威，修正 current_participants，并重算 Redis 剩余名额（max - 实际报名数）。
     */
    private void reconcileActivityRegistrationStats(Activity activity, int actualRegistered) {
        if (activity.getCurrentParticipants() != actualRegistered) {
            LambdaUpdateWrapper<Activity> uw = new LambdaUpdateWrapper<>();
            uw.eq(Activity::getId, activity.getId()).set(Activity::getCurrentParticipants, actualRegistered);
            activityMapper.update(null, uw);
        }
        activity.setCurrentParticipants(actualRegistered);
        int remaining = Math.max(0, activity.getMaxParticipants() - actualRegistered);
        redisTemplate.opsForValue().set(
                RedisKeyConstant.getActivityStockKey(activity.getId()),
                String.valueOf(remaining),
                7,
                TimeUnit.DAYS
        );
    }

    private void syncParticipantCountsWithRegistrations(List<Activity> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream().map(Activity::getId).toList();
        List<ActivityRegisteredCount> rows = registrationMapper.countRegisteredGroupByActivityId(ids);
        Map<Long, Integer> countMap = new HashMap<>();
        for (ActivityRegisteredCount row : rows) {
            countMap.put(row.getActivityId(), row.getCnt().intValue());
        }
        for (Activity activity : records) {
            int actual = countMap.getOrDefault(activity.getId(), 0);
            reconcileActivityRegistrationStats(activity, actual);
        }
    }

    private void applyActivityListOrdering(QueryWrapper<Activity> queryWrapper, String status) {
        if (StringUtils.hasText(status)) {
            queryWrapper.orderByAsc("registration_deadline", "start_time");
            return;
        }

        String priorityOrder = """
                CASE
                    WHEN status NOT IN ('COMPLETED', 'CANCELLED')
                         AND registration_start_time <= NOW()
                         AND registration_deadline >= NOW() THEN 0
                    WHEN status NOT IN ('COMPLETED', 'CANCELLED')
                         AND registration_start_time > NOW() THEN 1
                    WHEN status NOT IN ('COMPLETED', 'CANCELLED') THEN 2
                    ELSE 3
                END
                """;

        queryWrapper.orderByAsc(priorityOrder, "registration_deadline", "start_time");
    }

    private ActivityVO toActivityVO(Activity activity) {
        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(activity, vo);
        int current = activity.getCurrentParticipants() != null ? activity.getCurrentParticipants() : 0;
        int max = activity.getMaxParticipants() != null ? activity.getMaxParticipants() : 0;
        vo.setAvailableSlots(Math.max(0, max - current));
        vo.setImageUrl(minioStorageService.buildActivityImageUrl(activity.getImageKey()));
        return vo;
    }
}
