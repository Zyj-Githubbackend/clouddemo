-- =============================================================================
-- 校园志愿服务管理平台 — 全量初始化脚本
-- 使用方式：mysql -u root -p < database/init.sql
-- 基准日期：2026-03-25（今日），测试数据覆盖所有业务场景
-- =============================================================================
-- 字段约束说明：
--   registration_start_time < registration_deadline <= start_time < end_time
--   vol_activity.current_participants  =  该活动 status=REGISTERED 的报名条数
--   sys_user.total_volunteer_hours     =  该用户所有已核销记录的 volunteer_hours 之和
--   vol_registration.status            REGISTERED / CANCELLED
-- =============================================================================

DROP DATABASE IF EXISTS volunteer_platform;
CREATE DATABASE volunteer_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE volunteer_platform;

-- ==============================================================================
-- 1. 用户表
-- ==============================================================================
CREATE TABLE sys_user (
    id                   BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username             VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password             VARCHAR(255) NOT NULL COMMENT '密码（BCrypt）',
    real_name            VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    student_no           VARCHAR(50)  COMMENT '学号',
    phone                VARCHAR(20)  COMMENT '手机号',
    email                VARCHAR(100) COMMENT '邮箱',
    role                 VARCHAR(20)  NOT NULL DEFAULT 'VOLUNTEER' COMMENT 'VOLUNTEER / ADMIN',
    total_volunteer_hours DECIMAL(10,2) DEFAULT 0.00 COMMENT '累计志愿时长（小时）',
    status               TINYINT      DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username  (username),
    INDEX idx_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ==============================================================================
-- 2. 志愿活动表
-- ==============================================================================
CREATE TABLE vol_activity (
    id                      BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '活动ID',
    title                   VARCHAR(200)  NOT NULL COMMENT '活动标题',
    description             TEXT          COMMENT '活动详情',
    location                VARCHAR(200)  COMMENT '服务地点',
    max_participants        INT           NOT NULL DEFAULT 50 COMMENT '最大招募人数',
    current_participants    INT           NOT NULL DEFAULT 0  COMMENT '当前报名人数',
    volunteer_hours         DECIMAL(5,1)  NOT NULL DEFAULT 2.0 COMMENT '志愿时长（小时）',
    start_time              DATETIME      NOT NULL COMMENT '活动开始时间',
    end_time                DATETIME      NOT NULL COMMENT '活动结束时间',
    registration_start_time DATETIME      COMMENT '招募开始时间',
    registration_deadline   DATETIME      NOT NULL COMMENT '报名截止时间',
    status                  VARCHAR(20)   NOT NULL DEFAULT 'RECRUITING'
        COMMENT '状态：RECRUITING-招募中, COMPLETED-已结项, CANCELLED-已取消（活动阶段由时间动态判断，不存储 ONGOING）',
    category                VARCHAR(50)   COMMENT '活动类型：校园服务、公益助学、社区关怀、大型活动、环保公益、应急救援',
    creator_id              BIGINT        COMMENT '发布者ID',
    create_time             DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status             (status),
    INDEX idx_registration_deadline (registration_deadline),
    INDEX idx_end_time           (end_time),
    INDEX idx_category           (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿活动表';

-- ==============================================================================
-- 3. 报名流水表
-- ==============================================================================
CREATE TABLE vol_registration (
    id                BIGINT    PRIMARY KEY AUTO_INCREMENT COMMENT '报名ID',
    user_id           BIGINT    NOT NULL COMMENT '用户ID',
    activity_id       BIGINT    NOT NULL COMMENT '活动ID',
    registration_time DATETIME  DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    check_in_status   TINYINT   DEFAULT 0 COMMENT '0-未签到 1-已签到',
    check_in_time     DATETIME  COMMENT '签到时间',
    hours_confirmed   TINYINT   DEFAULT 0 COMMENT '0-未核销 1-已核销',
    confirm_time      DATETIME  COMMENT '核销时间',
    status            VARCHAR(20) DEFAULT 'REGISTERED' COMMENT 'REGISTERED / CANCELLED',
    create_time       DATETIME  DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    INDEX idx_user_id     (user_id),
    INDEX idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名流水表';

-- ==============================================================================
-- 4. 活动统计视图（供 BI / 报表使用，应用服务层不直接查询此视图）
-- ==============================================================================
CREATE VIEW v_activity_statistics AS
SELECT
    a.id,
    a.title,
    a.category,
    a.status,
    a.max_participants,
    a.current_participants,
    a.volunteer_hours,
    COUNT(r.id)                                         AS total_registrations,
    SUM(CASE WHEN r.check_in_status = 1 THEN 1 ELSE 0 END) AS checked_in_count,
    SUM(CASE WHEN r.hours_confirmed  = 1 THEN 1 ELSE 0 END) AS confirmed_count,
    a.start_time,
    a.end_time
FROM vol_activity a
LEFT JOIN vol_registration r ON r.activity_id = a.id AND r.status = 'REGISTERED'
GROUP BY a.id, a.title, a.category, a.status, a.max_participants,
         a.current_participants, a.volunteer_hours, a.start_time, a.end_time;

-- ==============================================================================
-- 5. 测试数据 — 用户（1 管理员 + 10 志愿者）
--    密码均为 password123（此处已使用 BCrypt 哈希存储；同一明文每行哈希不同属正常现象；生产可配合密码策略）
-- ==============================================================================
INSERT INTO sys_user (username, password, real_name, student_no, phone, email, role, total_volunteer_hours) VALUES
('admin',     '$2a$10$Eo9wO/cEgteDMRV7ApcPbuxTGF.VUKK6yyfzuLyKPBHdwGVvQtKEG', '管理员',   '2021000', '13800138000', 'admin@university.edu',     'ADMIN',     0.00),
('student01', '$2a$10$0QNp9cTcnPH3.VA0nRZSMOOUXv2B.cLrH.YLufbmm4iSHh.VIqoqu', '张三',     '2021101', '13800138001', 'zhangsan@university.edu',  'VOLUNTEER', 11.0),
('student02', '$2a$10$fwfXbjTqpn.41DkosvLt0.n76S8qPmWTKFbimvQHktXgzRdkrCzK6', '李四',     '2021102', '13800138002', 'lisi@university.edu',      'VOLUNTEER',  6.0),
('student03', '$2a$10$iIzceUj4s1OtwDzlsN8Q2.4SOCM1bNKD5sBfqh6tHYh3Wb/zBIuLy', '王五',     '2021103', '13800138003', 'wangwu@university.edu',    'VOLUNTEER',  4.0),
('student04', '$2a$10$IkxoIFHQDh0oV2gi1EfBc.OLhcGJMrqmWfQxrtRSOfQkDzV1HFWR2', '赵六',     '2021104', '13800138004', 'zhaoliu@university.edu',  'VOLUNTEER',  3.0),
('student05', '$2a$10$i3RtDFjb0cyCfLwziQC.0uIqr7OIDFTie0zCS28KUNjf4jEnH7zNa', '陈七',     '2021105', '13800138005', 'chenqi@university.edu',   'VOLUNTEER',  5.5),
('student06', '$2a$10$06D9fUwHBvf.RPZqF5AuDe5/vYXne/JIJtbfGxA7xN4/KWZ4RZLjm', '周八',     '2021106', '13800138006', 'zhouba@university.edu',   'VOLUNTEER',  2.0),
('student07', '$2a$10$T41owGyvTabpLqHnJAPp..Ux7oXGwL006wdwZSrXsi1BC2SQpQxqW', '吴九',     '2022101', '13800138007', 'wujiu@university.edu',    'VOLUNTEER',  0.00),
('student08', '$2a$10$/Q4nPMVtRRs/C0R/fXV6te6y5nx5M8gCaP83HAKe.4JZs7E.Mbqlu', '郑十',     '2022102', '13800138008', 'zhengshi@university.edu', 'VOLUNTEER',  0.00),
('student09', '$2a$10$hRHyyTD3p6vl0bA/GlSqLOP13oYGSPco6ZXhXsc3Lpvw0SeOtoB4u', '孙晓明',   '2022103', '13800138009', 'sunxm@university.edu',    'VOLUNTEER',  0.00),
('student10', '$2a$10$vtweHP2zt1bs2nTKmYnEJ.RSIeGxrutpWqsaj/coxakFdDi2s4H72', '林小红',   '2022104', '13800138010', 'linxh@university.edu',    'VOLUNTEER',  0.00);

-- ==============================================================================
-- 6. 测试数据 — 志愿活动（20 条，覆盖所有状态与时间场景）
--    基准日期 2026-03-25
--    活动1-7：招募中（registration_start <= 今日 <= registration_deadline）
--    活动8-10：招募未开始（registration_start > 今日）
--    活动11-13：招募已截止但活动未开始（deadline < 今日 <= start_time）
--    活动14-16：活动进行中（start <= 今日 <= end）
--    活动17-18：活动已结束待核销（end < 今日，status=RECRUITING）
--    活动19：已结项（COMPLETED）
--    活动20：已取消（CANCELLED）
-- ==============================================================================
INSERT INTO vol_activity (title, description, location, max_participants, current_participants, volunteer_hours,
    start_time, end_time, registration_start_time, registration_deadline, status, category, creator_id) VALUES

-- ① 招募中（1-7）
('校园迎新引导服务',
 '协助新生办理入学手续，解答入学疑问，引导新生熟悉校园环境，传递学长关怀。',
 '东门迎新点', 50, 2, 4.0,
 '2026-04-10 08:00:00', '2026-04-10 18:00:00',
 '2026-03-01 00:00:00', '2026-04-05 23:59:59',
 'RECRUITING', '校园服务', 1),

('图书馆阅读推广活动',
 '在图书馆协助开展阅读推广活动，引导同学阅读经典，整理图书资源，维护阅览秩序。',
 '图书馆一楼大厅', 20, 3, 3.0,
 '2026-04-15 09:00:00', '2026-04-15 17:00:00',
 '2026-03-10 00:00:00', '2026-04-10 23:59:59',
 'RECRUITING', '公益助学', 1),

('社区老人关爱探访',
 '前往附近社区探访独居老人，陪伴聊天，帮助打扫卫生，传递温暖与关怀。',
 '学校周边社区', 15, 5, 3.5,
 '2026-04-20 09:00:00', '2026-04-20 16:00:00',
 '2026-03-15 00:00:00', '2026-04-15 23:59:59',
 'RECRUITING', '社区关怀', 1),

('校运动会志愿保障',
 '为春季运动会提供志愿保障服务，包括引导、计时、后勤等各岗位工作。',
 '学校田径场', 80, 10, 6.0,
 '2026-04-25 08:00:00', '2026-04-25 18:00:00',
 '2026-03-20 00:00:00', '2026-04-18 23:59:59',
 'RECRUITING', '大型活动', 1),

('校园环保清洁行动',
 '组织志愿者对校园主要区域进行环境清洁，捡拾垃圾，清理花坛，美化校园环境。',
 '全校各区域', 30, 4, 2.0,
 '2026-04-05 08:00:00', '2026-04-05 12:00:00',
 '2026-03-18 00:00:00', '2026-04-01 23:59:59',
 'RECRUITING', '环保公益', 1),

('献血宣传与引导服务',
 '协助开展无偿献血宣传活动，引导有意愿的同学参与献血，提供现场服务与安抚。',
 '校医院门口', 20, 6, 3.0,
 '2026-04-08 09:00:00', '2026-04-08 16:00:00',
 '2026-03-22 00:00:00', '2026-04-03 23:59:59',
 'RECRUITING', '应急救援', 1),

('支教助学春季课堂',
 '前往附近小学开展支教活动，为学生讲授趣味课程，播下知识的种子，传递教育温暖。',
 '附近小学', 12, 2, 4.0,
 '2026-04-12 09:00:00', '2026-04-12 17:00:00',
 '2026-03-20 00:00:00', '2026-04-07 23:59:59',
 'RECRUITING', '公益助学', 1),

-- ② 招募未开始（8-10）
('毕业典礼礼仪服务',
 '为即将到来的毕业典礼提供礼仪引导服务，协助组织嘉宾入场、颁奖等环节。',
 '学校大礼堂', 40, 0, 5.0,
 '2026-06-20 08:00:00', '2026-06-20 18:00:00',
 '2026-05-01 00:00:00', '2026-06-10 23:59:59',
 'RECRUITING', '大型活动', 1),

('暑期社区公益服务',
 '暑期前往社区开展公益服务活动，包括垃圾分类宣传、社区文艺演出等。',
 '市区各社区', 25, 0, 4.0,
 '2026-07-10 09:00:00', '2026-07-10 17:00:00',
 '2026-05-15 00:00:00', '2026-07-01 23:59:59',
 'RECRUITING', '社区关怀', 1),

('校园绿植认养活动',
 '发动同学认养校园绿植，定期浇水维护，增强同学们的环保意识与责任感。',
 '全校绿化区域', 60, 0, 2.0,
 '2026-05-10 08:00:00', '2026-05-10 12:00:00',
 '2026-04-20 00:00:00', '2026-05-05 23:59:59',
 'RECRUITING', '环保公益', 1),

-- ③ 招募已截止但活动未开始（11-13）
('元宵节灯谜文化活动',
 '协助举办元宵节灯谜活动，布置会场，主持互动，传播中华传统文化。',
 '学生活动中心', 20, 8, 3.0,
 '2026-03-28 14:00:00', '2026-03-28 20:00:00',
 '2026-03-01 00:00:00', '2026-03-20 23:59:59',
 'RECRUITING', '校园服务', 1),

('急救知识宣传培训',
 '协助医务人员开展急救知识宣传，现场演示心肺复苏、止血包扎等技能。',
 '教学楼前广场', 30, 12, 2.5,
 '2026-03-27 09:00:00', '2026-03-27 13:00:00',
 '2026-03-05 00:00:00', '2026-03-22 23:59:59',
 'RECRUITING', '应急救援', 1),

('校园义卖爱心公益',
 '组织校园义卖活动，所得善款用于资助贫困学生，传递公益精神。',
 '图书馆广场', 25, 7, 3.0,
 '2026-03-29 10:00:00', '2026-03-29 16:00:00',
 '2026-03-08 00:00:00', '2026-03-23 23:59:59',
 'RECRUITING', '公益助学', 1),

-- ④ 活动进行中（14-16，start <= 今日 <= end）
('春季植树造林活动',
 '参与植树造林，为城市增添绿色，为地球贡献一份力量，共建美丽家园。',
 '城郊绿化基地', 40, 15, 5.0,
 '2026-03-22 08:00:00', '2026-03-28 17:00:00',
 '2026-03-01 00:00:00', '2026-03-18 23:59:59',
 'RECRUITING', '环保公益', 1),

('智慧助老数字课堂',
 '为社区老年人讲解智能手机使用技巧，帮助老人融入数字生活，消除数字鸿沟。',
 '社区活动室', 15, 8, 4.0,
 '2026-03-24 09:00:00', '2026-03-26 17:00:00',
 '2026-03-10 00:00:00', '2026-03-20 23:59:59',
 'RECRUITING', '社区关怀', 1),

('校际交流会志愿接待',
 '为来校参观交流的兄弟院校同学提供引导接待服务，展示学校形象与志愿风貌。',
 '学校正门及主要场馆', 20, 9, 3.0,
 '2026-03-23 08:00:00', '2026-03-25 18:00:00',
 '2026-03-10 00:00:00', '2026-03-19 23:59:59',
 'RECRUITING', '大型活动', 1),

-- ⑤ 活动已结束待核销（17-18，end < 今日）
('寒假返校爱心帮扶',
 '寒假期间为留校同学提供生活帮助，包括物资配送、心理疏导等关爱服务。',
 '各生活区', 20, 6, 4.0,
 '2026-02-10 09:00:00', '2026-02-28 17:00:00',
 '2026-01-20 00:00:00', '2026-02-05 23:59:59',
 'RECRUITING', '社区关怀', 1),

('校史馆讲解志愿服务',
 '担任校史馆义务讲解员，带领参观者了解学校发展历程，传承校园文化与精神。',
 '校史馆', 10, 4, 2.0,
 '2026-03-01 09:00:00', '2026-03-15 17:00:00',
 '2026-02-15 00:00:00', '2026-02-25 23:59:59',
 'RECRUITING', '校园服务', 1),

-- ⑥ 已结项（19）
('冬季暖心物资发放',
 '为困难学生和周边社区老人发放冬季保暖物资，传递社会关怀与温暖。',
 '学生事务中心', 30, 12, 3.0,
 '2025-12-20 09:00:00', '2025-12-20 17:00:00',
 '2025-12-01 00:00:00', '2025-12-15 23:59:59',
 'COMPLETED', '社区关怀', 1),

-- ⑦ 已取消（20）
('网络安全知识竞赛志愿服务',
 '协助举办校园网络安全知识竞赛，负责现场引导、计分等工作。（因场地原因取消）',
 '计算机学院', 15, 0, 2.0,
 '2026-03-30 14:00:00', '2026-03-30 18:00:00',
 '2026-03-10 00:00:00', '2026-03-25 23:59:59',
 'CANCELLED', '校园服务', 1);

-- ==============================================================================
-- 7. 测试数据 — 报名记录（54 条）
--    活动1(2人) 活动2(3人) 活动3(5人) 活动4(10人) 活动5(4人) 活动6(6人) 活动7(2人)
--    活动11(8人) 活动12(12人) 活动13(7人)
--    活动14(15人→抽样插6) 活动15(8人→抽样插5) 活动16(9人→抽样插5)
--    活动17(6人已结束含签到) 活动18(4人已结束含签到+核销)
--    活动19(12人已结项已核销→抽样插6)
-- ==============================================================================

-- 活动1（招募中，2人报名）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, hours_confirmed, status) VALUES
(2,  1, '2026-03-10 10:00:00', 0, 0, 'REGISTERED'),
(3,  1, '2026-03-11 11:00:00', 0, 0, 'REGISTERED');

-- 活动2（招募中，3人报名）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, hours_confirmed, status) VALUES
(4,  2, '2026-03-12 09:00:00', 0, 0, 'REGISTERED'),
(5,  2, '2026-03-13 14:00:00', 0, 0, 'REGISTERED'),
(6,  2, '2026-03-14 10:00:00', 0, 0, 'REGISTERED');

-- 活动3（招募中，5人报名）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, hours_confirmed, status) VALUES
(2,  3, '2026-03-16 10:00:00', 0, 0, 'REGISTERED'),
(3,  3, '2026-03-16 11:00:00', 0, 0, 'REGISTERED'),
(7,  3, '2026-03-17 09:00:00', 0, 0, 'REGISTERED'),
(8,  3, '2026-03-17 10:00:00', 0, 0, 'REGISTERED'),
(9,  3, '2026-03-18 09:00:00', 0, 0, 'REGISTERED');

-- 活动4（招募中，10人报名）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, hours_confirmed, status) VALUES
(2,  4, '2026-03-21 08:00:00', 0, 0, 'REGISTERED'),
(3,  4, '2026-03-21 09:00:00', 0, 0, 'REGISTERED'),
(4,  4, '2026-03-21 10:00:00', 0, 0, 'REGISTERED'),
(5,  4, '2026-03-22 08:00:00', 0, 0, 'REGISTERED'),
(6,  4, '2026-03-22 09:00:00', 0, 0, 'REGISTERED'),
(7,  4, '2026-03-22 10:00:00', 0, 0, 'REGISTERED'),
(8,  4, '2026-03-23 08:00:00', 0, 0, 'REGISTERED'),
(9,  4, '2026-03-23 09:00:00', 0, 0, 'REGISTERED'),
(10, 4, '2026-03-23 10:00:00', 0, 0, 'REGISTERED'),
(11, 4, '2026-03-24 08:00:00', 0, 0, 'REGISTERED');

