package org.example.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.mapper.EventOutboxMapper;
import org.example.messaging.outbox.EventOutbox;
import org.example.messaging.outbox.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ActivityOutboxPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ActivityOutboxPublisher.class);
    private static final int MAX_RETRY = 8;

    private final EventOutboxMapper eventOutboxMapper;
    private final RabbitTemplate rabbitTemplate;

    public ActivityOutboxPublisher(EventOutboxMapper eventOutboxMapper, RabbitTemplate rabbitTemplate) {
        this.eventOutboxMapper = eventOutboxMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        rabbitTemplate.convertAndSend(MessagingConstants.EVENT_EXCHANGE, event.getEventType(), event, message -> {
            message.getMessageProperties().setMessageId(event.getMessageId());
            message.getMessageProperties().setHeader("messageId", event.getMessageId());
            message.getMessageProperties().setHeader("eventType", event.getEventType());
            message.getMessageProperties().setHeader("stackId", event.getStackId());
            message.getMessageProperties().setHeader("serviceName", event.getServiceName());
            return message;
        });
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    @Transactional(rollbackFor = Exception.class)
    public void publishPendingOutbox() {
        List<EventOutbox> pendingEvents = eventOutboxMapper.selectList(new LambdaQueryWrapper<EventOutbox>()
                .eq(EventOutbox::getStatus, OutboxStatus.PENDING)
                .le(EventOutbox::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(EventOutbox::getId)
                .last("LIMIT 50"));

        for (EventOutbox outbox : pendingEvents) {
            try {
                DomainEvent event = new DomainEvent();
                event.setMessageId(outbox.getMessageId());
                event.setEventType(outbox.getEventType());
                event.setAggregateType(outbox.getAggregateType());
                event.setAggregateId(outbox.getAggregateId());
                event.setStackId(System.getenv().getOrDefault("STACK_ID", "single"));
                event.setServiceName(System.getenv().getOrDefault("SERVICE_NAME", "activity-service"));
                event.setOccurredAt(LocalDateTime.now());
                event.setPayload(outbox.getPayloadJson());

                publish(event);

                outbox.setStatus(OutboxStatus.SENT);
                outbox.setSentAt(LocalDateTime.now());
                eventOutboxMapper.updateById(outbox);
                log.info("published outbox event stackId={} serviceName={} eventType={} messageId={} outboxId={}",
                        event.getStackId(), event.getServiceName(), event.getEventType(), event.getMessageId(), outbox.getId());
            } catch (Exception ex) {
                int nextRetryCount = (outbox.getRetryCount() == null ? 0 : outbox.getRetryCount()) + 1;
                outbox.setRetryCount(nextRetryCount);
                if (nextRetryCount >= MAX_RETRY) {
                    outbox.setStatus(OutboxStatus.FAILED);
                }
                outbox.setNextRetryTime(LocalDateTime.now().plusSeconds(Math.min(300, (long) Math.pow(2, nextRetryCount))));
                eventOutboxMapper.updateById(outbox);
                log.warn("failed to publish outbox event stackId={} serviceName={} eventType={} messageId={} outboxId={} retryCount={}",
                        System.getenv().getOrDefault("STACK_ID", "single"),
                        System.getenv().getOrDefault("SERVICE_NAME", "activity-service"),
                        outbox.getEventType(),
                        outbox.getMessageId(),
                        outbox.getId(),
                        nextRetryCount,
                        ex);
            }
        }
    }
}
