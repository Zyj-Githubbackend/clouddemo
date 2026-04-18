package org.example.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.UserFeedbackProjection;
import org.example.mapper.MqConsumeRecordMapper;
import org.example.mapper.UserFeedbackProjectionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class FeedbackCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(FeedbackCreatedConsumer.class);
    private static final String CONSUMER_NAME = "user-service.feedback-created-consumer";

    private final MqConsumeRecordMapper mqConsumeRecordMapper;
    private final UserFeedbackProjectionMapper userFeedbackProjectionMapper;
    private final ObjectMapper objectMapper;

    public FeedbackCreatedConsumer(MqConsumeRecordMapper mqConsumeRecordMapper,
                                   UserFeedbackProjectionMapper userFeedbackProjectionMapper,
                                   ObjectMapper objectMapper) {
        this.mqConsumeRecordMapper = mqConsumeRecordMapper;
        this.userFeedbackProjectionMapper = userFeedbackProjectionMapper;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = MessagingConstants.QUEUE_FEEDBACK_CREATED)
    @Transactional(rollbackFor = Exception.class)
    public void onFeedbackCreated(DomainEvent event) {
        if (event == null || event.getMessageId() == null) {
            return;
        }
        boolean duplicated = mqConsumeRecordMapper.selectCount(new LambdaQueryWrapper<MqConsumeRecord>()
                .eq(MqConsumeRecord::getMessageId, event.getMessageId())
                .eq(MqConsumeRecord::getConsumerName, CONSUMER_NAME)) > 0;
        if (duplicated) {
            log.info("skip duplicated event stackId={} serviceName={} eventType={} messageId={} consumerName={}",
                    System.getenv().getOrDefault("STACK_ID", "single"),
                    System.getenv().getOrDefault("SERVICE_NAME", "user-service"),
                    event.getEventType(),
                    event.getMessageId(),
                    CONSUMER_NAME);
            return;
        }

        FeedbackCreatedPayload payload = parsePayload(event);
        if (payload.feedbackId == null || payload.userId == null) {
            throw new IllegalArgumentException("feedback.created payload missing feedbackId/userId");
        }
        upsertProjection(payload, event.getMessageId());

        MqConsumeRecord consumeRecord = new MqConsumeRecord();
        consumeRecord.setMessageId(event.getMessageId());
        consumeRecord.setConsumerName(CONSUMER_NAME);
        consumeRecord.setStatus("CONSUMED");
        consumeRecord.setConsumedAt(LocalDateTime.now());
        mqConsumeRecordMapper.insert(consumeRecord);

        log.info("consumed event stackId={} serviceName={} eventType={} messageId={} consumerName={} aggregateId={} feedbackId={} userId={}",
                System.getenv().getOrDefault("STACK_ID", "single"),
                System.getenv().getOrDefault("SERVICE_NAME", "user-service"),
                event.getEventType(),
                event.getMessageId(),
                CONSUMER_NAME,
                event.getAggregateId(),
                payload.feedbackId,
                payload.userId);
    }

    private void upsertProjection(FeedbackCreatedPayload payload, String messageId) {
        UserFeedbackProjection projection = userFeedbackProjectionMapper.selectById(payload.feedbackId);
        boolean isNew = projection == null;
        if (isNew) {
            projection = new UserFeedbackProjection();
            projection.setFeedbackId(payload.feedbackId);
            projection.setCreatedAt(payload.createdAt == null ? LocalDateTime.now() : payload.createdAt);
        }

        projection.setUserId(payload.userId);
        projection.setTitle(payload.title);
        projection.setCategory(payload.category);
        projection.setStatus(payload.status);
        projection.setSourceMessageId(messageId);
        projection.setUpdatedAt(LocalDateTime.now());

        if (isNew) {
            userFeedbackProjectionMapper.insert(projection);
        } else {
            userFeedbackProjectionMapper.updateById(projection);
        }
    }

    private FeedbackCreatedPayload parsePayload(DomainEvent event) {
        try {
            if (event.getPayload() instanceof String payloadJson) {
                try {
                    return objectMapper.readValue(payloadJson, FeedbackCreatedPayload.class);
                } catch (Exception jsonEx) {
                    FeedbackCreatedPayload legacyPayload = parseLegacyPayload(payloadJson);
                    if (legacyPayload != null) {
                        return legacyPayload;
                    }
                    throw jsonEx;
                }
            }
            return objectMapper.convertValue(event.getPayload(), FeedbackCreatedPayload.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid feedback.created payload", ex);
        }
    }

    private FeedbackCreatedPayload parseLegacyPayload(String payloadText) {
        if (payloadText == null || !payloadText.startsWith("{") || !payloadText.endsWith("}") || !payloadText.contains("=")) {
            return null;
        }

        String body = payloadText.substring(1, payloadText.length() - 1).trim();
        if (body.isEmpty()) {
            return null;
        }

        FeedbackCreatedPayload payload = new FeedbackCreatedPayload();
        for (String segment : body.split(",\\s*")) {
            int delimiterIndex = segment.indexOf('=');
            if (delimiterIndex <= 0) {
                continue;
            }
            String key = segment.substring(0, delimiterIndex).trim();
            String value = segment.substring(delimiterIndex + 1).trim();
            if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
                continue;
            }
            switch (key) {
                case "feedbackId" -> payload.feedbackId = Long.valueOf(value);
                case "userId" -> payload.userId = Long.valueOf(value);
                case "title" -> payload.title = value;
                case "category" -> payload.category = value;
                case "status" -> payload.status = value;
                case "createdAt" -> payload.createdAt = LocalDateTime.parse(value);
                default -> {
                    // ignore unknown keys from legacy payload
                }
            }
        }
        return payload;
    }

    private static final class FeedbackCreatedPayload {
        public Long feedbackId;
        public Long userId;
        public String title;
        public String category;
        public String status;
        public LocalDateTime createdAt;
    }
}
