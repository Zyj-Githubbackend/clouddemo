package org.example.service;

import org.example.common.exception.BusinessException;
import org.example.dto.ActivityCreateRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityScheduleValidatorTest {

    @Test
    void validateShouldPassForValidSchedule() {
        ActivityCreateRequest request = buildValidRequest();

        assertDoesNotThrow(() -> ActivityScheduleValidator.validate(request));
    }

    @Test
    void validateShouldRejectRegistrationDeadlineAfterActivityStart() {
        ActivityCreateRequest request = buildValidRequest();
        request.setRegistrationDeadline(request.getStartTime().plusHours(1));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> ActivityScheduleValidator.validate(request));

        assertEquals("报名截止时间不能晚于活动开始时间", exception.getMessage());
    }

    private ActivityCreateRequest buildValidRequest() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        ActivityCreateRequest request = new ActivityCreateRequest();
        request.setTitle("校园清洁");
        request.setLocation("图书馆");
        request.setCategory("校园服务");
        request.setMaxParticipants(20);
        request.setVolunteerHours(BigDecimal.valueOf(2));
        request.setRegistrationStartTime(now.plusDays(1));
        request.setRegistrationDeadline(now.plusDays(2));
        request.setStartTime(now.plusDays(3));
        request.setEndTime(now.plusDays(3).plusHours(2));
        return request;
    }
}
