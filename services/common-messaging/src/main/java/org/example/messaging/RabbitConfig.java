package org.example.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(MessagingConstants.EVENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(MessagingConstants.EVENT_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Declarables messagingDeclarables() {
        Queue activityQueue = durableQueueWithDlq(MessagingConstants.QUEUE_ACTIVITY_CREATED, MessagingConstants.DLQ_ACTIVITY_CREATED);
        Queue announcementQueue = durableQueueWithDlq(MessagingConstants.QUEUE_ANNOUNCEMENT_PUBLISHED, MessagingConstants.DLQ_ANNOUNCEMENT_PUBLISHED);
        Queue feedbackQueue = durableQueueWithDlq(MessagingConstants.QUEUE_FEEDBACK_CREATED, MessagingConstants.DLQ_FEEDBACK_CREATED);
        Queue userQueue = durableQueueWithDlq(MessagingConstants.QUEUE_USER_UPDATED, MessagingConstants.DLQ_USER_UPDATED);

        Queue activityDlq = QueueBuilder.durable(MessagingConstants.DLQ_ACTIVITY_CREATED).build();
        Queue announcementDlq = QueueBuilder.durable(MessagingConstants.DLQ_ANNOUNCEMENT_PUBLISHED).build();
        Queue feedbackDlq = QueueBuilder.durable(MessagingConstants.DLQ_FEEDBACK_CREATED).build();
        Queue userDlq = QueueBuilder.durable(MessagingConstants.DLQ_USER_UPDATED).build();

        Binding activityBinding = BindingBuilder.bind(activityQueue).to(eventExchange()).with(MessagingConstants.ROUTING_ACTIVITY_CREATED);
        Binding activityUpsertedBinding = BindingBuilder.bind(activityQueue).to(eventExchange()).with(MessagingConstants.ROUTING_ACTIVITY_UPSERTED);
        Binding activityDeletedBinding = BindingBuilder.bind(activityQueue).to(eventExchange()).with(MessagingConstants.ROUTING_ACTIVITY_DELETED);
        Binding announcementBinding = BindingBuilder.bind(announcementQueue).to(eventExchange()).with(MessagingConstants.ROUTING_ANNOUNCEMENT_PUBLISHED);
        Binding feedbackBinding = BindingBuilder.bind(feedbackQueue).to(eventExchange()).with(MessagingConstants.ROUTING_FEEDBACK_CREATED);
        Binding userBinding = BindingBuilder.bind(userQueue).to(eventExchange()).with(MessagingConstants.ROUTING_USER_UPDATED);

        Binding activityDlqBinding = BindingBuilder.bind(activityDlq).to(deadLetterExchange()).with(MessagingConstants.DLQ_ACTIVITY_CREATED);
        Binding announcementDlqBinding = BindingBuilder.bind(announcementDlq).to(deadLetterExchange()).with(MessagingConstants.DLQ_ANNOUNCEMENT_PUBLISHED);
        Binding feedbackDlqBinding = BindingBuilder.bind(feedbackDlq).to(deadLetterExchange()).with(MessagingConstants.DLQ_FEEDBACK_CREATED);
        Binding userDlqBinding = BindingBuilder.bind(userDlq).to(deadLetterExchange()).with(MessagingConstants.DLQ_USER_UPDATED);

        return new Declarables(
                activityQueue,
                announcementQueue,
                feedbackQueue,
                userQueue,
                activityDlq,
                announcementDlq,
                feedbackDlq,
                userDlq,
                activityBinding,
                activityUpsertedBinding,
                activityDeletedBinding,
                announcementBinding,
                feedbackBinding,
                userBinding,
                activityDlqBinding,
                announcementDlqBinding,
                feedbackDlqBinding,
                userDlqBinding
        );
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    private Queue durableQueueWithDlq(String queueName, String dlqRoutingKey) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(MessagingConstants.EVENT_DLX_EXCHANGE)
                .deadLetterRoutingKey(dlqRoutingKey)
                .build();
    }
}