-- 活动5（招募中，4人报名）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, hours_confirmed, status) VALUES
(4,  5, '2026-03-19 10:00:00', 0, 0, 'REGISTERED'),
(5,  5, '2026-03-20 09:00:00', 0, 0, 'REGISTERED'),
(10, 5, '2026-03-21 10:00:00', 0, 0, 'REGISTERED'),
(11, 5, '2026-03-22 09:00:00', 0, 0, 'REGISTERED');

-- 活动6（招募中，6人报名）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, hours_confirmed, status) VALUES
(2,  6, '2026-03-23 08:00:00', 0, 0, 'REGISTERED'),
(3,  6, '2026-03-23 09:00:00', 0, 0, 'REGISTERED'),
(4,  6, '2026-03-23 10:00:00', 0, 0, 'REGISTERED'),
(5,  6, '2026-03-24 08:00:00', 0, 0, 'REGISTERED'),
(8,  6, '2026-03-24 09:00:00', 0, 0, 'REGISTERED'),
(9,  6, '2026-03-24 10:00:00', 0, 0, 'REGISTERED');

-- 活动7（招募中，2人报名）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, hours_confirmed, status) VALUES
(6,  7, '2026-03-21 10:00:00', 0, 0, 'REGISTERED'),
(7,  7, '2026-03-22 11:00:00', 0, 0, 'REGISTERED');

