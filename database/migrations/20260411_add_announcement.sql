-- Add announcement table for existing deployments.
-- Usage:
--   docker compose exec -T mysql mysql -uroot -p123888 volunteer_platform < database/migrations/20260411_add_announcement.sql

SET NAMES utf8mb4;
USE volunteer_platform;

CREATE TABLE IF NOT EXISTS vol_announcement (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    title             VARCHAR(200)  NOT NULL COMMENT '公告标题',
    content           TEXT          NOT NULL COMMENT '公告正文',
    image_key         TEXT          COMMENT '公告图片对象键列表，逗号分隔',
    activity_id       BIGINT        COMMENT '关联活动ID，可为空',
    status            VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED-已发布, OFFLINE-已下线',
    sort_order        INT           NOT NULL DEFAULT 0 COMMENT '排序值，越大越靠前',
    publisher_id      BIGINT        COMMENT '发布管理员ID',
    publish_time      DATETIME      COMMENT '发布时间',
    create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_sort (status, sort_order, publish_time),
    INDEX idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

INSERT INTO vol_announcement (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT '四月志愿服务月安排发布',
       '四月志愿服务月已开启，图书馆整理、社区清洁、运动会保障等活动将陆续开放报名。请同学们关注活动时间，合理安排课业与志愿服务。',
       NULL,
       1,
       'PUBLISHED',
       30,
       1,
       '2026-03-25 09:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement
    WHERE publisher_id = 1 AND sort_order = 30 AND publish_time = '2026-03-25 09:00:00'
);

INSERT INTO vol_announcement (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT '运动会志愿保障招募提醒',
       '校运会志愿服务岗位包含检录协助、物资搬运、赛道引导和观众秩序维护。报名成功后请保持电话畅通，并按活动详情中的集合时间到场。',
       NULL,
       3,
       'PUBLISHED',
       20,
       1,
       '2026-03-26 10:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement
    WHERE publisher_id = 1 AND sort_order = 20 AND publish_time = '2026-03-26 10:00:00'
);

INSERT INTO vol_announcement (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT '志愿时长核销说明',
       '活动结束后，管理员会根据签到情况确认志愿时长。若长时间未核销，请先确认是否已完成签到，再联系活动负责人处理。',
       NULL,
       NULL,
       'PUBLISHED',
       10,
       1,
       '2026-03-27 10:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement
    WHERE publisher_id = 1 AND sort_order = 10 AND publish_time = '2026-03-27 10:00:00'
);

UPDATE vol_announcement
SET title = '四月志愿服务月安排发布',
    content = '四月志愿服务月已开启，图书馆整理、社区清洁、运动会保障等活动将陆续开放报名。请同学们关注活动时间，合理安排课业与志愿服务。',
    image_key = NULL,
    activity_id = 1,
    status = 'PUBLISHED'
WHERE publisher_id = 1 AND sort_order = 30 AND publish_time = '2026-03-25 09:00:00';

UPDATE vol_announcement
SET title = '运动会志愿保障招募提醒',
    content = '校运会志愿服务岗位包含检录协助、物资搬运、赛道引导和观众秩序维护。报名成功后请保持电话畅通，并按活动详情中的集合时间到场。',
    image_key = NULL,
    activity_id = 3,
    status = 'PUBLISHED'
WHERE publisher_id = 1 AND sort_order = 20 AND publish_time = '2026-03-26 10:00:00';

UPDATE vol_announcement
SET title = '志愿时长核销说明',
    content = '活动结束后，管理员会根据签到情况确认志愿时长。若长时间未核销，请先确认是否已完成签到，再联系活动负责人处理。',
    image_key = NULL,
    activity_id = NULL,
    status = 'PUBLISHED'
WHERE publisher_id = 1 AND sort_order = 10 AND publish_time = '2026-03-27 10:00:00';
