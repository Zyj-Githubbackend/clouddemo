-- Split the legacy shared database volunteer_platform into per-service databases.
-- Expected source schema: the current shared-database deployment already contains
-- user/activity/announcement/feedback tables plus event_outbox and mq_consume_record.
--
-- Usage:
--   docker compose exec -T mysql mysql -uroot -p123888 < database/migrations/20260416_split_service_databases.sql

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS user_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS activity_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS announcement_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS feedback_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'user_service'@'%' IDENTIFIED BY '123888';
CREATE USER IF NOT EXISTS 'activity_service'@'%' IDENTIFIED BY '123888';
CREATE USER IF NOT EXISTS 'announcement_service'@'%' IDENTIFIED BY '123888';
CREATE USER IF NOT EXISTS 'feedback_service'@'%' IDENTIFIED BY '123888';

GRANT ALL PRIVILEGES ON user_service_db.* TO 'user_service'@'%';
GRANT ALL PRIVILEGES ON activity_service_db.* TO 'activity_service'@'%';
GRANT ALL PRIVILEGES ON announcement_service_db.* TO 'announcement_service'@'%';
GRANT ALL PRIVILEGES ON feedback_service_db.* TO 'feedback_service'@'%';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS user_service_db.sys_user LIKE volunteer_platform.sys_user;
CREATE TABLE IF NOT EXISTS user_service_db.event_outbox LIKE volunteer_platform.event_outbox;
CREATE TABLE IF NOT EXISTS user_service_db.mq_consume_record LIKE volunteer_platform.mq_consume_record;
CREATE TABLE IF NOT EXISTS user_service_db.user_feedback_projection (
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

INSERT IGNORE INTO user_service_db.sys_user
SELECT *
FROM volunteer_platform.sys_user;

INSERT IGNORE INTO user_service_db.event_outbox
SELECT *
FROM volunteer_platform.event_outbox
WHERE aggregate_type = 'user';

INSERT IGNORE INTO user_service_db.mq_consume_record
SELECT *
FROM volunteer_platform.mq_consume_record
WHERE consumer_name LIKE 'user-service.%';

INSERT INTO user_service_db.user_feedback_projection
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
FROM volunteer_platform.feedback f
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    title = VALUES(title),
    category = VALUES(category),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

CREATE TABLE IF NOT EXISTS activity_service_db.vol_activity LIKE volunteer_platform.vol_activity;
CREATE TABLE IF NOT EXISTS activity_service_db.vol_registration LIKE volunteer_platform.vol_registration;
CREATE TABLE IF NOT EXISTS activity_service_db.event_outbox LIKE volunteer_platform.event_outbox;
CREATE TABLE IF NOT EXISTS activity_service_db.mq_consume_record LIKE volunteer_platform.mq_consume_record;

INSERT IGNORE INTO activity_service_db.vol_activity
SELECT *
FROM volunteer_platform.vol_activity;

INSERT IGNORE INTO activity_service_db.vol_registration
SELECT *
FROM volunteer_platform.vol_registration;

INSERT IGNORE INTO activity_service_db.event_outbox
SELECT *
FROM volunteer_platform.event_outbox
WHERE aggregate_type IN ('activity', 'user');

INSERT IGNORE INTO activity_service_db.mq_consume_record
SELECT *
FROM volunteer_platform.mq_consume_record
WHERE consumer_name LIKE 'activity-service.%';

DROP VIEW IF EXISTS activity_service_db.v_activity_statistics;
CREATE VIEW activity_service_db.v_activity_statistics AS
SELECT
    a.id,
    a.title,
    a.category,
    a.status,
    a.max_participants,
    a.current_participants,
    a.volunteer_hours,
    COUNT(r.id) AS total_registrations,
    SUM(CASE WHEN r.check_in_status = 1 THEN 1 ELSE 0 END) AS checked_in_count,
    SUM(CASE WHEN r.hours_confirmed = 1 THEN 1 ELSE 0 END) AS confirmed_count,
    a.start_time,
    a.end_time
FROM activity_service_db.vol_activity a
LEFT JOIN activity_service_db.vol_registration r
    ON r.activity_id = a.id AND r.status = 'REGISTERED'
GROUP BY
    a.id,
    a.title,
    a.category,
    a.status,
    a.max_participants,
    a.current_participants,
    a.volunteer_hours,
    a.start_time,
    a.end_time;

CREATE TABLE IF NOT EXISTS announcement_service_db.vol_announcement LIKE volunteer_platform.vol_announcement;
CREATE TABLE IF NOT EXISTS announcement_service_db.vol_announcement_activity LIKE volunteer_platform.vol_announcement_activity;
CREATE TABLE IF NOT EXISTS announcement_service_db.vol_announcement_attachment LIKE volunteer_platform.vol_announcement_attachment;
CREATE TABLE IF NOT EXISTS announcement_service_db.event_outbox LIKE volunteer_platform.event_outbox;
CREATE TABLE IF NOT EXISTS announcement_service_db.mq_consume_record LIKE volunteer_platform.mq_consume_record;

CREATE TABLE IF NOT EXISTS announcement_service_db.vol_announcement_activity_projection (
    id                BIGINT        PRIMARY KEY COMMENT 'Activity ID copied from activity-service',
    title             VARCHAR(200)  NOT NULL COMMENT 'Activity title snapshot',
    location          VARCHAR(200)  COMMENT 'Activity location snapshot',
    start_time        DATETIME      COMMENT 'Activity start time snapshot',
    end_time          DATETIME      COMMENT 'Activity end time snapshot',
    status            VARCHAR(20)   COMMENT 'Activity status snapshot',
    category          VARCHAR(50)   COMMENT 'Activity category snapshot',
    create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_projection_status (status),
    INDEX idx_projection_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Announcement local projection of activities';

INSERT IGNORE INTO announcement_service_db.vol_announcement
SELECT *
FROM volunteer_platform.vol_announcement;

INSERT IGNORE INTO announcement_service_db.vol_announcement_activity
SELECT *
FROM volunteer_platform.vol_announcement_activity;

INSERT IGNORE INTO announcement_service_db.vol_announcement_attachment
SELECT *
FROM volunteer_platform.vol_announcement_attachment;

INSERT INTO announcement_service_db.vol_announcement_activity_projection
    (id, title, location, start_time, end_time, status, category)
SELECT id, title, location, start_time, end_time, status, category
FROM volunteer_platform.vol_activity
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    location = VALUES(location),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    status = VALUES(status),
    category = VALUES(category);

INSERT IGNORE INTO announcement_service_db.mq_consume_record
SELECT *
FROM volunteer_platform.mq_consume_record
WHERE consumer_name LIKE 'announcement-service.%';

INSERT IGNORE INTO announcement_service_db.event_outbox
SELECT *
FROM volunteer_platform.event_outbox
WHERE aggregate_type = 'announcement';

CREATE TABLE IF NOT EXISTS feedback_service_db.feedback LIKE volunteer_platform.feedback;
CREATE TABLE IF NOT EXISTS feedback_service_db.feedback_message LIKE volunteer_platform.feedback_message;
CREATE TABLE IF NOT EXISTS feedback_service_db.feedback_message_attachment LIKE volunteer_platform.feedback_message_attachment;
CREATE TABLE IF NOT EXISTS feedback_service_db.event_outbox LIKE volunteer_platform.event_outbox;
CREATE TABLE IF NOT EXISTS feedback_service_db.mq_consume_record LIKE volunteer_platform.mq_consume_record;

INSERT IGNORE INTO feedback_service_db.feedback
SELECT *
FROM volunteer_platform.feedback;

INSERT IGNORE INTO feedback_service_db.feedback_message
SELECT *
FROM volunteer_platform.feedback_message;

INSERT IGNORE INTO feedback_service_db.feedback_message_attachment
SELECT *
FROM volunteer_platform.feedback_message_attachment;

INSERT IGNORE INTO feedback_service_db.event_outbox
SELECT *
FROM volunteer_platform.event_outbox
WHERE aggregate_type = 'feedback';

INSERT IGNORE INTO feedback_service_db.mq_consume_record
SELECT *
FROM volunteer_platform.mq_consume_record
WHERE consumer_name LIKE 'feedback-service.%';
