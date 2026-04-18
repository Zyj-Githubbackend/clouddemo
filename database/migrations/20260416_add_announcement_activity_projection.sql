-- Add announcement-side activity projection table and backfill local snapshots.
-- Usage:
--   docker compose exec -T mysql mysql -uroot -p123888 < database/migrations/20260416_add_announcement_activity_projection.sql

SET NAMES utf8mb4;
USE announcement_service_db;

CREATE TABLE IF NOT EXISTS vol_announcement_activity_projection (
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

INSERT INTO vol_announcement_activity_projection (id, title, location, start_time, end_time, status, category)
SELECT id, title, location, start_time, end_time, status, category
FROM activity_service_db.vol_activity
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    location = VALUES(location),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    status = VALUES(status),
    category = VALUES(category);
