package org.example.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.ActivityProjection;
import org.example.mapper.ActivityProjectionMapper;
import org.example.mapper.MqConsumeRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ActivityProjectionConsumer {

    private static final Logger log = LoggerFactory.getLogger(ActivityProjectionConsumer.class);
    private static final String CONSUMER_NAME = "announcement-service.activity-projection-consumer";

    private final MqConsumeRecordMapper mqConsumeRecordMapper;
    private final ActivityProjectionMapper activityProjectionMapper;
    private final ObjectMapper objectMapper;

    public ActivityProjectionConsumer(MqConsumeRecordMapper mqConsumeRecordMapper,
                                      ActivityProjectionMapper activityProjectionMapper,
                                      ObjectMapper objectMapper) {
        this.mqConsumeRecordMapper = mqConsumeRecordMapper;
        this.activityProjectionMapper = activityProjectionMapper;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = MessagingConstants.QUEUE_ACTIVITY_CREATED)
    @Transactional(rollbackFor = Exception.class)
    public void onActivityChanged(DomainEvent event) {
        if (event == null || event.getMessageId() == null) {
            return;
        }
        boolean duplicated = mqConsumeRecordMapper.selectCount(new LambdaQueryWrapper<MqConsumeRecord>()
                .eq(MqConsumeRecord::getMessageId, event.getMessageId())
                .eq(MqConsumeRecord::getConsumerName, CONSUMER_NAME)) > 0;
        if (duplicated) {
            log.info("skip duplicated event stackId={} serviceName={} eventType={} messageId={} consumerName={}",
                    System.getenv().getOrDefault("STACK_ID", "single"),
                    System.getenv().getOrDefault("SERVICE_NAME", "announcement-service"),
                    event.getEventType(),
                    event.getMessageId(),
                    CONSUMER_NAME);
            return;
        }

        ActivityProjectionPayload payload = parsePayload(event);
        if (payload.activityId == null) {
            throw new IllegalArgumentException("activity projection payload missing activityId");
        }
        if (MessagingConstants.ROUTING_ACTIVITY_DELETED.equals(event.getEventType())) {
            activityProjectionMapper.deleteById(payload.activityId);
        } else if (MessagingConstants.ROUTING_ACTIVITY_UPSERTED.equals(event.getEventType())
                || MessagingConstants.ROUTING_ACTIVITY_CREATED.equals(event.getEventType())) {
            upsertProjection(payload);
        } else {
            throw new IllegalArgumentException("unsupported activity event type: " + event.getEventType());
        }

        MqConsumeRecord consumeRecord = new MqConsumeRecord();
        consumeRecord.setMessageId(event.getMessageId());
        consumeRecord.setConsumerName(CONSUMER_NAME);
        consumeRecord.setStatus("CONSUMED");
        consumeRecord.setConsumedAt(LocalDateTime.now());
        mqConsumeRecordMapper.insert(consumeRecord);

        log.info("consumed event stackId={} serviceName={} eventType={} messageId={} consumerName={} aggregateId={} activityId={}",
                System.getenv().getOrDefault("STACK_ID", "single"),
                System.getenv().getOrDefault("SERVICE_NAME", "announcement-service"),
                event.getEventType(),
                event.getMessageId(),
                CONSUMER_NAME,
                event.getAggregateId(),
                payload.activityId);
    }

    private void upsertProjection(ActivityProjectionPayload payload) {
        ActivityProjection projection = activityProjectionMapper.selectById(payload.activityId);
        boolean isNew = projection == null;
        if (isNew) {
            projection = new ActivityProjection();
            projection.setId(payload.activityId);
        }

        projection.setTitle(payload.title);
        projection.setLocation(payload.location);
        projection.setStartTime(payload.startTime);
        projection.setEndTime(payload.endTime);
        projection.setStatus(payload.status);
        projection.setCategory(payload.category);

        if (isNew) {
            activityProjectionMapper.insert(projection);
        } else {
            activityProjectionMapper.updateById(projection);
        }
    }

    private ActivityProjectionPayload parsePayload(DomainEvent event) {
        try {
            if (event.getPayload() instanceof String payloadJson) {
                return objectMapper.readValue(payloadJson, ActivityProjectionPayload.class);
            }
            return objectMapper.convertValue(event.getPayload(), ActivityProjectionPayload.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid activity projection payload", ex);
        }
    }

    private static final class ActivityProjectionPayload {
        public Long activityId;
        public String title;
        public String location;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public String status;
        public String category;
    }
}
