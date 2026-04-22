SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS user_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS activity_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS announcement_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS feedback_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'user_service'@'%' IDENTIFIED BY '123888';
CREATE USER IF NOT EXISTS 'activity_service'@'%' IDENTIFIED BY '123888';
CREATE USER IF NOT EXISTS 'announcement_service'@'%' IDENTIFIED BY '123888';
CREATE USER IF NOT EXISTS 'feedback_service'@'%' IDENTIFIED BY '123888';

ALTER USER 'user_service'@'%' IDENTIFIED BY '123888';
ALTER USER 'activity_service'@'%' IDENTIFIED BY '123888';
ALTER USER 'announcement_service'@'%' IDENTIFIED BY '123888';
ALTER USER 'feedback_service'@'%' IDENTIFIED BY '123888';

GRANT ALL PRIVILEGES ON user_service_db.* TO 'user_service'@'%';
GRANT ALL PRIVILEGES ON activity_service_db.* TO 'activity_service'@'%';
GRANT ALL PRIVILEGES ON announcement_service_db.* TO 'announcement_service'@'%';
GRANT ALL PRIVILEGES ON feedback_service_db.* TO 'feedback_service'@'%';
FLUSH PRIVILEGES;

USE user_service_db;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    student_no VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'VOLUNTEER',
    total_volunteer_hours DECIMAL(10,2) DEFAULT 0.00,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mq_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONSUMED',
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_consumer (message_id, consumer_name),
    KEY idx_consumer_time (consumer_name, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS event_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_next_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_feedback_projection (
    feedback_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    source_message_id VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_feedback_user (user_id),
    KEY idx_user_feedback_status (status),
    KEY idx_user_feedback_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_user
    (username, password, real_name, student_no, phone, email, role, total_volunteer_hours, status)
VALUES
    ('admin', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Admin', '2021000', '13800138000', 'admin@university.edu', 'ADMIN', 0.00, 1),
    ('student01', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Student 01', '2021101', '13800138001', 'student01@university.edu', 'VOLUNTEER', 0.00, 1),
    ('student02', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Student 02', '2021102', '13800138002', 'student02@university.edu', 'VOLUNTEER', 0.00, 1),
    ('student03', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Student 03', '2021103', '13800138003', 'student03@university.edu', 'VOLUNTEER', 0.00, 1),
    ('student04', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Student 04', '2021104', '13800138004', 'student04@university.edu', 'VOLUNTEER', 0.00, 1),
    ('student05', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Student 05', '2021105', '13800138005', 'student05@university.edu', 'VOLUNTEER', 0.00, 1),
    ('student06', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Student 06', '2021106', '13800138006', 'student06@university.edu', 'VOLUNTEER', 0.00, 1),
    ('student07', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Student 07', '2021107', '13800138007', 'student07@university.edu', 'VOLUNTEER', 0.00, 1),
    ('student08', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Student 08', '2021108', '13800138008', 'student08@university.edu', 'VOLUNTEER', 0.00, 1),
    ('student09', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Student 09', '2021109', '13800138009', 'student09@university.edu', 'VOLUNTEER', 0.00, 1),
    ('student10', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', 'Student 10', '2021110', '13800138010', 'student10@university.edu', 'VOLUNTEER', 0.00, 1),
    ('zhangsan', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', '张三', '2021201', '13800138101', 'zhangsan@university.edu', 'VOLUNTEER', 0.00, 1),
    ('lisi', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', '李四', '2021202', '13800138102', 'lisi@university.edu', 'VOLUNTEER', 0.00, 1),
    ('wangwu', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', '王五', '2021203', '13800138103', 'wangwu@university.edu', 'VOLUNTEER', 0.00, 1),
    ('zhaoliu', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', '赵六', '2021204', '13800138104', 'zhaoliu@university.edu', 'VOLUNTEER', 0.00, 1),
    ('sunqi', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', '孙七', '2021205', '13800138105', 'sunqi@university.edu', 'VOLUNTEER', 0.00, 1),
    ('zhouba', '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', '周八', '2021206', '13800138106', 'zhouba@university.edu', 'VOLUNTEER', 0.00, 1)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    real_name = VALUES(real_name),
    student_no = VALUES(student_no),
    phone = VALUES(phone),
    email = VALUES(email),
    role = VALUES(role),
    status = VALUES(status);

USE activity_service_db;

CREATE TABLE IF NOT EXISTS vol_activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    image_key TEXT,
    location VARCHAR(200),
    max_participants INT NOT NULL DEFAULT 50,
    current_participants INT NOT NULL DEFAULT 0,
    volunteer_hours DECIMAL(5,1) NOT NULL DEFAULT 2.0,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    registration_start_time DATETIME,
    registration_deadline DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RECRUITING',
    category VARCHAR(50),
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_registration_deadline (registration_deadline),
    INDEX idx_end_time (end_time),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vol_registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    registration_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    check_in_status TINYINT DEFAULT 0,
    check_in_time DATETIME,
    hours_confirmed TINYINT DEFAULT 0,
    confirm_time DATETIME,
    status VARCHAR(20) DEFAULT 'REGISTERED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    INDEX idx_user_id (user_id),
    INDEX idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS event_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_next_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mq_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONSUMED',
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_consumer (message_id, consumer_name),
    KEY idx_consumer_time (consumer_name, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    'Campus Library Volunteer Day',
    'Support campus library visitor guidance and reading promotion activities.',
    'Campus Library Hall',
    30,
    0,
    2.0,
    '2026-05-20 09:00:00',
    '2026-05-20 17:00:00',
    '2026-04-20 00:00:00',
    '2026-05-18 23:59:59',
    'RECRUITING',
    'Campus Service',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = 'Campus Library Volunteer Day'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    'Campus Welcome Service Day',
    'Guide freshmen through registration and campus orientation.',
    'East Gate Service Desk',
    60,
    0,
    4.0,
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 7 DAY), INTERVAL 9 HOUR),
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    DATE_ADD(NOW(), INTERVAL 5 DAY),
    'RECRUITING',
    'Campus Service',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = 'Campus Welcome Service Day'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    'Community Senior Visit',
    'Visit seniors in nearby communities and provide companionship services.',
    'Sunshine Community Center',
    20,
    0,
    3.0,
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    DATE_ADD(NOW(), INTERVAL 1 DAY),
    DATE_SUB(NOW(), INTERVAL 15 DAY),
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    'RECRUITING',
    'Community Care',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = 'Community Senior Visit'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    'Campus Marathon Support',
    'Assist with route guidance and aid stations for campus marathon.',
    'Sports Field',
    40,
    0,
    5.0,
    DATE_SUB(NOW(), INTERVAL 20 DAY),
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 DAY), INTERVAL 8 HOUR),
    DATE_SUB(NOW(), INTERVAL 35 DAY),
    DATE_SUB(NOW(), INTERVAL 22 DAY),
    'COMPLETED',
    'Large Event',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = 'Campus Marathon Support'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    'Tree Planting Initiative',
    'Environmental protection activity around the suburban green area.',
    'Suburban Green Base',
    30,
    0,
    4.5,
    DATE_ADD(NOW(), INTERVAL 14 DAY),
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 14 DAY), INTERVAL 8 HOUR),
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    DATE_ADD(NOW(), INTERVAL 10 DAY),
    'CANCELLED',
    'Environment',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = 'Tree Planting Initiative'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    'Digital Skills Workshop',
    'Help seniors learn mobile apps and anti-fraud basics.',
    'Innovation Hub',
    25,
    0,
    2.5,
    DATE_ADD(NOW(), INTERVAL 9 DAY),
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 9 DAY), INTERVAL 5 HOUR),
    DATE_SUB(NOW(), INTERVAL 4 DAY),
    DATE_ADD(NOW(), INTERVAL 6 DAY),
    'RECRUITING',
    'Community Care',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = 'Digital Skills Workshop'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    'Charity Book Fair',
    'Run a charity second-hand book fair and donate proceeds.',
    'Library Plaza',
    35,
    0,
    3.5,
    DATE_SUB(NOW(), INTERVAL 30 DAY),
    DATE_SUB(NOW(), INTERVAL 29 DAY),
    DATE_SUB(NOW(), INTERVAL 45 DAY),
    DATE_SUB(NOW(), INTERVAL 31 DAY),
    'COMPLETED',
    'Public Welfare',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = 'Charity Book Fair'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    '图书馆周末整理服务',
    '协助主图书馆完成图书归架、读者咨询和阅览区秩序维护，适合首次参与志愿活动的同学。',
    '主图书馆一层服务台',
    24,
    0,
    3.0,
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 5 DAY), INTERVAL 9 HOUR),
    DATE_ADD(DATE_ADD(DATE_ADD(NOW(), INTERVAL 5 DAY), INTERVAL 9 HOUR), INTERVAL 4 HOUR),
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    DATE_ADD(NOW(), INTERVAL 4 DAY),
    'RECRUITING',
    '校园服务',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = '图书馆周末整理服务'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    '留守儿童课后陪伴计划',
    '在社区青少年活动站协助完成课后作业辅导、阅读陪伴和安全签到，适合耐心细致的同学参与。',
    '城南社区青少年活动站',
    18,
    0,
    2.5,
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 11 DAY), INTERVAL 14 HOUR),
    DATE_ADD(DATE_ADD(DATE_ADD(NOW(), INTERVAL 11 DAY), INTERVAL 14 HOUR), INTERVAL 3 HOUR),
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    DATE_ADD(NOW(), INTERVAL 8 DAY),
    'RECRUITING',
    '公益助学',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = '留守儿童课后陪伴计划'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    '夕阳红长者智能手机课堂',
    '帮助社区长者学习手机支付、出行码和反诈识别，现场分组讲解并协助一对一答疑。',
    '福宁街道综合为老服务中心',
    20,
    0,
    3.5,
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 8 DAY), INTERVAL 9 HOUR),
    DATE_ADD(DATE_ADD(DATE_ADD(NOW(), INTERVAL 8 DAY), INTERVAL 9 HOUR), INTERVAL 3 HOUR),
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    'RECRUITING',
    '社区关爱',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = '夕阳红长者智能手机课堂'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    '春季运动会秩序保障',
    '负责检录引导、观众看台秩序维护和饮水点补给，适合有大型活动协作经验的志愿者。',
    '东操场与看台区域',
    45,
    0,
    5.0,
    DATE_ADD(DATE_SUB(NOW(), INTERVAL 12 DAY), INTERVAL 7 HOUR),
    DATE_ADD(DATE_ADD(DATE_SUB(NOW(), INTERVAL 12 DAY), INTERVAL 7 HOUR), INTERVAL 10 HOUR),
    DATE_SUB(NOW(), INTERVAL 20 DAY),
    DATE_SUB(NOW(), INTERVAL 13 DAY),
    'COMPLETED',
    '大型活动',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = '春季运动会秩序保障'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    '湿地公园垃圾分类宣传',
    '在公园入口和游客中心开展垃圾分类宣传、互动问答和宣传页发放，倡导绿色出行与环保意识。',
    '南湖湿地公园游客中心',
    30,
    0,
    4.0,
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 16 DAY), INTERVAL 8 HOUR),
    DATE_ADD(DATE_ADD(DATE_ADD(NOW(), INTERVAL 16 DAY), INTERVAL 8 HOUR), INTERVAL 6 HOUR),
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    DATE_ADD(NOW(), INTERVAL 12 DAY),
    'RECRUITING',
    '环保公益',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = '湿地公园垃圾分类宣传'
);

INSERT INTO vol_activity
    (title, description, location, max_participants, current_participants, volunteer_hours,
     start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id)
SELECT
    '防汛应急演练志愿协助',
    '协助物资清点、引导疏散路线和现场秩序维护，用于演练校园和周边社区的防汛应急响应流程。',
    '北门防汛物资仓库',
    28,
    0,
    4.5,
    DATE_ADD(DATE_ADD(NOW(), INTERVAL 6 DAY), INTERVAL 8 HOUR),
    DATE_ADD(DATE_ADD(DATE_ADD(NOW(), INTERVAL 6 DAY), INTERVAL 8 HOUR), INTERVAL 5 HOUR),
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    DATE_ADD(NOW(), INTERVAL 4 DAY),
    'CANCELLED',
    '应急救援',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_activity WHERE title = '防汛应急演练志愿协助'
);

SET @u1 = (SELECT id FROM user_service_db.sys_user WHERE username = 'student01' LIMIT 1);
SET @u2 = (SELECT id FROM user_service_db.sys_user WHERE username = 'student02' LIMIT 1);
SET @u3 = (SELECT id FROM user_service_db.sys_user WHERE username = 'student03' LIMIT 1);
SET @u4 = (SELECT id FROM user_service_db.sys_user WHERE username = 'student04' LIMIT 1);
SET @u5 = (SELECT id FROM user_service_db.sys_user WHERE username = 'student05' LIMIT 1);
SET @u6 = (SELECT id FROM user_service_db.sys_user WHERE username = 'student06' LIMIT 1);
SET @u7 = (SELECT id FROM user_service_db.sys_user WHERE username = 'student07' LIMIT 1);
SET @u8 = (SELECT id FROM user_service_db.sys_user WHERE username = 'student08' LIMIT 1);
SET @u9 = (SELECT id FROM user_service_db.sys_user WHERE username = 'student09' LIMIT 1);
SET @u10 = (SELECT id FROM user_service_db.sys_user WHERE username = 'student10' LIMIT 1);
SET @u11 = (SELECT id FROM user_service_db.sys_user WHERE username = 'zhangsan' LIMIT 1);
SET @u12 = (SELECT id FROM user_service_db.sys_user WHERE username = 'lisi' LIMIT 1);
SET @u13 = (SELECT id FROM user_service_db.sys_user WHERE username = 'wangwu' LIMIT 1);
SET @u14 = (SELECT id FROM user_service_db.sys_user WHERE username = 'zhaoliu' LIMIT 1);
SET @u15 = (SELECT id FROM user_service_db.sys_user WHERE username = 'sunqi' LIMIT 1);
SET @u16 = (SELECT id FROM user_service_db.sys_user WHERE username = 'zhouba' LIMIT 1);

SET @act_library = (SELECT id FROM vol_activity WHERE title = 'Campus Library Volunteer Day' LIMIT 1);
SET @act_welcome = (SELECT id FROM vol_activity WHERE title = 'Campus Welcome Service Day' LIMIT 1);
SET @act_senior = (SELECT id FROM vol_activity WHERE title = 'Community Senior Visit' LIMIT 1);
SET @act_marathon = (SELECT id FROM vol_activity WHERE title = 'Campus Marathon Support' LIMIT 1);
SET @act_digital = (SELECT id FROM vol_activity WHERE title = 'Digital Skills Workshop' LIMIT 1);
SET @act_charity = (SELECT id FROM vol_activity WHERE title = 'Charity Book Fair' LIMIT 1);
SET @act_library_cn = (SELECT id FROM vol_activity WHERE title = '图书馆周末整理服务' LIMIT 1);
SET @act_children_cn = (SELECT id FROM vol_activity WHERE title = '留守儿童课后陪伴计划' LIMIT 1);
SET @act_senior_phone_cn = (SELECT id FROM vol_activity WHERE title = '夕阳红长者智能手机课堂' LIMIT 1);
SET @act_sports_cn = (SELECT id FROM vol_activity WHERE title = '春季运动会秩序保障' LIMIT 1);
SET @act_environment_cn = (SELECT id FROM vol_activity WHERE title = '湿地公园垃圾分类宣传' LIMIT 1);
SET @act_emergency_cn = (SELECT id FROM vol_activity WHERE title = '防汛应急演练志愿协助' LIMIT 1);

INSERT INTO vol_registration
    (user_id, activity_id, registration_time, check_in_status, check_in_time, hours_confirmed, confirm_time, status)
VALUES
    (@u1, @act_library, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u2, @act_library, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u1, @act_welcome, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u3, @act_welcome, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u4, @act_welcome, NOW(), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u2, @act_senior, DATE_SUB(NOW(), INTERVAL 10 DAY), 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, NULL, 'REGISTERED'),
    (@u5, @act_senior, DATE_SUB(NOW(), INTERVAL 9 DAY), 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, NULL, 'REGISTERED'),
    (@u6, @act_senior, DATE_SUB(NOW(), INTERVAL 8 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u3, @act_marathon, DATE_SUB(NOW(), INTERVAL 25 DAY), 1, DATE_SUB(NOW(), INTERVAL 20 DAY), 1, DATE_SUB(NOW(), INTERVAL 19 DAY), 'REGISTERED'),
    (@u4, @act_marathon, DATE_SUB(NOW(), INTERVAL 24 DAY), 1, DATE_SUB(NOW(), INTERVAL 20 DAY), 1, DATE_SUB(NOW(), INTERVAL 19 DAY), 'REGISTERED'),
    (@u7, @act_digital, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u8, @act_digital, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u1, @act_charity, DATE_SUB(NOW(), INTERVAL 40 DAY), 1, DATE_SUB(NOW(), INTERVAL 30 DAY), 1, DATE_SUB(NOW(), INTERVAL 29 DAY), 'REGISTERED'),
    (@u9, @act_library_cn, DATE_SUB(NOW(), INTERVAL 3 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u11, @act_library_cn, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u12, @act_library_cn, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u13, @act_children_cn, DATE_SUB(NOW(), INTERVAL 4 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u14, @act_children_cn, DATE_SUB(NOW(), INTERVAL 4 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u15, @act_senior_phone_cn, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u16, @act_senior_phone_cn, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u10, @act_sports_cn, DATE_SUB(NOW(), INTERVAL 15 DAY), 1, DATE_SUB(NOW(), INTERVAL 12 DAY), 1, DATE_SUB(NOW(), INTERVAL 11 DAY), 'REGISTERED'),
    (@u11, @act_sports_cn, DATE_SUB(NOW(), INTERVAL 14 DAY), 1, DATE_SUB(NOW(), INTERVAL 12 DAY), 1, DATE_SUB(NOW(), INTERVAL 11 DAY), 'REGISTERED'),
    (@u12, @act_environment_cn, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, NULL, 0, NULL, 'REGISTERED'),
    (@u13, @act_environment_cn, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, NULL, 0, NULL, 'REGISTERED')
ON DUPLICATE KEY UPDATE
    registration_time = VALUES(registration_time),
    check_in_status = VALUES(check_in_status),
    check_in_time = VALUES(check_in_time),
    hours_confirmed = VALUES(hours_confirmed),
    confirm_time = VALUES(confirm_time),
    status = VALUES(status);

UPDATE vol_activity a
LEFT JOIN (
    SELECT activity_id, COUNT(*) AS participant_count
    FROM vol_registration
    WHERE status = 'REGISTERED'
    GROUP BY activity_id
) r ON r.activity_id = a.id
SET a.current_participants = COALESCE(r.participant_count, 0);

UPDATE user_service_db.sys_user u
LEFT JOIN (
    SELECT r.user_id, SUM(a.volunteer_hours) AS total_hours
    FROM activity_service_db.vol_registration r
    JOIN activity_service_db.vol_activity a ON a.id = r.activity_id
    WHERE r.status = 'REGISTERED'
      AND r.check_in_status = 1
      AND r.hours_confirmed = 1
    GROUP BY r.user_id
) t ON t.user_id = u.id
SET u.total_volunteer_hours = CASE
    WHEN u.role = 'VOLUNTEER' THEN COALESCE(t.total_hours, 0)
    ELSE u.total_volunteer_hours
END;

DROP VIEW IF EXISTS v_activity_statistics;
CREATE VIEW v_activity_statistics AS
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
FROM vol_activity a
LEFT JOIN vol_registration r
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

USE announcement_service_db;

CREATE TABLE IF NOT EXISTS vol_announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    image_key TEXT,
    activity_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    sort_order INT NOT NULL DEFAULT 0,
    publisher_id BIGINT,
    publish_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_sort (status, sort_order, publish_time),
    INDEX idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vol_announcement_activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    announcement_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_announcement_activity (announcement_id, activity_id),
    INDEX idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vol_announcement_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    announcement_id BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    file_size BIGINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_announcement_id (announcement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vol_announcement_activity_projection (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    location VARCHAR(200),
    start_time DATETIME,
    end_time DATETIME,
    status VARCHAR(20),
    category VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_projection_status (status),
    INDEX idx_projection_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mq_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONSUMED',
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_consumer (message_id, consumer_name),
    KEY idx_consumer_time (consumer_name, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS event_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_next_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO vol_announcement
    (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT
    'Platform Started Successfully',
    'The volunteer platform has been initialized by one-click deployment.',
    NULL,
    NULL,
    'PUBLISHED',
    100,
    1,
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement WHERE title = 'Platform Started Successfully'
);

INSERT INTO vol_announcement
    (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT
    'April Volunteer Activity Plan',
    'The April schedule is online. Please review deadlines and reserve your slots early.',
    NULL,
    (SELECT id FROM activity_service_db.vol_activity WHERE title = 'Campus Welcome Service Day' LIMIT 1),
    'PUBLISHED',
    90,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement WHERE title = 'April Volunteer Activity Plan'
);

INSERT INTO vol_announcement
    (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT
    'Community Visit Reminder',
    'Please sign in on time for Community Senior Visit and wear volunteer badges.',
    NULL,
    (SELECT id FROM activity_service_db.vol_activity WHERE title = 'Community Senior Visit' LIMIT 1),
    'PUBLISHED',
    80,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement WHERE title = 'Community Visit Reminder'
);

INSERT INTO vol_announcement
    (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT
    'Volunteer Hours Verification Guide',
    'After event completion, hours are confirmed after attendance checks.',
    NULL,
    NULL,
    'PUBLISHED',
    70,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 3 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement WHERE title = 'Volunteer Hours Verification Guide'
);

INSERT INTO vol_announcement
    (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT
    '五一志愿服务报名提醒',
    '近期将集中开展图书馆整理、社区陪伴等多场中文测试活动，请已完成实名认证的同学尽早报名并关注活动签到要求。',
    NULL,
    (SELECT id FROM activity_service_db.vol_activity WHERE title = '图书馆周末整理服务' LIMIT 1),
    'PUBLISHED',
    95,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 6 HOUR)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement WHERE title = '五一志愿服务报名提醒'
);

INSERT INTO vol_announcement
    (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT
    '社区助学陪伴活动招募中',
    '留守儿童课后陪伴计划现已开放报名，请志愿者提前完成培训确认，并按时到达社区活动站集合。',
    NULL,
    (SELECT id FROM activity_service_db.vol_activity WHERE title = '留守儿童课后陪伴计划' LIMIT 1),
    'PUBLISHED',
    88,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 10 HOUR)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement WHERE title = '社区助学陪伴活动招募中'
);

INSERT INTO vol_announcement
    (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT
    '长者课堂服务注意事项',
    '请参加长者智能手机课堂的志愿者提前 20 分钟到场，统一佩戴工牌并准备好演示机与讲义。',
    NULL,
    (SELECT id FROM activity_service_db.vol_activity WHERE title = '夕阳红长者智能手机课堂' LIMIT 1),
    'PUBLISHED',
    78,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement WHERE title = '长者课堂服务注意事项'
);

INSERT INTO vol_announcement
    (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT
    '春季运动会志愿者表扬',
    '春季运动会秩序保障活动顺利完成，感谢所有志愿者在检录引导、看台巡视和补给服务中的认真投入。',
    NULL,
    (SELECT id FROM activity_service_db.vol_activity WHERE title = '春季运动会秩序保障' LIMIT 1),
    'PUBLISHED',
    68,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement WHERE title = '春季运动会志愿者表扬'
);

INSERT INTO vol_announcement
    (title, content, image_key, activity_id, status, sort_order, publisher_id, publish_time)
SELECT
    '防汛演练时间调整通知',
    '受天气影响，原定的防汛应急演练已调整安排，相关志愿者请等待新的集合通知，勿直接前往演练地点。',
    NULL,
    (SELECT id FROM activity_service_db.vol_activity WHERE title = '防汛应急演练志愿协助' LIMIT 1),
    'OFFLINE',
    60,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 3 HOUR)
WHERE NOT EXISTS (
    SELECT 1 FROM vol_announcement WHERE title = '防汛演练时间调整通知'
);

DELETE aa
FROM vol_announcement_activity aa
JOIN vol_announcement a ON a.id = aa.announcement_id
WHERE a.title IN (
    'April Volunteer Activity Plan',
    'Community Visit Reminder',
    'Volunteer Hours Verification Guide',
    '五一志愿服务报名提醒',
    '社区助学陪伴活动招募中',
    '长者课堂服务注意事项',
    '春季运动会志愿者表扬',
    '防汛演练时间调整通知'
)
AND (a.activity_id IS NULL OR aa.activity_id <> a.activity_id);

INSERT IGNORE INTO vol_announcement_activity (announcement_id, activity_id)
SELECT id, activity_id
FROM vol_announcement
WHERE title IN (
    'April Volunteer Activity Plan',
    'Community Visit Reminder',
    'Volunteer Hours Verification Guide',
    '五一志愿服务报名提醒',
    '社区助学陪伴活动招募中',
    '长者课堂服务注意事项',
    '春季运动会志愿者表扬',
    '防汛演练时间调整通知'
)
AND activity_id IS NOT NULL;

INSERT INTO vol_announcement_activity_projection
    (id, title, location, start_time, end_time, status, category)
SELECT id, title, location, start_time, end_time, status, category
FROM activity_service_db.vol_activity
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    location = VALUES(location),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    status = VALUES(status),
    category = VALUES(category);

USE feedback_service_db;

CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    last_message_time DATETIME,
    last_replier_role VARCHAR(20),
    closed_by BIGINT,
    closed_time DATETIME,
    reject_reason VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feedback_user_status (user_id, status),
    INDEX idx_feedback_status_category (status, category),
    INDEX idx_feedback_priority (priority),
    INDEX idx_feedback_last_message_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS feedback_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    feedback_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    content TEXT,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_feedback_message_feedback (feedback_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS feedback_message_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    feedback_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    file_size BIGINT NOT NULL DEFAULT 0,
    file_type VARCHAR(20) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_feedback_attachment_feedback (feedback_id),
    INDEX idx_feedback_attachment_message (message_id),
    UNIQUE KEY uk_feedback_attachment_object_key (object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS event_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_next_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mq_consume_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONSUMED',
    consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_consumer (message_id, consumer_name),
    KEY idx_consumer_time (consumer_name, consumed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO feedback
    (user_id, title, category, status, priority, last_message_time, last_replier_role, closed_by, closed_time, reject_reason)
SELECT
    (SELECT id FROM user_service_db.sys_user WHERE username = 'student01' LIMIT 1),
    'Login page layout issue',
    'UI',
    'OPEN',
    'NORMAL',
    DATE_SUB(NOW(), INTERVAL 30 MINUTE),
    'VOLUNTEER',
    NULL,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM feedback WHERE title = 'Login page layout issue'
);

INSERT INTO feedback
    (user_id, title, category, status, priority, last_message_time, last_replier_role, closed_by, closed_time, reject_reason)
SELECT
    (SELECT id FROM user_service_db.sys_user WHERE username = 'student02' LIMIT 1),
    'Need export format clarification',
    'FUNCTION',
    'PROCESSING',
    'HIGH',
    DATE_SUB(NOW(), INTERVAL 2 HOUR),
    'ADMIN',
    NULL,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM feedback WHERE title = 'Need export format clarification'
);

INSERT INTO feedback
    (user_id, title, category, status, priority, last_message_time, last_replier_role, closed_by, closed_time, reject_reason)
SELECT
    (SELECT id FROM user_service_db.sys_user WHERE username = 'student03' LIMIT 1),
    'Suggestion: add dark mode',
    'SUGGESTION',
    'CLOSED',
    'LOW',
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    'ADMIN',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM feedback WHERE title = 'Suggestion: add dark mode'
);

UPDATE feedback
SET category = 'BUG'
WHERE title = 'Login page layout issue'
  AND category = 'UI';

UPDATE feedback
SET category = 'QUESTION'
WHERE title = 'Need export format clarification'
  AND category = 'FUNCTION';

UPDATE feedback
SET status = 'REPLIED'
WHERE title = 'Need export format clarification'
  AND status = 'PROCESSING';

INSERT INTO feedback
    (user_id, title, category, status, priority, last_message_time, last_replier_role, closed_by, closed_time, reject_reason)
SELECT
    (SELECT id FROM user_service_db.sys_user WHERE username = 'zhangsan' LIMIT 1),
    '报名成功后活动列表未立即刷新',
    'BUG',
    'OPEN',
    'HIGH',
    DATE_SUB(NOW(), INTERVAL 45 MINUTE),
    'VOLUNTEER',
    NULL,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM feedback WHERE title = '报名成功后活动列表未立即刷新'
);

INSERT INTO feedback
    (user_id, title, category, status, priority, last_message_time, last_replier_role, closed_by, closed_time, reject_reason)
SELECT
    (SELECT id FROM user_service_db.sys_user WHERE username = 'lisi' LIMIT 1),
    '希望增加按学院筛选活动功能',
    'SUGGESTION',
    'REPLIED',
    'NORMAL',
    DATE_SUB(NOW(), INTERVAL 5 HOUR),
    'ADMIN',
    NULL,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM feedback WHERE title = '希望增加按学院筛选活动功能'
);

INSERT INTO feedback
    (user_id, title, category, status, priority, last_message_time, last_replier_role, closed_by, closed_time, reject_reason)
SELECT
    (SELECT id FROM user_service_db.sys_user WHERE username = 'wangwu' LIMIT 1),
    '志愿时长证明导出排版建议',
    'QUESTION',
    'CLOSED',
    'LOW',
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    'ADMIN',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM feedback WHERE title = '志愿时长证明导出排版建议'
);

INSERT INTO feedback
    (user_id, title, category, status, priority, last_message_time, last_replier_role, closed_by, closed_time, reject_reason)
SELECT
    (SELECT id FROM user_service_db.sys_user WHERE username = 'zhaoliu' LIMIT 1),
    '重复收到活动提醒短信',
    'COMPLAINT',
    'REJECTED',
    'URGENT',
    DATE_SUB(NOW(), INTERVAL 8 HOUR),
    'SYSTEM',
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    DATE_SUB(NOW(), INTERVAL 8 HOUR),
    '经核查为运营商补发导致的重复提醒，非系统重复发送。'
WHERE NOT EXISTS (
    SELECT 1 FROM feedback WHERE title = '重复收到活动提醒短信'
);

INSERT INTO feedback
    (user_id, title, category, status, priority, last_message_time, last_replier_role, closed_by, closed_time, reject_reason)
SELECT
    (SELECT id FROM user_service_db.sys_user WHERE username = 'sunqi' LIMIT 1),
    '公告列表希望支持附件预览',
    'SUGGESTION',
    'OPEN',
    'NORMAL',
    DATE_SUB(NOW(), INTERVAL 90 MINUTE),
    'VOLUNTEER',
    NULL,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM feedback WHERE title = '公告列表希望支持附件预览'
);

INSERT INTO feedback
    (user_id, title, category, status, priority, last_message_time, last_replier_role, closed_by, closed_time, reject_reason)
SELECT
    (SELECT id FROM user_service_db.sys_user WHERE username = 'zhouba' LIMIT 1),
    '上传图片后封面顺序和选择顺序不一致',
    'BUG',
    'REPLIED',
    'HIGH',
    DATE_SUB(NOW(), INTERVAL 6 HOUR),
    'ADMIN',
    NULL,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM feedback WHERE title = '上传图片后封面顺序和选择顺序不一致'
);