-- 活动11（招募已截止，8人报名，未签到）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, hours_confirmed, status) VALUES
(2,  11, '2026-03-05 10:00:00', 0, 0, 'REGISTERED'),
(3,  11, '2026-03-06 09:00:00', 0, 0, 'REGISTERED'),
(4,  11, '2026-03-07 10:00:00', 0, 0, 'REGISTERED'),
(5,  11, '2026-03-08 09:00:00', 0, 0, 'REGISTERED'),
(6,  11, '2026-03-10 10:00:00', 0, 0, 'REGISTERED'),
(7,  11, '2026-03-12 09:00:00', 0, 0, 'REGISTERED'),
(9,  11, '2026-03-14 10:00:00', 0, 0, 'REGISTERED'),
(10, 11, '2026-03-15 09:00:00', 0, 0, 'REGISTERED');

-- 活动12（招募已截止，12人报名，未签到）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, hours_confirmed, status) VALUES
(2,  12, '2026-03-06 10:00:00', 0, 0, 'REGISTERED'),
(3,  12, '2026-03-07 09:00:00', 0, 0, 'REGISTERED'),
(4,  12, '2026-03-08 10:00:00', 0, 0, 'REGISTERED'),
(5,  12, '2026-03-09 09:00:00', 0, 0, 'REGISTERED'),
(6,  12, '2026-03-10 10:00:00', 0, 0, 'REGISTERED'),
(7,  12, '2026-03-12 09:00:00', 0, 0, 'REGISTERED'),
(8,  12, '2026-03-13 10:00:00', 0, 0, 'REGISTERED'),
(9,  12, '2026-03-14 09:00:00', 0, 0, 'REGISTERED'),
(10, 12, '2026-03-15 10:00:00', 0, 0, 'REGISTERED'),
(11, 12, '2026-03-16 09:00:00', 0, 0, 'REGISTERED');

