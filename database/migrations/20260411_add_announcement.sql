-- Add announcement tables for split-database deployments.
-- Usage:
--   docker compose exec -T mysql mysql -uroot -p123888 < database/migrations/20260411_add_announcement.sql

SET NAMES utf8mb4;
USE announcement_service_db;

CREATE TABLE IF NOT EXISTS vol_announcement (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '鍏憡ID',
    title             VARCHAR(200)  NOT NULL COMMENT '鍏憡鏍囬',
    content           TEXT          NOT NULL COMMENT '鍏憡姝ｆ枃',
    image_key         TEXT          COMMENT '鍏憡鍥剧墖瀵硅薄閿垪琛紝閫楀彿鍒嗛殧',
    activity_id       BIGINT        COMMENT '鍏宠仈娲诲姩ID锛屽彲涓虹┖',
    status            VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED-宸插彂甯? OFFLINE-宸蹭笅绾?,
    sort_order        INT           NOT NULL DEFAULT 0 COMMENT '鎺掑簭鍊硷紝瓒婂ぇ瓒婇潬鍓?,
    publisher_id      BIGINT        COMMENT '鍙戝竷绠＄悊鍛業D',
    publish_time      DATETIME      COMMENT '鍙戝竷鏃堕棿',
    create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_sort (status, sort_order, publish_time),
    INDEX idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鍏憡琛?;

CREATE TABLE IF NOT EXISTS vol_announcement_activity (
    id                BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    announcement_id   BIGINT       NOT NULL COMMENT 'Announcement ID',
    activity_id       BIGINT       NOT NULL COMMENT 'Activity ID',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_announcement_activity (announcement_id, activity_id),
    INDEX idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Announcement activity links';

CREATE TABLE IF NOT EXISTS vol_announcement_attachment (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    announcement_id   BIGINT        NOT NULL COMMENT 'Announcement ID',
    object_key        VARCHAR(512)  NOT NULL COMMENT 'MinIO object key',
    original_name     VARCHAR(255)  NOT NULL COMMENT 'Original file name',
    content_type      VARCHAR(120)  COMMENT 'Content type',
    file_size         BIGINT        NOT NULL DEFAULT 0 COMMENT 'File size in bytes',
    sort_order        INT           NOT NULL DEFAULT 0 COMMENT 'Sort order',
    create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_announcement_id (announcement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Announcement attachments';

INSERT INTO vol_announcement (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT '鍥涙湀蹇楁効鏈嶅姟鏈堝畨鎺掑彂甯?,
       '鍥涙湀蹇楁効鏈嶅姟鏈堝凡寮€鍚紝鍥句功棣嗘暣鐞嗐€佺ぞ鍖烘竻娲併€佽繍鍔ㄤ細淇濋殰绛夋椿鍔ㄥ皢闄嗙画寮€鏀炬姤鍚嶃€傝鍚屽浠叧娉ㄦ椿鍔ㄦ椂闂达紝鍚堢悊瀹夋帓璇句笟涓庡織鎰挎湇鍔°€?,
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
SELECT '杩愬姩浼氬織鎰夸繚闅滄嫑鍕熸彁閱?,
       '鏍¤繍浼氬織鎰挎湇鍔″矖浣嶅寘鍚褰曞崗鍔┿€佺墿璧勬惉杩愩€佽禌閬撳紩瀵煎拰瑙備紬绉╁簭缁存姢銆傛姤鍚嶆垚鍔熷悗璇蜂繚鎸佺數璇濈晠閫氾紝骞舵寜娲诲姩璇︽儏涓殑闆嗗悎鏃堕棿鍒板満銆?,
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
SELECT '蹇楁効鏃堕暱鏍搁攢璇存槑',
       '娲诲姩缁撴潫鍚庯紝绠＄悊鍛樹細鏍规嵁绛惧埌鎯呭喌纭蹇楁効鏃堕暱銆傝嫢闀挎椂闂存湭鏍搁攢锛岃鍏堢‘璁ゆ槸鍚﹀凡瀹屾垚绛惧埌锛屽啀鑱旂郴娲诲姩璐熻矗浜哄鐞嗐€?,
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
SET title = '鍥涙湀蹇楁効鏈嶅姟鏈堝畨鎺掑彂甯?,
    content = '鍥涙湀蹇楁効鏈嶅姟鏈堝凡寮€鍚紝鍥句功棣嗘暣鐞嗐€佺ぞ鍖烘竻娲併€佽繍鍔ㄤ細淇濋殰绛夋椿鍔ㄥ皢闄嗙画寮€鏀炬姤鍚嶃€傝鍚屽浠叧娉ㄦ椿鍔ㄦ椂闂达紝鍚堢悊瀹夋帓璇句笟涓庡織鎰挎湇鍔°€?,
    image_key = NULL,
    activity_id = 1,
    status = 'PUBLISHED'
WHERE publisher_id = 1 AND sort_order = 30 AND publish_time = '2026-03-25 09:00:00';

UPDATE vol_announcement
SET title = '杩愬姩浼氬織鎰夸繚闅滄嫑鍕熸彁閱?,
    content = '鏍¤繍浼氬織鎰挎湇鍔″矖浣嶅寘鍚褰曞崗鍔┿€佺墿璧勬惉杩愩€佽禌閬撳紩瀵煎拰瑙備紬绉╁簭缁存姢銆傛姤鍚嶆垚鍔熷悗璇蜂繚鎸佺數璇濈晠閫氾紝骞舵寜娲诲姩璇︽儏涓殑闆嗗悎鏃堕棿鍒板満銆?,
    image_key = NULL,
    activity_id = 3,
    status = 'PUBLISHED'
WHERE publisher_id = 1 AND sort_order = 20 AND publish_time = '2026-03-26 10:00:00';

UPDATE vol_announcement
SET title = '蹇楁効鏃堕暱鏍搁攢璇存槑',
    content = '娲诲姩缁撴潫鍚庯紝绠＄悊鍛樹細鏍规嵁绛惧埌鎯呭喌纭蹇楁効鏃堕暱銆傝嫢闀挎椂闂存湭鏍搁攢锛岃鍏堢‘璁ゆ槸鍚﹀凡瀹屾垚绛惧埌锛屽啀鑱旂郴娲诲姩璐熻矗浜哄鐞嗐€?,
    image_key = NULL,
    activity_id = NULL,
    status = 'PUBLISHED'
WHERE publisher_id = 1 AND sort_order = 10 AND publish_time = '2026-03-27 10:00:00';

INSERT IGNORE INTO vol_announcement_activity (announcement_id, activity_id)
SELECT id, activity_id
FROM vol_announcement
WHERE activity_id IS NOT NULL;
