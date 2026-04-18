package org.example.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.ActivityProjection;
import org.example.mapper.ActivityProjectionMapper;
import org.example.mapper.MqConsumeRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityProjectionConsumerTest {

    @Mock
    private MqConsumeRecordMapper mqConsumeRecordMapper;

    @Mock
    private ActivityProjectionMapper activityProjectionMapper;

    @Test
    void onActivityChangedShouldInsertProjectionForUpsertEvent() {
        ActivityProjectionConsumer consumer = new ActivityProjectionConsumer(
                mqConsumeRecordMapper,
                activityProjectionMapper,
                new ObjectMapper().findAndRegisterModules()
        );
        DomainEvent event = new DomainEvent();
        event.setMessageId("msg-1");
        event.setEventType(MessagingConstants.ROUTING_ACTIVITY_UPSERTED);
        event.setAggregateId("18");
        event.setPayload("""
                {"activityId":18,"title":"Tree Planting","location":"North Gate","status":"RECRUITING","category":"GREEN","startTime":"2026-04-20T09:00:00","endTime":"2026-04-20T12:00:00"}
                """);

        when(mqConsumeRecordMapper.selectCount(any())).thenReturn(0L);
        when(activityProjectionMapper.selectById(18L)).thenReturn(null);

        consumer.onActivityChanged(event);

        ArgumentCaptor<ActivityProjection> projectionCaptor = ArgumentCaptor.forClass(ActivityProjection.class);
        verify(activityProjectionMapper).insert(projectionCaptor.capture());
        ActivityProjection projection = projectionCaptor.getValue();
        assertEquals(18L, projection.getId());
        assertEquals("Tree Planting", projection.getTitle());
        assertEquals("North Gate", projection.getLocation());
        verify(mqConsumeRecordMapper).insert(any(MqConsumeRecord.class));
    }

    @Test
    void onActivityChangedShouldDeleteProjectionForDeleteEvent() {
        ActivityProjectionConsumer consumer = new ActivityProjectionConsumer(
                mqConsumeRecordMapper,
                activityProjectionMapper,
                new ObjectMapper().findAndRegisterModules()
        );
        DomainEvent event = new DomainEvent();
        event.setMessageId("msg-2");
        event.setEventType(MessagingConstants.ROUTING_ACTIVITY_DELETED);
        event.setAggregateId("18");
        event.setPayload("{\"activityId\":18}");

        when(mqConsumeRecordMapper.selectCount(any())).thenReturn(0L);

        consumer.onActivityChanged(event);

        verify(activityProjectionMapper).deleteById(18L);
        verify(mqConsumeRecordMapper).insert(any(MqConsumeRecord.class));
    }
}