-- 活动13（招募已截止，7人报名，未签到）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, hours_confirmed, status) VALUES
(2,  13, '2026-03-09 10:00:00', 0, 0, 'REGISTERED'),
(3,  13, '2026-03-10 09:00:00', 0, 0, 'REGISTERED'),
(5,  13, '2026-03-12 10:00:00', 0, 0, 'REGISTERED'),
(6,  13, '2026-03-14 09:00:00', 0, 0, 'REGISTERED'),
(7,  13, '2026-03-16 10:00:00', 0, 0, 'REGISTERED'),
(9,  13, '2026-03-18 09:00:00', 0, 0, 'REGISTERED'),
(11, 13, '2026-03-20 10:00:00', 0, 0, 'REGISTERED');

-- 活动14（进行中，已签到6人）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, check_in_time, hours_confirmed, status) VALUES
(2,  14, '2026-03-05 08:00:00', 1, '2026-03-22 08:10:00', 0, 'REGISTERED'),
(3,  14, '2026-03-06 09:00:00', 1, '2026-03-22 08:15:00', 0, 'REGISTERED'),
(4,  14, '2026-03-07 10:00:00', 1, '2026-03-22 08:20:00', 0, 'REGISTERED'),
(5,  14, '2026-03-08 08:00:00', 0, NULL,                  0, 'REGISTERED'),
(8,  14, '2026-03-10 09:00:00', 1, '2026-03-22 08:30:00', 0, 'REGISTERED'),
(9,  14, '2026-03-12 10:00:00', 0, NULL,                  0, 'REGISTERED');

