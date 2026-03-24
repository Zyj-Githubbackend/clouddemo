-- =============================================================================
-- 校园志愿服务管理平台 — 全量初始化脚本（唯一入口）
-- 合并内容：原 init.sql + migration_add_registration_start_time（已并入表结构）
-- 使用方式：删除旧库后执行本文件，例如：
--   mysql -u root -p < database/init.sql
-- =============================================================================
-- 关键字段说明（示例数据已对齐，避免「冗余计数」与流水不一致）：
--   vol_activity.current_participants  应与 status=REGISTERED 的报名条数一致
--                                    （应用列表/详情会再次与流水对账，但库内建议一致）
--   registration_start_time            志愿招募开放报名起点（须 < registration_deadline）
--   registration_deadline              报名截止（须 <= start_time，与后端校验一致）
--   vol_registration.status            REGISTERED / CANCELLED
-- =============================================================================

DROP DATABASE IF EXISTS volunteer_platform;
CREATE DATABASE volunteer_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE volunteer_platform;

-- ==================== 用户表 ====================
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名/学号',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    student_no VARCHAR(50) COMMENT '学号',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    role VARCHAR(20) NOT NULL DEFAULT 'VOLUNTEER' COMMENT '角色：VOLUNTEER-志愿者, ADMIN-管理员',
    total_volunteer_hours DECIMAL(10,2) DEFAULT 0.00 COMMENT '累计志愿时长（小时）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

