package org.example.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.entity.User;
import org.example.mapper.MqConsumeRecordMapper;
import org.example.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class UserUpdatedConsumerIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("user_service_test")
            .withUsername("root")
            .withPassword("123888")
            .withInitScript("testcontainers/user-service-init.sql");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.cloud.nacos.discovery.enabled", () -> false);
        registry.add("spring.cloud.nacos.enabled", () -> false);
        registry.add("management.tracing.enabled", () -> false);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MqConsumeRecordMapper mqConsumeRecordMapper;

    @Test
    void userUpdatedEventShouldUpdateVolunteerHoursAndRecordConsumption() throws Exception {
        DomainEvent event = new DomainEvent();
        event.setMessageId("integration-msg-1");
        event.setEventType(MessagingConstants.ROUTING_USER_UPDATED);
        event.setAggregateType("user");
        event.setAggregateId("5");
        event.setOccurredAt(LocalDateTime.now());
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", 5);
        payload.put("hours", 2.5);
        event.setPayload(payload);

        rabbitTemplate.convertAndSend(MessagingConstants.EVENT_EXCHANGE, MessagingConstants.ROUTING_USER_UPDATED, event);

        long deadline = System.currentTimeMillis() + 10000;
        User updated = null;
        while (System.currentTimeMillis() < deadline) {
            updated = userMapper.selectById(5L);
            long consumeCount = mqConsumeRecordMapper.selectCount(new LambdaQueryWrapper<MqConsumeRecord>()
                    .eq(MqConsumeRecord::getMessageId, "integration-msg-1")
                    .eq(MqConsumeRecord::getConsumerName, "user-service.user-updated-consumer"));
            if (updated != null && new BigDecimal("4.00").compareTo(updated.getTotalVolunteerHours()) == 0 && consumeCount == 1L) {
                break;
            }
            Thread.sleep(250);
        }

        updated = userMapper.selectById(5L);
        assertEquals(0, new BigDecimal("4.00").compareTo(updated.getTotalVolunteerHours()));
        assertEquals(1L, mqConsumeRecordMapper.selectCount(new LambdaQueryWrapper<MqConsumeRecord>()
                .eq(MqConsumeRecord::getMessageId, "integration-msg-1")
                .eq(MqConsumeRecord::getConsumerName, "user-service.user-updated-consumer")));
    }
}
