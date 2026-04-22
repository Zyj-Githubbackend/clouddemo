package org.example.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.mapper.MqConsumeRecordMapper;
import org.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUpdatedConsumerTest {

    @Mock
    private MqConsumeRecordMapper mqConsumeRecordMapper;

    @Mock
    private UserService userService;

    @Test
    void onUserUpdatedShouldApplyVolunteerHoursAndRecordConsumption() {
        UserUpdatedConsumer consumer = new UserUpdatedConsumer(mqConsumeRecordMapper, userService, new ObjectMapper());
        DomainEvent event = new DomainEvent();
        event.setMessageId("msg-1");
        event.setEventType(MessagingConstants.ROUTING_USER_UPDATED);
        event.setAggregateId("5");
        event.setPayload("{\"userId\":5,\"hours\":2.5}");

        when(mqConsumeRecordMapper.selectCount(any())).thenReturn(0L);

        consumer.onUserUpdated(event);

        verify(userService).updateVolunteerHours(5L, new BigDecimal("2.5"));
        ArgumentCaptor<MqConsumeRecord> recordCaptor = ArgumentCaptor.forClass(MqConsumeRecord.class);
        verify(mqConsumeRecordMapper).insert(recordCaptor.capture());
        assertEquals("msg-1", recordCaptor.getValue().getMessageId());
        assertEquals("user-service.user-updated-consumer", recordCaptor.getValue().getConsumerName());
        assertEquals("CONSUMED", recordCaptor.getValue().getStatus());
    }

    @Test
    void onUserUpdatedShouldSkipDuplicatedMessage() {
        UserUpdatedConsumer consumer = new UserUpdatedConsumer(mqConsumeRecordMapper, userService, new ObjectMapper());
        DomainEvent event = new DomainEvent();
        event.setMessageId("dup-1");
        event.setEventType(MessagingConstants.ROUTING_USER_UPDATED);
        event.setPayload("{\"userId\":5,\"hours\":2.5}");

        when(mqConsumeRecordMapper.selectCount(any())).thenReturn(1L);

        consumer.onUserUpdated(event);

        verify(userService, never()).updateVolunteerHours(any(), any());
        verify(mqConsumeRecordMapper, never()).insert(any(MqConsumeRecord.class));
    }
}
