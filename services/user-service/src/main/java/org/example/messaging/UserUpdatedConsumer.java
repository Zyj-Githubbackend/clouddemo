package org.example.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.mapper.MqConsumeRecordMapper;
import org.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class UserUpdatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserUpdatedConsumer.class);
    private static final String CONSUMER_NAME = "user-service.user-updated-consumer";

    private final MqConsumeRecordMapper mqConsumeRecordMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public UserUpdatedConsumer(MqConsumeRecordMapper mqConsumeRecordMapper,
                               UserService userService,
                               ObjectMapper objectMapper) {
        this.mqConsumeRecordMapper = mqConsumeRecordMapper;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = MessagingConstants.QUEUE_USER_UPDATED)
    @Transactional(rollbackFor = Exception.class)
    public void onUserUpdated(DomainEvent event) {
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

        UserUpdatedPayload payload = parsePayload(event);
        userService.updateVolunteerHours(payload.userId, payload.hours);

        MqConsumeRecord consumeRecord = new MqConsumeRecord();
        consumeRecord.setMessageId(event.getMessageId());
        consumeRecord.setConsumerName(CONSUMER_NAME);
        consumeRecord.setStatus("CONSUMED");
        consumeRecord.setConsumedAt(LocalDateTime.now());
        mqConsumeRecordMapper.insert(consumeRecord);

        log.info("consumed event stackId={} serviceName={} eventType={} messageId={} consumerName={} userId={} hours={} aggregateId={}",
                System.getenv().getOrDefault("STACK_ID", "single"),
                System.getenv().getOrDefault("SERVICE_NAME", "user-service"),
                event.getEventType(),
                event.getMessageId(),
                CONSUMER_NAME,
                payload.userId,
                payload.hours,
                event.getAggregateId());
    }

    private UserUpdatedPayload parsePayload(DomainEvent event) {
        try {
            if (event.getPayload() instanceof String payloadJson) {
                return objectMapper.readValue(payloadJson, UserUpdatedPayload.class);
            }
            return objectMapper.convertValue(event.getPayload(), UserUpdatedPayload.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid user.updated payload", ex);
        }
    }

    private static final class UserUpdatedPayload {
        public Long userId;
        public BigDecimal hours;
    }
}