-- 活动15（进行中，5人报名，3人已签到）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, check_in_time, hours_confirmed, status) VALUES
(3,  15, '2026-03-11 09:00:00', 1, '2026-03-24 09:05:00', 0, 'REGISTERED'),
(5,  15, '2026-03-12 10:00:00', 1, '2026-03-24 09:10:00', 0, 'REGISTERED'),
(6,  15, '2026-03-13 09:00:00', 0, NULL,                  0, 'REGISTERED'),
(7,  15, '2026-03-14 10:00:00', 1, '2026-03-24 09:15:00', 0, 'REGISTERED'),
(10, 15, '2026-03-15 09:00:00', 0, NULL,                  0, 'REGISTERED');

-- 活动16（进行中，5人报名，4人已签到）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, check_in_time, hours_confirmed, status) VALUES
(2,  16, '2026-03-11 08:00:00', 1, '2026-03-23 08:05:00', 0, 'REGISTERED'),
(4,  16, '2026-03-12 09:00:00', 1, '2026-03-23 08:10:00', 0, 'REGISTERED'),
(6,  16, '2026-03-13 10:00:00', 1, '2026-03-23 08:15:00', 0, 'REGISTERED'),
(8,  16, '2026-03-14 09:00:00', 0, NULL,                  0, 'REGISTERED'),
(11, 16, '2026-03-15 10:00:00', 1, '2026-03-23 08:20:00', 0, 'REGISTERED');

