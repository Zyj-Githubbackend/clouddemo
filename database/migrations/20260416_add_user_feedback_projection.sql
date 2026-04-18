-- Add user-service local projection for feedback.created events and backfill existing tickets.
-- Usage:
--   docker compose exec -T mysql mysql -uroot -p123888 < database/migrations/20260416_add_user_feedback_projection.sql

SET NAMES utf8mb4;
USE user_service_db;

CREATE TABLE IF NOT EXISTS user_feedback_projection (
    feedback_id       BIGINT       PRIMARY KEY COMMENT 'Feedback ID from feedback-service',
    user_id           BIGINT       NOT NULL COMMENT 'Feedback owner user ID',
    title             VARCHAR(200) NOT NULL COMMENT 'Feedback title snapshot',
    category          VARCHAR(30)  NOT NULL COMMENT 'Feedback category snapshot',
    status            VARCHAR(30)  NOT NULL COMMENT 'Feedback status snapshot',
    source_message_id VARCHAR(64)  COMMENT 'Last source MQ message ID',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_feedback_user (user_id),
    INDEX idx_user_feedback_status (status),
    INDEX idx_user_feedback_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-service local projection of feedback tickets';

INSERT INTO user_feedback_projection
    (feedback_id, user_id, title, category, status, source_message_id, created_at, updated_at)
SELECT
    f.id,
    f.user_id,
    f.title,
    f.category,
    f.status,
    NULL,
    COALESCE(f.create_time, NOW()),
    COALESCE(f.update_time, NOW())
FROM feedback_service_db.feedback f
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    title = VALUES(title),
    category = VALUES(category),
    status = VALUES(status),
    updated_at = VALUES(updated_at);
