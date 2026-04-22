package org.example.messaging;

public final class MessagingConstants {

    private MessagingConstants() {
    }

    public static final String EVENT_EXCHANGE = "volunteer.events.exchange";
    public static final String EVENT_DLX_EXCHANGE = "volunteer.events.dlx.exchange";

    public static final String ROUTING_ACTIVITY_CREATED = "activity.created";
    public static final String ROUTING_ACTIVITY_UPSERTED = "activity.upserted";
    public static final String ROUTING_ACTIVITY_DELETED = "activity.deleted";
    public static final String ROUTING_ANNOUNCEMENT_PUBLISHED = "announcement.published";
    public static final String ROUTING_FEEDBACK_CREATED = "feedback.created";
    public static final String ROUTING_USER_UPDATED = "user.updated";

    public static final String QUEUE_ACTIVITY_CREATED = "activity.created.queue";
    public static final String QUEUE_ANNOUNCEMENT_PUBLISHED = "announcement.published.queue";
    public static final String QUEUE_FEEDBACK_CREATED = "feedback.created.queue";
    public static final String QUEUE_USER_UPDATED = "user.updated.queue";

    public static final String DLQ_ACTIVITY_CREATED = "activity.created.dlq";
    public static final String DLQ_ANNOUNCEMENT_PUBLISHED = "announcement.published.dlq";
    public static final String DLQ_FEEDBACK_CREATED = "feedback.created.dlq";
    public static final String DLQ_USER_UPDATED = "user.updated.dlq";
}
