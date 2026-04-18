SET NAMES utf8mb4;

USE user_service_db;

CREATE TABLE IF NOT EXISTS event_outbox (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Outbox ID',
    message_id      VARCHAR(64)  NOT NULL COMMENT 'Business message ID',
    event_type      VARCHAR(100) NOT NULL COMMENT 'Event type',
    aggregate_type  VARCHAR(100) NOT NULL COMMENT 'Aggregate type',
    aggregate_id    VARCHAR(100) NOT NULL COMMENT 'Aggregate ID',
    payload_json    LONGTEXT     NOT NULL COMMENT 'Event payload JSON',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT 'Retry count',
    next_retry_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Next retry time',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at         DATETIME     NULL,
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_next_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Transactional outbox events';

CREATE TABLE IF NOT EXISTS mq_consume_record (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Consume record ID',
    message_id    VARCHAR(64)  NOT NULL COMMENT 'Message ID',
    consumer_name VARCHAR(120) NOT NULL COMMENT 'Consumer name',
    status        VARCHAR(20)  NOT NULL DEFAULT 'CONSUMED' COMMENT 'Consume status',
    consumed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_consumer (message_id, consumer_name),
    KEY idx_consumer_time (consumer_name, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ consume idempotency records';

USE activity_service_db;

CREATE TABLE IF NOT EXISTS event_outbox (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Outbox ID',
    message_id      VARCHAR(64)  NOT NULL COMMENT 'Business message ID',
    event_type      VARCHAR(100) NOT NULL COMMENT 'Event type',
    aggregate_type  VARCHAR(100) NOT NULL COMMENT 'Aggregate type',
    aggregate_id    VARCHAR(100) NOT NULL COMMENT 'Aggregate ID',
    payload_json    LONGTEXT     NOT NULL COMMENT 'Event payload JSON',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT 'Retry count',
    next_retry_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Next retry time',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at         DATETIME     NULL,
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_next_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Transactional outbox events';

CREATE TABLE IF NOT EXISTS mq_consume_record (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Consume record ID',
    message_id    VARCHAR(64)  NOT NULL COMMENT 'Message ID',
    consumer_name VARCHAR(120) NOT NULL COMMENT 'Consumer name',
    status        VARCHAR(20)  NOT NULL DEFAULT 'CONSUMED' COMMENT 'Consume status',
    consumed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_consumer (message_id, consumer_name),
    KEY idx_consumer_time (consumer_name, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ consume idempotency records';

USE announcement_service_db;

CREATE TABLE IF NOT EXISTS event_outbox (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Outbox ID',
    message_id      VARCHAR(64)  NOT NULL COMMENT 'Business message ID',
    event_type      VARCHAR(100) NOT NULL COMMENT 'Event type',
    aggregate_type  VARCHAR(100) NOT NULL COMMENT 'Aggregate type',
    aggregate_id    VARCHAR(100) NOT NULL COMMENT 'Aggregate ID',
    payload_json    LONGTEXT     NOT NULL COMMENT 'Event payload JSON',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT 'Retry count',
    next_retry_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Next retry time',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at         DATETIME     NULL,
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_next_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Transactional outbox events';

CREATE TABLE IF NOT EXISTS mq_consume_record (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Consume record ID',
    message_id    VARCHAR(64)  NOT NULL COMMENT 'Message ID',
    consumer_name VARCHAR(120) NOT NULL COMMENT 'Consumer name',
    status        VARCHAR(20)  NOT NULL DEFAULT 'CONSUMED' COMMENT 'Consume status',
    consumed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_consumer (message_id, consumer_name),
    KEY idx_consumer_time (consumer_name, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ consume idempotency records';

USE feedback_service_db;

CREATE TABLE IF NOT EXISTS event_outbox (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Outbox ID',
    message_id      VARCHAR(64)  NOT NULL COMMENT 'Business message ID',
    event_type      VARCHAR(100) NOT NULL COMMENT 'Event type',
    aggregate_type  VARCHAR(100) NOT NULL COMMENT 'Aggregate type',
    aggregate_id    VARCHAR(100) NOT NULL COMMENT 'Aggregate ID',
    payload_json    LONGTEXT     NOT NULL COMMENT 'Event payload JSON',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT 'Retry count',
    next_retry_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Next retry time',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at         DATETIME     NULL,
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_next_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Transactional outbox events';

CREATE TABLE IF NOT EXISTS mq_consume_record (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Consume record ID',
    message_id    VARCHAR(64)  NOT NULL COMMENT 'Message ID',
    consumer_name VARCHAR(120) NOT NULL COMMENT 'Consumer name',
    status        VARCHAR(20)  NOT NULL DEFAULT 'CONSUMED' COMMENT 'Consume status',
    consumed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_consumer (message_id, consumer_name),
    KEY idx_consumer_time (consumer_name, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ consume idempotency records';
