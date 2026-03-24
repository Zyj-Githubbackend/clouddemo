package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    
    public ActivityService(ActivityMapper activityMapper, 
                          RegistrationMapper registrationMapper,
                          StringRedisTemplate redisTemplate,
                          UserServiceClient userServiceClient) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.redisTemplate = redisTemplate;
        this.userServiceClient = userServiceClient;
    }
    
    public IPage<ActivityVO> listActivities(Integer page, Integer size, String status, String category,
                                            String recruitmentPhase, Long userId) {
        Page<Activity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        
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
        
        wrapper.orderByDesc(Activity::getCreateTime);
        IPage<Activity> activityPage = activityMapper.selectPage(pageParam, wrapper);
        syncParticipantCountsWithRegistrations(activityPage.getRecords());

        return activityPage.convert(activity -> {
            ActivityVO vo = new ActivityVO();
            BeanUtils.copyProperties(activity, vo);
            vo.setAvailableSlots(activity.getMaxParticipants() - activity.getCurrentParticipants());
            
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

        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(activity, vo);
        vo.setAvailableSlots(activity.getMaxParticipants() - activity.getCurrentParticipants());
        
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
        if (request.getRegistrationStartTime() == null || request.getRegistrationDeadline() == null) {
            throw new BusinessException("请填写招募开始时间与截止时间");
        }
        if (!request.getRegistrationStartTime().isBefore(request.getRegistrationDeadline())) {
            throw new BusinessException("志愿招募开始时间须早于招募截止时间");
        }
        if (request.getRegistrationDeadline().isAfter(request.getStartTime())) {
            throw new BusinessException("报名截止时间不能晚于活动开始时间");
        }
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
    
    @Transactional(rollbackFor = Exception.class)
    public void confirmHours(Long registrationId) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException("报名记录不存在");
        }
        
        if (registration.getHoursConfirmed() == 1) {
            throw new BusinessException("时长已核销");
        }
        
        Activity activity = activityMapper.selectById(registration.getActivityId());
        if (activity == null) {
            throw new BusinessException("活动不存在");
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
}
