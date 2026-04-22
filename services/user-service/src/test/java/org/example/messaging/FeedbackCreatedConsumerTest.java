package org.example.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.UserFeedbackProjection;
import org.example.mapper.MqConsumeRecordMapper;
import org.example.mapper.UserFeedbackProjectionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackCreatedConsumerTest {

    @Mock
    private MqConsumeRecordMapper mqConsumeRecordMapper;

    @Mock
    private UserFeedbackProjectionMapper userFeedbackProjectionMapper;

    @Test
    void onFeedbackCreatedShouldUpsertProjectionAndRecordConsumption() {
        FeedbackCreatedConsumer consumer = new FeedbackCreatedConsumer(
                mqConsumeRecordMapper,
                userFeedbackProjectionMapper,
                new ObjectMapper().findAndRegisterModules()
        );

        DomainEvent event = new DomainEvent();
        event.setMessageId("msg-feedback-1");
        event.setEventType(MessagingConstants.ROUTING_FEEDBACK_CREATED);
        event.setAggregateId("101");
        event.setPayload("""
                {"feedbackId":101,"userId":5,"title":"Login issue","category":"BUG","status":"OPEN","createdAt":"2026-04-16T10:00:00"}
                """);

        when(mqConsumeRecordMapper.selectCount(any())).thenReturn(0L);
        when(userFeedbackProjectionMapper.selectById(101L)).thenReturn(null);

        consumer.onFeedbackCreated(event);

        ArgumentCaptor<UserFeedbackProjection> projectionCaptor = ArgumentCaptor.forClass(UserFeedbackProjection.class);
        verify(userFeedbackProjectionMapper).insert(projectionCaptor.capture());
        UserFeedbackProjection projection = projectionCaptor.getValue();
        assertEquals(101L, projection.getFeedbackId());
        assertEquals(5L, projection.getUserId());
        assertEquals("Login issue", projection.getTitle());
        assertEquals("BUG", projection.getCategory());
        assertEquals("OPEN", projection.getStatus());
        assertEquals("msg-feedback-1", projection.getSourceMessageId());
        verify(mqConsumeRecordMapper).insert(any(MqConsumeRecord.class));
    }

    @Test
    void onFeedbackCreatedShouldSkipDuplicatedMessage() {
        FeedbackCreatedConsumer consumer = new FeedbackCreatedConsumer(
                mqConsumeRecordMapper,
                userFeedbackProjectionMapper,
                new ObjectMapper().findAndRegisterModules()
        );

        DomainEvent event = new DomainEvent();
        event.setMessageId("dup-feedback-1");
        event.setEventType(MessagingConstants.ROUTING_FEEDBACK_CREATED);
        event.setPayload("{\"feedbackId\":101,\"userId\":5}");

        when(mqConsumeRecordMapper.selectCount(any())).thenReturn(1L);

        consumer.onFeedbackCreated(event);

        verify(userFeedbackProjectionMapper, never()).selectById(any());
        verify(userFeedbackProjectionMapper, never()).insert(any(UserFeedbackProjection.class));
        verify(userFeedbackProjectionMapper, never()).updateById(any(UserFeedbackProjection.class));
        verify(mqConsumeRecordMapper, never()).insert(any(MqConsumeRecord.class));
    }

    @Test
    void onFeedbackCreatedShouldAcceptLegacyMapPayload() {
        FeedbackCreatedConsumer consumer = new FeedbackCreatedConsumer(
                mqConsumeRecordMapper,
                userFeedbackProjectionMapper,
                new ObjectMapper().findAndRegisterModules()
        );

        DomainEvent event = new DomainEvent();
        event.setMessageId("legacy-feedback-1");
        event.setEventType(MessagingConstants.ROUTING_FEEDBACK_CREATED);
        event.setPayload("{feedbackId=102, messageId=77, userId=6, title=Legacy payload, category=QUESTION, status=OPEN}");

        when(mqConsumeRecordMapper.selectCount(any())).thenReturn(0L);
        when(userFeedbackProjectionMapper.selectById(102L)).thenReturn(null);

        consumer.onFeedbackCreated(event);

        ArgumentCaptor<UserFeedbackProjection> projectionCaptor = ArgumentCaptor.forClass(UserFeedbackProjection.class);
        verify(userFeedbackProjectionMapper).insert(projectionCaptor.capture());
        UserFeedbackProjection projection = projectionCaptor.getValue();
        assertEquals(102L, projection.getFeedbackId());
        assertEquals(6L, projection.getUserId());
        assertEquals("Legacy payload", projection.getTitle());
        assertEquals("QUESTION", projection.getCategory());
        verify(mqConsumeRecordMapper).insert(any(MqConsumeRecord.class));
    }
}
