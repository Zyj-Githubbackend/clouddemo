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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

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
                default -> {
                    // ignore unknown values
                }
            }
        }

        applyActivityListOrdering(queryWrapper, status);

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
            throw new BusinessException("Activity not found");
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
        redisTemplate.opsForValue().set(stockKey, String.valueOf(activity.getMaxParticipants()), 7, TimeUnit.DAYS);
        log.info("created activity activityId={} creatorId={} maxParticipants={}",
                activity.getId(), creatorId, activity.getMaxParticipants());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateActivity(Long activityId, ActivityCreateRequest request) {
        Activity existing = activityMapper.selectById(activityId);
        if (existing == null) {
            throw new BusinessException("Activity not found");
        }
        if (isTerminalOrCancelled(existing.getStatus())) {
            throw new BusinessException("Completed or cancelled activities cannot be edited");
        }
        ActivityScheduleValidator.validate(request);
        if (request.getMaxParticipants() < existing.getCurrentParticipants()) {
            throw new BusinessException("Max participants cannot be less than current participants");
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
        log.info("updated activity activityId={} creatorId={}", activityId, creatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelActivity(Long activityId) {
        Activity existing = activityMapper.selectById(activityId);
        if (existing == null) {
            throw new BusinessException("Activity not found");
        }
        if ("COMPLETED".equals(existing.getStatus())) {
            throw new BusinessException("Completed activities cannot be cancelled");
        }
        if ("CANCELLED".equals(existing.getStatus())) {
            throw new BusinessException("Activity has already been cancelled");
        }

        LambdaQueryWrapper<Registration> regWrapper = new LambdaQueryWrapper<>();
        regWrapper.eq(Registration::getActivityId, activityId);
        int deletedRegistrations = registrationMapper.delete(regWrapper);
        existing.setStatus("CANCELLED");
        existing.setCurrentParticipants(0);
        activityMapper.updateById(existing);
        reconcileActivityRegistrationStats(existing, 0);
        log.info("cancelled activity activityId={} removedRegistrations={}", activityId, deletedRegistrations);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeActivity(Long activityId) {
        Activity existing = activityMapper.selectById(activityId);
        if (existing == null) {
            throw new BusinessException("Activity not found");
        }
        if ("COMPLETED".equals(existing.getStatus())) {
            throw new BusinessException("Activity has already been completed");
        }
        if ("CANCELLED".equals(existing.getStatus())) {
            throw new BusinessException("Cancelled activities cannot be completed");
        }

        existing.setStatus("COMPLETED");
        activityMapper.updateById(existing);
        int registeredCount = countRegisteredForActivity(activityId);
        reconcileActivityRegistrationStats(existing, registeredCount);
        log.info("completed activity activityId={} registeredCount={}", activityId, registeredCount);
    }

    private static boolean isTerminalOrCancelled(String status) {
        return "COMPLETED".equals(status) || "CANCELLED".equals(status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void registerActivity(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("Activity not found");
        }
        if (!"RECRUITING".equals(activity.getStatus())) {
            throw new BusinessException("Activity is not open for registration");
        }
        if (activity.getRegistrationStartTime() != null
                && LocalDateTime.now().isBefore(activity.getRegistrationStartTime())) {
            throw new BusinessException("Registration has not started yet");
        }
        if (LocalDateTime.now().isAfter(activity.getRegistrationDeadline())) {
            throw new BusinessException("Registration has ended");
        }

        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getStatus, "REGISTERED");
        if (registrationMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("You have already registered for this activity");
        }

        String stockKey = RedisKeyConstant.getActivityStockKey(activityId);
        Long stock = redisTemplate.opsForValue().decrement(stockKey);
        if (stock == null || stock < 0) {
            redisTemplate.opsForValue().increment(stockKey);
            throw new BusinessException("No slots available");
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
        log.info("registered user for activity activityId={} userId={} registrationId={} remainingSlots={}",
                activityId, userId, registration.getId(), stock);
    }

    public List<RegistrationVO> getUserRegistrations(Long userId) {
        return registrationMapper.selectUserRegistrations(userId);
    }

    public List<RegistrationVO> listRegistrationsForAdmin(Long activityId) {
        if (activityId != null) {
            return registrationMapper.selectRegistrationsForAdminByActivityId(activityId);
        }
        return registrationMapper.selectAllRegistrationsForAdmin();
    }

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
            throw new BusinessException("Registration record not found");
        }
        if (!"REGISTERED".equals(registration.getStatus())) {
            throw new BusinessException("Registration record is invalid");
        }
        if (registration.getCheckInStatus() != null && registration.getCheckInStatus() == 1) {
            throw new BusinessException("The volunteer has already checked in");
        }

        Activity activity = activityMapper.selectById(registration.getActivityId());
        if (activity == null) {
            throw new BusinessException("Activity not found");
        }
        if ("CANCELLED".equals(activity.getStatus())) {
            throw new BusinessException("Cancelled activities cannot be checked in");
        }

        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new BusinessException("Activity has not started yet");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException("Activity has already ended");
        }

        registration.setCheckInStatus(1);
        registration.setCheckInTime(now);
        registrationMapper.updateById(registration);
        log.info("checked in registration registrationId={} activityId={} userId={}",
                registrationId, registration.getActivityId(), registration.getUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmHours(Long registrationId) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException("Registration record not found");
        }
        if (registration.getHoursConfirmed() == 1) {
            throw new BusinessException("Volunteer hours have already been confirmed");
        }
        if (registration.getCheckInStatus() == null || registration.getCheckInStatus() != 1) {
            throw new BusinessException("Only checked-in volunteers can be confirmed");
        }

        Activity activity = activityMapper.selectById(registration.getActivityId());
        if (activity == null) {
            throw new BusinessException("Activity not found");
        }
        if (activity.getEndTime() == null) {
            throw new BusinessException("Activity data is invalid");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(activity.getEndTime())) {
            throw new BusinessException("Volunteer hours can only be confirmed after the activity ends");
        }
        if ("CANCELLED".equals(activity.getStatus())) {
            throw new BusinessException("Cancelled activities cannot be confirmed");
        }

        registration.setHoursConfirmed(1);
        registration.setConfirmTime(LocalDateTime.now());
        registrationMapper.updateById(registration);

        userServiceClient.updateVolunteerHours(registration.getUserId(), activity.getVolunteerHours());
        log.info("confirmed volunteer hours registrationId={} activityId={} userId={} hours={}",
                registrationId, registration.getActivityId(), registration.getUserId(), activity.getVolunteerHours());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("Activity not found");
        }

        LambdaQueryWrapper<Registration> regWrapper = new LambdaQueryWrapper<>();
        regWrapper.eq(Registration::getActivityId, activityId);
        int deletedRegistrations = registrationMapper.delete(regWrapper);
        activityMapper.deleteById(activityId);
        redisTemplate.delete(RedisKeyConstant.getActivityStockKey(activityId));
        minioStorageService.deleteObjectQuietly(activity.getImageKey());
        log.info("deleted activity activityId={} removedRegistrations={}", activityId, deletedRegistrations);
    }

    private int countRegisteredForActivity(Long activityId) {
        LambdaQueryWrapper<Registration> w = new LambdaQueryWrapper<>();
        w.eq(Registration::getActivityId, activityId).eq(Registration::getStatus, "REGISTERED");
        return Math.toIntExact(registrationMapper.selectCount(w));
    }

    private void reconcileActivityRegistrationStats(Activity activity, int actualRegistered) {
        if (activity.getCurrentParticipants() != actualRegistered) {
            LambdaUpdateWrapper<Activity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Activity::getId, activity.getId())
                    .set(Activity::getCurrentParticipants, actualRegistered);
            activityMapper.update(null, updateWrapper);
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
