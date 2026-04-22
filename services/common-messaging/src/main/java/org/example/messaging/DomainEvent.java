package org.example.messaging;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DomainEvent {

    private String messageId;

    private String eventType;

    private String aggregateType;

    private String aggregateId;

    private String stackId;

    private String serviceName;

    private LocalDateTime occurredAt;

    private Object payload;
}
