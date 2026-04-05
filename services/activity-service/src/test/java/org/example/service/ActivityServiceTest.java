package org.example.service;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dto.ActivityCreateRequest;
import org.example.entity.Activity;
import org.example.feign.UserServiceClient;
import org.example.mapper.ActivityMapper;
import org.example.mapper.RegistrationMapper;
import org.example.vo.RegistrationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private RegistrationMapper registrationMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private MinioStorageService minioStorageService;

    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        activityService = new ActivityService(
                activityMapper,
                registrationMapper,
                redisTemplate,
                userServiceClient,
                minioStorageService
        );
    }

    @Test
    void createActivityShouldNormalizeAndStoreImageKeys() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

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
}