INSERT INTO sys_user (username, password, real_name, student_no, phone, email, role, total_volunteer_hours, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', '2021001', '13800138000', 'admin@university.edu', 'ADMIN', 0.00, 1),
('student01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张三', '2021101', '13800138001', 'zhangsan@university.edu', 'VOLUNTEER', 12.50, 1),
('student02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李四', '2021102', '13800138002', 'lisi@university.edu', 'VOLUNTEER', 8.00, 1);
-- 以上账号密码均为: password123

-- ==================== 志愿活动表 ====================
CREATE TABLE vol_activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动ID',
    title VARCHAR(200) NOT NULL COMMENT '活动标题',
    description TEXT COMMENT '活动详细描述',
    location VARCHAR(200) NOT NULL COMMENT '服务地点',
    max_participants INT NOT NULL COMMENT '招募人数上限',
    current_participants INT DEFAULT 0 COMMENT '当前有效报名人数（与 REGISTERED 流水一致）',
    volunteer_hours DECIMAL(5,2) NOT NULL COMMENT '志愿时长（小时）',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME NOT NULL COMMENT '活动结束时间',
    registration_start_time DATETIME NOT NULL COMMENT '志愿招募开始时间',
    registration_deadline DATETIME NOT NULL COMMENT '志愿招募截止时间',
    status VARCHAR(20) NOT NULL DEFAULT 'RECRUITING' COMMENT '状态：RECRUITING-招募中, ONGOING-进行中, COMPLETED-已结项, CANCELLED-已取消',
    category VARCHAR(50) COMMENT '活动类型',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_start_time (start_time),
    INDEX idx_registration_start (registration_start_time),
    INDEX idx_registration_deadline (registration_deadline),
    INDEX idx_creator_id (creator_id),
    FOREIGN KEY (creator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿活动表';

-- current_participants 与下方 vol_registration 中 REGISTERED 条数一一对应
INSERT INTO vol_activity (
    title, description, location, max_participants, current_participants, volunteer_hours,
    start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id
) VALUES
-- 招募未开始（例如当前时间在 2026-08-01 之前）
('学长火炬 - 新生入学引导', '协助新生办理入学手续、校园导览与答疑。要求：热情、熟悉校园、善于沟通。', '学校东门迎新点', 50, 2, 4.00,
 '2026-09-01 08:00:00', '2026-09-01 18:00:00', '2026-08-01 00:00:00', '2026-08-25 23:59:59', 'RECRUITING', '学长火炬', 1),
-- 招募中（例如当前时间在 2026-01-01～2026-10-10 之间）
('书记驿站 - 图书馆值班', '书记驿站值班：咨询、阅览秩序、图书整理协助。', '图书馆三楼书记驿站', 20, 1, 3.00,
 '2026-10-15 14:00:00', '2026-10-15 20:00:00', '2026-01-01 00:00:00', '2026-10-10 23:59:59', 'RECRUITING', '书记驿站', 1),
-- 招募中（例如当前时间在 2026-03-01～2026-11-15 之间）
('爱心小屋献血车志愿服务', '献血车旁引导、爱心服务与纪念品发放。', '校医院门口献血车', 15, 1, 2.50,
 '2026-11-20 09:00:00', '2026-11-20 17:00:00', '2026-03-01 00:00:00', '2026-11-15 23:59:59', 'RECRUITING', '爱心小屋', 1),
-- 招募未开始（例如当前时间在 2026-06-01 之前）
('校友招商大会引导服务', '会场引导、嘉宾接待、资料发放。着装：正装。', '国际会议中心', 30, 0, 5.00,
 '2026-12-10 08:00:00', '2026-12-10 18:00:00', '2026-06-01 00:00:00', '2026-12-05 23:59:59', 'RECRUITING', '校友招商', 1),
-- 招募已结束（例如当前时间晚于 2026-02-01 截止）
('暖冬行动 - 关爱留守儿童', '乡村小学学业辅导、心理陪伴与冬令物资捐赠。', '阳光乡村小学', 25, 0, 6.00,
 '2026-12-25 09:00:00', '2026-12-25 16:00:00', '2025-06-01 00:00:00', '2026-02-01 23:59:59', 'RECRUITING', '暖冬行动', 1),
-- 已结项（业务状态，前端招募展示仍为「已结束」类）
('春季校园开放日引导（已结项）', '开放日参观路线引导与咨询。活动已结束归档。', '校史馆广场', 40, 0, 3.00,
 '2025-04-20 08:00:00', '2025-04-20 17:00:00', '2025-03-01 00:00:00', '2025-04-15 23:59:59', 'COMPLETED', '学长火炬', 1),
-- 已取消
('暑期支教报名（已取消）', '因合作方原因取消，不再招募。', '线上说明会', 20, 0, 8.00,
 '2026-07-10 09:00:00', '2026-07-10 12:00:00', '2026-05-01 00:00:00', '2026-06-30 23:59:59', 'CANCELLED', '暖冬行动', 1);

-- ==================== 报名流水表 ====================
CREATE TABLE vol_registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报名记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    registration_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    check_in_status TINYINT DEFAULT 0 COMMENT '签到：0-未签到，1-已签到',
    check_in_time DATETIME COMMENT '签到时间',
    hours_confirmed TINYINT DEFAULT 0 COMMENT '时长核销：0-未核销，1-已核销',
    confirm_time DATETIME COMMENT '核销时间',
    status VARCHAR(20) DEFAULT 'REGISTERED' COMMENT 'REGISTERED-已报名, CANCELLED-已取消',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    INDEX idx_user_id (user_id),
    INDEX idx_activity_id (activity_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    FOREIGN KEY (activity_id) REFERENCES vol_activity(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名流水表';

-- 与 activity id=1,2,3 的 current_participants 一致（各 2、1、1）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, check_in_time, hours_confirmed, confirm_time, status) VALUES
(2, 1, '2026-08-05 10:00:00', 1, '2026-09-01 08:15:00', 1, '2026-09-01 19:00:00', 'REGISTERED'),
(3, 1, '2026-08-06 14:30:00', 1, '2026-09-01 08:20:00', 1, '2026-09-01 19:00:00', 'REGISTERED'),
(2, 2, '2026-09-01 11:00:00', 0, NULL, 0, NULL, 'REGISTERED'),
(3, 3, '2026-06-10 16:00:00', 0, NULL, 0, NULL, 'REGISTERED');

-- ==================== 视图：活动统计 ====================
CREATE OR REPLACE VIEW v_activity_statistics AS
SELECT
    a.id,
    a.title,
    a.category,
    a.status,
    a.max_participants,
    a.current_participants,
    ROUND((a.current_participants / NULLIF(a.max_participants, 0)) * 100, 2) AS enrollment_rate,
    COUNT(DISTINCT r.user_id) AS actual_registrations,
    SUM(CASE WHEN r.check_in_status = 1 THEN 1 ELSE 0 END) AS checked_in_count,
    SUM(CASE WHEN r.hours_confirmed = 1 THEN 1 ELSE 0 END) AS hours_confirmed_count
FROM vol_activity a
LEFT JOIN vol_registration r ON a.id = r.activity_id AND r.status = 'REGISTERED'
GROUP BY a.id, a.title, a.category, a.status, a.max_participants, a.current_participants;
