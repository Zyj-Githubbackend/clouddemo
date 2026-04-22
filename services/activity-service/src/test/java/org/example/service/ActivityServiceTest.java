package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.client.UserServiceClient;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dto.ActivityCreateRequest;
import org.example.dto.UserSummary;
import org.example.entity.Activity;
import org.example.entity.Registration;
import org.example.mapper.ActivityMapper;
import org.example.mapper.EventOutboxMapper;
import org.example.mapper.RegistrationMapper;
import org.example.messaging.IdempotencyHelper;
import org.example.messaging.MessagingConstants;
import org.example.messaging.outbox.EventOutbox;
import org.example.vo.RegistrationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private RegistrationMapper registrationMapper;

    @Mock
    private EventOutboxMapper eventOutboxMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MinioStorageService minioStorageService;

    @Mock
    private IdempotencyHelper idempotencyHelper;

    @Mock
    private UserServiceClient userServiceClient;

    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        activityService = new ActivityService(
                activityMapper,
                registrationMapper,
                eventOutboxMapper,
                redisTemplate,
                minioStorageService,
                idempotencyHelper,
                userServiceClient,
                new ObjectMapper()
        );

    }

    @Test
    void createActivityShouldNormalizeAndStoreImageKeys() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(idempotencyHelper.newMessageId()).thenReturn("test-message-id");

        ActivityCreateRequest request = new ActivityCreateRequest();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        request.setTitle("环保活动");
        request.setLocation("操场");
        request.setCategory("校园服务");
        request.setMaxParticipants(30);
        request.setVolunteerHours(BigDecimal.valueOf(3));
        request.setRegistrationStartTime(now.plusDays(1));
        request.setRegistrationDeadline(now.plusDays(2));
        request.setStartTime(now.plusDays(3));
        request.setEndTime(now.plusDays(3).plusHours(2));
        request.setImageKeys(List.of(" first.png ", "second.png", "first.png", "  "));

        doAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            activity.setId(88L);
            return 1;
        }).when(activityMapper).insert(any(Activity.class));

        activityService.createActivity(request, 7L);

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(activityCaptor.capture());
        Activity savedActivity = activityCaptor.getValue();

        assertEquals("first.png,second.png", savedActivity.getImageKey());
        assertEquals("RECRUITING", savedActivity.getStatus());
        assertEquals(0, savedActivity.getCurrentParticipants());
        assertEquals(7L, savedActivity.getCreatorId());
        verify(valueOperations).set("activity:stock:88", "30", 7, TimeUnit.DAYS);
        ArgumentCaptor<EventOutbox> outboxCaptor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(eventOutboxMapper).insert(outboxCaptor.capture());
        assertEquals(MessagingConstants.ROUTING_ACTIVITY_UPSERTED, outboxCaptor.getValue().getEventType());
    }

    @Test
    void exportConfirmedUserRegistrationsShouldGenerateWorkbook() throws Exception {
        RegistrationVO first = new RegistrationVO();
        first.setActivityTitle("图书整理");
        first.setLocation("图书馆");
        first.setVolunteerHours(new BigDecimal("1.5"));
        first.setStartTime(LocalDateTime.of(2026, 4, 1, 9, 0));
        first.setRegistrationTime(LocalDateTime.of(2026, 3, 28, 10, 0));
        first.setConfirmTime(LocalDateTime.of(2026, 4, 1, 12, 0));

        RegistrationVO second = new RegistrationVO();
        second.setActivityTitle("校园引导");
        second.setLocation("南门");
        second.setVolunteerHours(new BigDecimal("2.0"));
        second.setStartTime(LocalDateTime.of(2026, 4, 2, 14, 0));
        second.setRegistrationTime(LocalDateTime.of(2026, 3, 29, 16, 0));
        second.setConfirmTime(LocalDateTime.of(2026, 4, 2, 18, 0));

        when(registrationMapper.selectConfirmedRegistrationsByUserId(5L)).thenReturn(List.of(first, second));

        byte[] content = activityService.exportConfirmedUserRegistrations(5L);

        assertNotNull(content);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("2", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("3.5", sheet.getRow(3).getCell(1).getStringCellValue());
            assertEquals("图书整理", sheet.getRow(6).getCell(0).getStringCellValue());
            assertEquals("校园引导", sheet.getRow(7).getCell(0).getStringCellValue());
        }
    }

    @Test
    void cancelMyRegistrationShouldMarkRegistrationCancelledAndRestoreStock() {
        Activity activity = new Activity();
        activity.setId(9L);
        activity.setStatus("RECRUITING");
        activity.setStartTime(LocalDateTime.now().plusDays(1));

        Registration registration = new Registration();
        registration.setId(12L);
        registration.setActivityId(9L);
        registration.setUserId(5L);
        registration.setStatus("REGISTERED");
        registration.setCheckInStatus(0);
        registration.setHoursConfirmed(0);

        when(activityMapper.selectById(9L)).thenReturn(activity);
        when(registrationMapper.selectOne(any())).thenReturn(registration);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        activityService.cancelMyRegistration(9L, 5L);

        assertEquals("CANCELLED", registration.getStatus());
        verify(registrationMapper).updateById(registration);
        verify(activityMapper).decrementParticipants(9L);
        verify(valueOperations).increment("activity:stock:9");
    }

    @Test
    void cancelActivityShouldRejectAfterActivityStarts() {
        Activity activity = new Activity();
        activity.setId(9L);
        activity.setStatus("RECRUITING");
        activity.setStartTime(LocalDateTime.now().minusMinutes(1));

        when(activityMapper.selectById(9L)).thenReturn(activity);

        org.example.common.exception.BusinessException ex = assertThrows(
                org.example.common.exception.BusinessException.class,
                () -> activityService.cancelActivity(9L));

        assertEquals("Activities can only be cancelled before they start", ex.getMessage());
        verify(registrationMapper, never()).delete(any());
        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void completeActivityShouldRejectBeforeActivityEnds() {
        Activity activity = new Activity();
        activity.setId(9L);
        activity.setStatus("RECRUITING");
        activity.setEndTime(LocalDateTime.now().plusMinutes(1));

        when(activityMapper.selectById(9L)).thenReturn(activity);

        org.example.common.exception.BusinessException ex = assertThrows(
                org.example.common.exception.BusinessException.class,
                () -> activityService.completeActivity(9L));

        assertEquals("Activities can only be completed after they end", ex.getMessage());
        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void registerActivityShouldReactivateCancelledRegistrationInsteadOfInsert() {
        Activity activity = new Activity();
        activity.setId(19L);
        activity.setStatus("RECRUITING");
        activity.setRegistrationStartTime(LocalDateTime.now().minusHours(1));
        activity.setRegistrationDeadline(LocalDateTime.now().plusHours(2));

        Registration registration = new Registration();
        registration.setId(31L);
        registration.setActivityId(19L);
        registration.setUserId(1L);
        registration.setStatus("CANCELLED");
        registration.setCheckInStatus(1);
        registration.setCheckInTime(LocalDateTime.now().minusHours(3));
        registration.setHoursConfirmed(1);
        registration.setConfirmTime(LocalDateTime.now().minusHours(2));

        when(activityMapper.selectById(19L)).thenReturn(activity);
        when(registrationMapper.selectOne(any())).thenReturn(registration);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement("activity:stock:19")).thenReturn(4L);

        activityService.registerActivity(19L, 1L);

        assertEquals("REGISTERED", registration.getStatus());
        assertEquals(0, registration.getCheckInStatus());
        assertEquals(0, registration.getHoursConfirmed());
        assertNull(registration.getCheckInTime());
        assertNull(registration.getConfirmTime());
        assertNotNull(registration.getRegistrationTime());
        verify(registrationMapper).updateById(registration);
        verify(registrationMapper, never()).insert(any(Registration.class));
        verify(activityMapper).incrementParticipants(19L);
    }

    @Test
    void registerActivityShouldRebuildStockFromDatabaseWhenRedisKeyIsMissing() {
        Activity activity = new Activity();
        activity.setId(19L);
        activity.setStatus("RECRUITING");
        activity.setMaxParticipants(5);
        activity.setCurrentParticipants(2);
        activity.setRegistrationStartTime(LocalDateTime.now().minusHours(1));
        activity.setRegistrationDeadline(LocalDateTime.now().plusHours(2));

        when(activityMapper.selectById(19L)).thenReturn(activity);
        when(registrationMapper.selectOne(any())).thenReturn(null);
        when(registrationMapper.selectCount(any())).thenReturn(2L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement("activity:stock:19")).thenReturn(-1L, 2L);

        activityService.registerActivity(19L, 1L);

        verify(valueOperations).increment("activity:stock:19");
        verify(valueOperations).set("activity:stock:19", "3", 7, TimeUnit.DAYS);
        verify(registrationMapper).insert(any(Registration.class));
        verify(activityMapper).incrementParticipants(19L);
    }

    @Test
    void registerActivityShouldTranslateDuplicateKeyAndRestoreStock() {
        Activity activity = new Activity();
        activity.setId(19L);
        activity.setStatus("RECRUITING");
        activity.setRegistrationStartTime(LocalDateTime.now().minusHours(1));
        activity.setRegistrationDeadline(LocalDateTime.now().plusHours(2));

        when(activityMapper.selectById(19L)).thenReturn(activity);
        when(registrationMapper.selectOne(any())).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.decrement("activity:stock:19")).thenReturn(3L);
        doThrow(new DuplicateKeyException("duplicate"))
                .when(registrationMapper)
                .insert(any(Registration.class));

        org.example.common.exception.BusinessException ex = assertThrows(
                org.example.common.exception.BusinessException.class,
                () -> activityService.registerActivity(19L, 1L));

        assertEquals("You have already registered for this activity", ex.getMessage());
        verify(valueOperations).increment("activity:stock:19");
        verify(activityMapper, never()).incrementParticipants(19L);
        ArgumentCaptor<Registration> registrationCaptor = ArgumentCaptor.forClass(Registration.class);
        verify(registrationMapper).insert(registrationCaptor.capture());
        Registration insertedRegistration = registrationCaptor.getValue();
        assertEquals(1L, insertedRegistration.getUserId());
        assertEquals(19L, insertedRegistration.getActivityId());
        assertEquals("REGISTERED", insertedRegistration.getStatus());
    }

    @Test
    void cancelMyRegistrationShouldRejectCheckedInRegistration() {
        Activity activity = new Activity();
        activity.setId(9L);
        activity.setStatus("RECRUITING");
        activity.setStartTime(LocalDateTime.now().plusDays(1));

        Registration registration = new Registration();
        registration.setId(12L);
        registration.setActivityId(9L);
        registration.setUserId(5L);
        registration.setStatus("REGISTERED");
        registration.setCheckInStatus(1);
        registration.setHoursConfirmed(0);

        when(activityMapper.selectById(9L)).thenReturn(activity);
        when(registrationMapper.selectOne(any())).thenReturn(registration);

        assertThrows(org.example.common.exception.BusinessException.class,
                () -> activityService.cancelMyRegistration(9L, 5L));
        verify(registrationMapper, never()).updateById(any(Registration.class));
    }

    @Test
    void confirmHoursShouldWriteUserUpdatedOutboxInsteadOfCallingUserServiceSynchronously() {
        Registration registration = new Registration();
        registration.setId(73L);
        registration.setActivityId(9L);
        registration.setUserId(5L);
        registration.setCheckInStatus(1);
        registration.setHoursConfirmed(0);

        Activity activity = new Activity();
        activity.setId(9L);
        activity.setStatus("RECRUITING");
        activity.setVolunteerHours(new BigDecimal("2.5"));
        activity.setEndTime(LocalDateTime.now().minusHours(1));

        when(registrationMapper.selectById(73L)).thenReturn(registration);
        when(activityMapper.selectById(9L)).thenReturn(activity);
        when(idempotencyHelper.newMessageId()).thenReturn("msg-user-updated-1");

        activityService.confirmHours(73L);

        assertEquals(1, registration.getHoursConfirmed());
        assertNotNull(registration.getConfirmTime());
        verify(registrationMapper).updateById(registration);

        ArgumentCaptor<EventOutbox> outboxCaptor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(eventOutboxMapper).insert(outboxCaptor.capture());
        EventOutbox outbox = outboxCaptor.getValue();
        assertEquals(MessagingConstants.ROUTING_USER_UPDATED, outbox.getEventType());
        assertEquals("user", outbox.getAggregateType());
        assertEquals("5", outbox.getAggregateId());
    }

    @Test
    void confirmHoursShouldRejectCompletedActivity() {
        Registration registration = new Registration();
        registration.setId(73L);
        registration.setActivityId(9L);
        registration.setUserId(5L);
        registration.setCheckInStatus(1);
        registration.setHoursConfirmed(0);

        Activity activity = new Activity();
        activity.setId(9L);
        activity.setStatus("COMPLETED");
        activity.setEndTime(LocalDateTime.now().minusHours(1));

        when(registrationMapper.selectById(73L)).thenReturn(registration);
        when(activityMapper.selectById(9L)).thenReturn(activity);

        org.example.common.exception.BusinessException ex = assertThrows(
                org.example.common.exception.BusinessException.class,
                () -> activityService.confirmHours(73L));

        assertEquals("Completed activities cannot be confirmed", ex.getMessage());
        verify(registrationMapper, never()).updateById(any(Registration.class));
        verify(eventOutboxMapper, never()).insert(any(EventOutbox.class));
    }

    @Test
    void listRegistrationsForAdminShouldEnrichUserDataFromUserService() {
        RegistrationVO registration = new RegistrationVO();
        registration.setId(1L);
        registration.setUserId(5L);
        registration.setActivityId(9L);

        UserSummary userSummary = new UserSummary();
        userSummary.setId(5L);
        userSummary.setUsername("student05");
        userSummary.setRealName("Chen Qi");
        userSummary.setStudentNo("2021105");
        userSummary.setPhone("13800138005");

        when(registrationMapper.selectAllRegistrationsForAdmin()).thenReturn(List.of(registration));
        when(userServiceClient.listUserSummariesByIds(List.of(5L))).thenReturn(java.util.Map.of(5L, userSummary));

        List<RegistrationVO> result = activityService.listRegistrationsForAdmin(null);

        assertEquals(1, result.size());
        assertEquals("student05", result.get(0).getUsername());
        assertEquals("Chen Qi", result.get(0).getRealName());
        assertEquals("2021105", result.get(0).getStudentNo());
        assertEquals("13800138005", result.get(0).getPhone());
    }
}