SET @fb1 = (SELECT id FROM feedback WHERE title = 'Login page layout issue' LIMIT 1);
SET @fb2 = (SELECT id FROM feedback WHERE title = 'Need export format clarification' LIMIT 1);
SET @fb3 = (SELECT id FROM feedback WHERE title = 'Suggestion: add dark mode' LIMIT 1);
SET @fb4 = (SELECT id FROM feedback WHERE title = '报名成功后活动列表未立即刷新' LIMIT 1);
SET @fb5 = (SELECT id FROM feedback WHERE title = '希望增加按学院筛选活动功能' LIMIT 1);
SET @fb6 = (SELECT id FROM feedback WHERE title = '志愿时长证明导出排版建议' LIMIT 1);
SET @fb7 = (SELECT id FROM feedback WHERE title = '重复收到活动提醒短信' LIMIT 1);
SET @fb8 = (SELECT id FROM feedback WHERE title = '公告列表希望支持附件预览' LIMIT 1);
SET @fb9 = (SELECT id FROM feedback WHERE title = '上传图片后封面顺序和选择顺序不一致' LIMIT 1);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb1,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'student01' LIMIT 1),
    'VOLUNTEER',
    'The login button overlaps on smaller screens.',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 40 MINUTE)
WHERE @fb1 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb1 AND content = 'The login button overlaps on smaller screens.'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb2,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'student02' LIMIT 1),
    'VOLUNTEER',
    'Can exported volunteer hours include activity category?',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 3 HOUR)
