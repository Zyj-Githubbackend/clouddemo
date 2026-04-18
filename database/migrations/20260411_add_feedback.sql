SET NAMES utf8mb4;

USE feedback_service_db;

CREATE TABLE IF NOT EXISTS feedback (
    id                 BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id            BIGINT        NOT NULL,
    title              VARCHAR(200)  NOT NULL,
    category           VARCHAR(30)   NOT NULL,
    status             VARCHAR(30)   NOT NULL DEFAULT 'OPEN',
    priority           VARCHAR(20)   NOT NULL DEFAULT 'NORMAL',
    last_message_time  DATETIME,
    last_replier_role  VARCHAR(20),
    closed_by          BIGINT,
    closed_time        DATETIME,
    reject_reason      VARCHAR(500),
    create_time        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feedback_user_status (user_id, status),
    INDEX idx_feedback_status_category (status, category),
    INDEX idx_feedback_priority (priority),
    INDEX idx_feedback_last_message_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Feedback tickets';

CREATE TABLE IF NOT EXISTS feedback_message (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    feedback_id    BIGINT       NOT NULL,
    sender_id      BIGINT       NOT NULL,
    sender_role    VARCHAR(20)  NOT NULL,
    content        TEXT,
    message_type   VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_feedback_message_feedback (feedback_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Feedback messages';

CREATE TABLE IF NOT EXISTS feedback_message_attachment (
    id             BIGINT        PRIMARY KEY AUTO_INCREMENT,
    feedback_id    BIGINT        NOT NULL,
    message_id     BIGINT        NOT NULL,
    object_key     VARCHAR(512)  NOT NULL,
    original_name  VARCHAR(255)  NOT NULL,
    content_type   VARCHAR(120),
    file_size      BIGINT        NOT NULL DEFAULT 0,
    file_type      VARCHAR(20)   NOT NULL,
    create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_feedback_attachment_feedback (feedback_id),
    INDEX idx_feedback_attachment_message (message_id),
    UNIQUE KEY uk_feedback_attachment_object_key (object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Feedback message attachments';