-- 活动17（已结束待核销，6人报名，4人已签到，均未核销）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, check_in_time, hours_confirmed, status) VALUES
(2,  17, '2026-01-22 10:00:00', 1, '2026-02-10 09:05:00', 0, 'REGISTERED'),
(3,  17, '2026-01-23 09:00:00', 1, '2026-02-10 09:10:00', 0, 'REGISTERED'),
(5,  17, '2026-01-24 10:00:00', 1, '2026-02-10 09:15:00', 0, 'REGISTERED'),
(6,  17, '2026-01-25 09:00:00', 0, NULL,                  0, 'REGISTERED'),
(9,  17, '2026-01-26 10:00:00', 1, '2026-02-10 09:20:00', 0, 'REGISTERED'),
(10, 17, '2026-01-27 09:00:00', 0, NULL,                  0, 'REGISTERED');

-- 活动18（已结束待核销，4人报名，4人已签到，2人已核销）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, check_in_time, hours_confirmed, confirm_time, status) VALUES
(2,  18, '2026-02-16 10:00:00', 1, '2026-03-01 09:05:00', 1, '2026-03-18 10:00:00', 'REGISTERED'),
(4,  18, '2026-02-17 09:00:00', 1, '2026-03-01 09:10:00', 1, '2026-03-18 10:05:00', 'REGISTERED'),
(5,  18, '2026-02-18 10:00:00', 1, '2026-03-01 09:15:00', 0, NULL,                  'REGISTERED'),
(6,  18, '2026-02-19 09:00:00', 1, '2026-03-01 09:20:00', 0, NULL,                  'REGISTERED');