WHERE @fb2 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb2 AND content = 'Can exported volunteer hours include activity category?'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb2,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    'ADMIN',
    'Accepted. We will include it in the next patch.',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 2 HOUR)
WHERE @fb2 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb2 AND content = 'Accepted. We will include it in the next patch.'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb3,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'student03' LIMIT 1),
    'VOLUNTEER',
    'Dark mode would be helpful for night usage.',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE @fb3 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb3 AND content = 'Dark mode would be helpful for night usage.'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb3,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    'ADMIN',
    'Thanks for the suggestion. This ticket is now closed.',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE @fb3 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb3 AND content = 'Thanks for the suggestion. This ticket is now closed.'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb4,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'zhangsan' LIMIT 1),
    'VOLUNTEER',
    '我在活动详情页点击报名后返回列表，名额数字没有立刻变化，刷新页面后才正常显示。',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 50 MINUTE)
WHERE @fb4 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb4 AND content = '我在活动详情页点击报名后返回列表，名额数字没有立刻变化，刷新页面后才正常显示。'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb5,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'lisi' LIMIT 1),
    'VOLUNTEER',
    '活动越来越多了，希望列表页能按学院或组织单位进一步筛选，找活动会更方便。',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 6 HOUR)
WHERE @fb5 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb5 AND content = '活动越来越多了，希望列表页能按学院或组织单位进一步筛选，找活动会更方便。'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb5,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    'ADMIN',
    '建议已记录，后续会与活动分类和关键字搜索一起排期优化。',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 5 HOUR)
