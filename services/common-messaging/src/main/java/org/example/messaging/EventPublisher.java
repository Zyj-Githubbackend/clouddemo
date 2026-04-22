package org.example.messaging;

public interface EventPublisher {

    void publish(DomainEvent event);
}