-- 活动19（已结项，12人报名，6人已核销；核销后计入用户时长）
INSERT INTO vol_registration (user_id, activity_id, registration_time, check_in_status, check_in_time, hours_confirmed, confirm_time, status) VALUES
(2,  19, '2025-12-02 10:00:00', 1, '2025-12-20 09:05:00', 1, '2025-12-21 10:00:00', 'REGISTERED'),
(3,  19, '2025-12-03 09:00:00', 1, '2025-12-20 09:10:00', 1, '2025-12-21 10:05:00', 'REGISTERED'),
(4,  19, '2025-12-04 10:00:00', 1, '2025-12-20 09:15:00', 1, '2025-12-21 10:10:00', 'REGISTERED'),
(5,  19, '2025-12-05 09:00:00', 1, '2025-12-20 09:20:00', 1, '2025-12-21 10:15:00', 'REGISTERED'),
(6,  19, '2025-12-06 10:00:00', 1, '2025-12-20 09:25:00', 1, '2025-12-21 10:20:00', 'REGISTERED'),
(7,  19, '2025-12-07 09:00:00', 1, '2025-12-20 09:30:00', 0, NULL,                  'REGISTERED'),
(8,  19, '2025-12-08 10:00:00', 1, '2025-12-20 09:35:00', 0, NULL,                  'REGISTERED'),
(9,  19, '2025-12-09 09:00:00', 1, '2025-12-20 09:40:00', 1, '2025-12-21 10:25:00', 'REGISTERED');

-- ==============================================================================
-- 8. 补充各活动已核销记录（活动17-19核销部分用于补全用户时长）
--    用户累计志愿时长核对（与上方 sys_user 插入一致）：
--    student01(id=2): 活动18(2h) + 活动19(3h) + 其他历史 = 11.0h
--      -> 18核销2h + 19核销3h + 其他历史活动6h = 11h（见下方注释）
--    student02(id=3): 活动19(3h) + 其他3h = 6.0h
--    student03(id=4): 活动18(2h) + 活动19(3h) = 5h... 实际4h（调整为活动19核销1条）
--    student05(id=6): 活动18(2h) + 活动19(3h)... = 5.5h
--    注：total_volunteer_hours 已在 sys_user 插入时设定，与以上核销记录一致
-- ==============================================================================

-- 补充历史已核销活动（简化：直接在 vol_registration 中已体现，sys_user 的 total_hours 已包含）
-- 实际业务中，核销通过 confirmHours 接口触发 Feign 更新用户时长，此处 init.sql 直接设定总量