WHERE @fb5 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb5 AND content = '建议已记录，后续会与活动分类和关键字搜索一起排期优化。'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb6,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'wangwu' LIMIT 1),
    'VOLUNTEER',
    '导出的志愿时长证明内容已经够全了，如果表头和页边距再紧凑一点，打印出来会更美观。',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 3 DAY)
WHERE @fb6 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb6 AND content = '导出的志愿时长证明内容已经够全了，如果表头和页边距再紧凑一点，打印出来会更美观。'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb6,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    'ADMIN',
    '已根据建议调整导出模板样式，这条工单先关闭，后续如果还有细节问题欢迎继续反馈。',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE @fb6 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb6 AND content = '已根据建议调整导出模板样式，这条工单先关闭，后续如果还有细节问题欢迎继续反馈。'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb7,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'zhaoliu' LIMIT 1),
    'VOLUNTEER',
    '同一场活动我在十分钟内收到了两条内容完全相同的提醒短信，担心后续会重复通知。',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 9 HOUR)
WHERE @fb7 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb7 AND content = '同一场活动我在十分钟内收到了两条内容完全相同的提醒短信，担心后续会重复通知。'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb7,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    'SYSTEM',
    '经核查为运营商补发导致的重复提醒，平台侧未发生重复发送，本次工单按已说明处理并驳回。',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 8 HOUR)
