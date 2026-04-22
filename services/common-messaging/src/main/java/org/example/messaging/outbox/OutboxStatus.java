package org.example.messaging.outbox;

public final class OutboxStatus {

    private OutboxStatus() {
    }

    public static final String PENDING = "PENDING";
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
}
