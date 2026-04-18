package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.client.UserServiceClient;
import org.example.common.constant.RedisKeyConstant;
import org.example.common.exception.BusinessException;
import org.example.dto.ActivityCreateRequest;
import org.example.dto.ActivityRegisteredCount;
import org.example.dto.UserSummary;
import org.example.entity.Activity;
import org.example.entity.Registration;
import org.example.mapper.ActivityMapper;
import org.example.mapper.EventOutboxMapper;
import org.example.mapper.RegistrationMapper;
import org.example.messaging.IdempotencyHelper;
import org.example.messaging.MessagingConstants;
import org.example.messaging.outbox.EventOutbox;
import org.example.messaging.outbox.OutboxStatus;
import org.example.vo.ActivityVO;
import org.example.vo.RegistrationVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final EventOutboxMapper eventOutboxMapper;
    private final StringRedisTemplate redisTemplate;
    private final MinioStorageService minioStorageService;
    private final IdempotencyHelper idempotencyHelper;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper;

    public ActivityService(ActivityMapper activityMapper,
                           RegistrationMapper registrationMapper,
                           EventOutboxMapper eventOutboxMapper,
                           StringRedisTemplate redisTemplate,
                           MinioStorageService minioStorageService,
                           IdempotencyHelper idempotencyHelper,
                           UserServiceClient userServiceClient,
                           ObjectMapper objectMapper) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.eventOutboxMapper = eventOutboxMapper;
        this.redisTemplate = redisTemplate;
        this.minioStorageService = minioStorageService;
        this.idempotencyHelper = idempotencyHelper;
        this.userServiceClient = userServiceClient;
        this.objectMapper = objectMapper;
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
        activity.setImageKey(joinImageKeys(normalizeImageKeys(request)));
        activity.setCurrentParticipants(0);
        activity.setStatus("RECRUITING");

        activityMapper.insert(activity);
        eventOutboxMapper.insert(buildActivityUpsertedOutbox(activity, creatorId));

        String stockKey = RedisKeyConstant.getActivityStockKey(activity.getId());
        redisTemplate.opsForValue().set(stockKey, String.valueOf(activity.getMaxParticipants()), 7, TimeUnit.DAYS);
        log.info("created activity activityId={} creatorId={} maxParticipants={}",
                activity.getId(), creatorId, activity.getMaxParticipants());
    }

    private EventOutbox buildActivityUpsertedOutbox(Activity activity, Long creatorId) {
        EventOutbox outbox = new EventOutbox();
        outbox.setMessageId(idempotencyHelper.newMessageId());
        outbox.setEventType(MessagingConstants.ROUTING_ACTIVITY_UPSERTED);
        outbox.setAggregateType("activity");
        outbox.setAggregateId(String.valueOf(activity.getId()));
        outbox.setPayloadJson(buildActivityUpsertedPayload(activity, creatorId));
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        outbox.setCreatedAt(LocalDateTime.now());
        return outbox;
    }

    private EventOutbox buildActivityDeletedOutbox(Long activityId) {
        EventOutbox outbox = new EventOutbox();
        outbox.setMessageId(idempotencyHelper.newMessageId());
        outbox.setEventType(MessagingConstants.ROUTING_ACTIVITY_DELETED);
        outbox.setAggregateType("activity");
        outbox.setAggregateId(String.valueOf(activityId));
        outbox.setPayloadJson(buildActivityDeletedPayload(activityId));
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        outbox.setCreatedAt(LocalDateTime.now());
        return outbox;
    }

    private String buildActivityUpsertedPayload(Activity activity, Long creatorId) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("activityId", activity.getId());
        payload.put("title", activity.getTitle());
        payload.put("location", activity.getLocation());
        payload.put("status", activity.getStatus());
        payload.put("category", activity.getCategory());
        payload.put("creatorId", creatorId);
        payload.put("startTime", activity.getStartTime() == null ? null : activity.getStartTime().toString());
        payload.put("endTime", activity.getEndTime() == null ? null : activity.getEndTime().toString());
        payload.put("maxParticipants", activity.getMaxParticipants());
        payload.put("stackId", System.getenv().getOrDefault("STACK_ID", "single"));
        return writePayload(payload, "activity.upserted");
    }

    private String buildActivityDeletedPayload(Long activityId) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("activityId", activityId);
        payload.put("stackId", System.getenv().getOrDefault("STACK_ID", "single"));
        return writePayload(payload, "activity.deleted");
    }

    private String writePayload(Map<String, Object> payload, String eventName) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize " + eventName + " payload", ex);
        }
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
        List<String> oldImageKeys = splitImageKeys(existing.getImageKey());
        BeanUtils.copyProperties(request, existing);
        existing.setId(activityId);
        existing.setCreatorId(creatorId);
        existing.setImageKey(joinImageKeys(normalizeImageKeys(request)));
        activityMapper.updateById(existing);
        reconcileActivityRegistrationStats(existing, countRegisteredForActivity(activityId));
        Set<String> currentImageKeys = new LinkedHashSet<>(splitImageKeys(existing.getImageKey()));
        for (String oldImageKey : oldImageKeys) {
            if (!currentImageKeys.contains(oldImageKey)) {
                minioStorageService.deleteObjectQuietly(oldImageKey);
            }
        }
        eventOutboxMapper.insert(buildActivityUpsertedOutbox(existing, creatorId));
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
        eventOutboxMapper.insert(buildActivityUpsertedOutbox(existing, existing.getCreatorId()));
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
        eventOutboxMapper.insert(buildActivityUpsertedOutbox(existing, existing.getCreatorId()));
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

        Registration existingRegistration = findExistingRegistration(activityId, userId);
        if (existingRegistration != null && "REGISTERED".equals(existingRegistration.getStatus())) {
            throw new BusinessException("You have already registered for this activity");
        }

        String stockKey = RedisKeyConstant.getActivityStockKey(activityId);
        Long stock = redisTemplate.opsForValue().decrement(stockKey);
        if (stock == null || stock < 0) {
            redisTemplate.opsForValue().increment(stockKey);
            throw new BusinessException("No slots available");
        }

        if (existingRegistration != null) {
            reactivateRegistration(existingRegistration);
            activityMapper.incrementParticipants(activityId);
            log.info("reactivated registration for activity activityId={} userId={} registrationId={} remainingSlots={}",
                    activityId, userId, existingRegistration.getId(), stock);
            return;
        }

        Registration registration = new Registration();
        registration.setUserId(userId);
        registration.setActivityId(activityId);
        registration.setRegistrationTime(LocalDateTime.now());
        registration.setCheckInStatus(0);
        registration.setHoursConfirmed(0);
        registration.setStatus("REGISTERED");

        try {
            registrationMapper.insert(registration);
        } catch (DataIntegrityViolationException ex) {
            redisTemplate.opsForValue().increment(stockKey);
            log.warn("duplicate registration blocked by database constraint activityId={} userId={}", activityId, userId, ex);
            throw new BusinessException("You have already registered for this activity");
        }
        activityMapper.incrementParticipants(activityId);
        log.info("registered user for activity activityId={} userId={} registrationId={} remainingSlots={}",
                activityId, userId, registration.getId(), stock);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelMyRegistration(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("Activity not found");
        }
        if ("CANCELLED".equals(activity.getStatus())) {
            throw new BusinessException("Activity has already been cancelled");
        }
        if ("COMPLETED".equals(activity.getStatus())) {
            throw new BusinessException("Completed activities cannot be cancelled by users");
        }
        if (activity.getStartTime() != null && !LocalDateTime.now().isBefore(activity.getStartTime())) {
            throw new BusinessException("Registrations cannot be cancelled after the activity starts");
        }

        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getStatus, "REGISTERED")
                .orderByDesc(Registration::getCreateTime)
                .last("LIMIT 1");
        Registration registration = registrationMapper.selectOne(wrapper);
        if (registration == null) {
            throw new BusinessException("No active registration found for this activity");
        }
        if (registration.getCheckInStatus() != null && registration.getCheckInStatus() == 1) {
            throw new BusinessException("Checked-in registrations cannot be cancelled");
        }
        if (registration.getHoursConfirmed() != null && registration.getHoursConfirmed() == 1) {
            throw new BusinessException("Confirmed registrations cannot be cancelled");
        }

        registration.setStatus("CANCELLED");
        registrationMapper.updateById(registration);
        activityMapper.decrementParticipants(activityId);
        redisTemplate.opsForValue().increment(RedisKeyConstant.getActivityStockKey(activityId));
        log.info("cancelled registration activityId={} userId={} registrationId={}",
                activityId, userId, registration.getId());
    }

    public List<RegistrationVO> getUserRegistrations(Long userId) {
        return registrationMapper.selectUserRegistrations(userId);
    }

    public byte[] exportConfirmedUserRegistrations(Long userId) {
        List<RegistrationVO> confirmedRegistrations = registrationMapper.selectConfirmedRegistrationsByUserId(userId);
        BigDecimal totalConfirmedHours = confirmedRegistrations.stream()
                .map(RegistrationVO::getVolunteerHours)
                .filter(hours -> hours != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("已核销志愿记录");
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);

            createCell(sheet.createRow(0), 0, "我的志愿足迹已核销记录", titleStyle);
            createCell(sheet.createRow(1), 0, "导出时间");
            createCell(sheet.getRow(1), 1, formatDateTime(LocalDateTime.now()));
            createCell(sheet.createRow(2), 0, "已核销活动数");
            createCell(sheet.getRow(2), 1, String.valueOf(confirmedRegistrations.size()));
            createCell(sheet.createRow(3), 0, "已核销志愿时长（小时）");
            createCell(sheet.getRow(3), 1, totalConfirmedHours.stripTrailingZeros().toPlainString());

            Row headerRow = sheet.createRow(5);
            String[] headers = {"活动名称", "活动地点", "活动开始时间", "报名时间", "核销时间", "核销时长（小时）"};
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 6;
            for (RegistrationVO registration : confirmedRegistrations) {
                Row row = sheet.createRow(rowIndex++);
                createCell(row, 0, registration.getActivityTitle());
                createCell(row, 1, registration.getLocation());
                createCell(row, 2, formatDateTime(registration.getStartTime()));
                createCell(row, 3, formatDateTime(registration.getRegistrationTime()));
                createCell(row, 4, formatDateTime(registration.getConfirmTime()));
                createCell(row, 5, registration.getVolunteerHours() == null
                        ? "0"
                        : registration.getVolunteerHours().stripTrailingZeros().toPlainString());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1024, 40 * 256));
            }

            workbook.write(outputStream);
            log.info("exported confirmed volunteer registrations userId={} count={} totalHours={}",
                    userId, confirmedRegistrations.size(), totalConfirmedHours);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            log.error("failed to export confirmed volunteer registrations userId={}", userId, ex);
            throw new BusinessException("Failed to export confirmed volunteer records");
        }
    }

    public List<RegistrationVO> listRegistrationsForAdmin(Long activityId) {
        List<RegistrationVO> registrations;
        if (activityId != null) {
            registrations = registrationMapper.selectRegistrationsForAdminByActivityId(activityId);
        } else {
            registrations = registrationMapper.selectAllRegistrationsForAdmin();
        }
        enrichRegistrationUsers(registrations);
        return registrations;
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

        eventOutboxMapper.insert(buildUserUpdatedOutbox(registration, activity));
        log.info("confirmed volunteer hours registrationId={} activityId={} userId={} hours={} eventType={}",
                registrationId,
                registration.getActivityId(),
                registration.getUserId(),
                activity.getVolunteerHours(),
                MessagingConstants.ROUTING_USER_UPDATED);
    }

    private EventOutbox buildUserUpdatedOutbox(Registration registration, Activity activity) {
        EventOutbox outbox = new EventOutbox();
        outbox.setMessageId(idempotencyHelper.newMessageId());
        outbox.setEventType(MessagingConstants.ROUTING_USER_UPDATED);
        outbox.setAggregateType("user");
        outbox.setAggregateId(String.valueOf(registration.getUserId()));
        outbox.setPayloadJson(buildUserUpdatedPayload(registration, activity));
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        outbox.setCreatedAt(LocalDateTime.now());
        return outbox;
    }

    private String buildUserUpdatedPayload(Registration registration, Activity activity) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", registration.getUserId());
        payload.put("registrationId", registration.getId());
        payload.put("activityId", registration.getActivityId());
        payload.put("hours", activity.getVolunteerHours());
        payload.put("confirmedAt", registration.getConfirmTime() == null ? null : registration.getConfirmTime().toString());
        payload.put("stackId", System.getenv().getOrDefault("STACK_ID", "single"));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize user.updated payload", ex);
        }
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
        eventOutboxMapper.insert(buildActivityDeletedOutbox(activityId));
        for (String imageKey : splitImageKeys(activity.getImageKey())) {
            minioStorageService.deleteObjectQuietly(imageKey);
        }
        log.info("deleted activity activityId={} removedRegistrations={}", activityId, deletedRegistrations);
    }

    private void enrichRegistrationUsers(List<RegistrationVO> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return;
        }

        List<Long> userIds = registrations.stream()
                .map(RegistrationVO::getUserId)
                .filter(userId -> userId != null && userId > 0)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, UserSummary> userSummaryMap = userServiceClient.listUserSummariesByIds(userIds);
        for (RegistrationVO registration : registrations) {
            UserSummary userSummary = userSummaryMap.get(registration.getUserId());
            if (userSummary == null) {
                continue;
            }
            registration.setUsername(userSummary.getUsername());
            registration.setRealName(userSummary.getRealName());
            registration.setStudentNo(userSummary.getStudentNo());
            registration.setPhone(userSummary.getPhone());
        }
    }

    private int countRegisteredForActivity(Long activityId) {
        LambdaQueryWrapper<Registration> w = new LambdaQueryWrapper<>();
        w.eq(Registration::getActivityId, activityId).eq(Registration::getStatus, "REGISTERED");
        return Math.toIntExact(registrationMapper.selectCount(w));
    }

    private Registration findExistingRegistration(Long activityId, Long userId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
                .eq(Registration::getActivityId, activityId)
                .last("LIMIT 1");
        return registrationMapper.selectOne(wrapper);
    }

    private void reactivateRegistration(Registration registration) {
        registration.setRegistrationTime(LocalDateTime.now());
        registration.setCheckInStatus(0);
        registration.setCheckInTime(null);
        registration.setHoursConfirmed(0);
        registration.setConfirmTime(null);
        registration.setStatus("REGISTERED");
        registrationMapper.updateById(registration);
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
        List<String> imageKeys = splitImageKeys(activity.getImageKey());
        List<String> imageUrls = buildImageUrls(imageKeys);
        int current = activity.getCurrentParticipants() != null ? activity.getCurrentParticipants() : 0;
        int max = activity.getMaxParticipants() != null ? activity.getMaxParticipants() : 0;
        vo.setAvailableSlots(Math.max(0, max - current));
        vo.setImageKeys(imageKeys);
        vo.setImageUrls(imageUrls);
        vo.setImageKey(imageKeys.isEmpty() ? null : imageKeys.get(0));
        vo.setImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
        return vo;
    }

    private List<String> normalizeImageKeys(ActivityCreateRequest request) {
        if (request == null) {
            return Collections.emptyList();
        }

        List<String> rawKeys = request.getImageKeys();
        if (rawKeys == null || rawKeys.isEmpty()) {
            rawKeys = StringUtils.hasText(request.getImageKey())
                    ? List.of(request.getImageKey())
                    : Collections.emptyList();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String key : rawKeys) {
            if (StringUtils.hasText(key)) {
                normalized.add(key.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    private List<String> splitImageKeys(String rawImageKeys) {
        if (!StringUtils.hasText(rawImageKeys)) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String part : rawImageKeys.split(",")) {
            if (StringUtils.hasText(part)) {
                keys.add(part.trim());
            }
        }
        return new ArrayList<>(keys);
    }

    private String joinImageKeys(List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return null;
        }
        return String.join(",", imageKeys);
    }

    private List<String> buildImageUrls(List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> imageUrls = new ArrayList<>(imageKeys.size());
        for (String imageKey : imageKeys) {
            String imageUrl = minioStorageService.buildActivityImageUrl(imageKey);
            if (StringUtils.hasText(imageUrl)) {
                imageUrls.add(imageUrl);
            }
        }
        return imageUrls;
    }

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void createCell(Row row, int cellIndex, String value) {
        createCell(row, cellIndex, value, null);
    }

    private void createCell(Row row, int cellIndex, String value, CellStyle style) {
        Cell cell = row.createCell(cellIndex);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(EXPORT_TIME_FORMATTER);
    }
}