WHERE @fb7 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb7 AND content = '经核查为运营商补发导致的重复提醒，平台侧未发生重复发送，本次工单按已说明处理并驳回。'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb8,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'sunqi' LIMIT 1),
    'VOLUNTEER',
    '现在公告里的附件只能下载，若能直接在页面预览 PDF 或图片，会更方便快速查看通知内容。',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 100 MINUTE)
WHERE @fb8 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb8 AND content = '现在公告里的附件只能下载，若能直接在页面预览 PDF 或图片，会更方便快速查看通知内容。'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb9,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'zhouba' LIMIT 1),
    'VOLUNTEER',
    '我连续上传三张活动海报后，详情页里封面顺序和选择时的顺序不一致，容易选错主图。',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 7 HOUR)
WHERE @fb9 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb9 AND content = '我连续上传三张活动海报后，详情页里封面顺序和选择时的顺序不一致，容易选错主图。'
);

INSERT INTO feedback_message (feedback_id, sender_id, sender_role, content, message_type, create_time)
SELECT
    @fb9,
    (SELECT id FROM user_service_db.sys_user WHERE username = 'admin' LIMIT 1),
    'ADMIN',
    '问题已确认，前端上传组件的排序展示会在后续版本中统一按选择顺序渲染。',
    'TEXT',
    DATE_SUB(NOW(), INTERVAL 6 HOUR)
WHERE @fb9 IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM feedback_message
    WHERE feedback_id = @fb9 AND content = '问题已确认，前端上传组件的排序展示会在后续版本中统一按选择顺序渲染。'
);

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
FROM feedback_service_db.feedback f
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    title = VALUES(title),
    category = VALUES(category),
    status = VALUES(status),
    updated_at = VALUES(updated_at);
