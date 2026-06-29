-- 创建数据库
CREATE DATABASE IF NOT EXISTS health_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE health_management;

-- ==============================
-- 系统管理表
-- ==============================
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(64) NOT NULL UNIQUE,
    nick_name VARCHAR(64),
    password VARCHAR(128) NOT NULL,
    email VARCHAR(128),
    phone_number VARCHAR(20),
    sex CHAR(1) DEFAULT '0',
    avatar VARCHAR(256),
    status CHAR(1) DEFAULT '0',
    dept_id BIGINT,
    login_ip VARCHAR(128),
    login_date DATETIME,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(64) NOT NULL,
    role_key VARCHAR(64) NOT NULL UNIQUE,
    role_sort INT DEFAULT 0,
    data_scope CHAR(1) DEFAULT '1',
    status CHAR(1) DEFAULT '0',
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_name VARCHAR(64) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    order_num INT DEFAULT 0,
    path VARCHAR(256),
    component VARCHAR(256),
    query VARCHAR(256),
    perms VARCHAR(128),
    icon VARCHAR(64),
    menu_type CHAR(1) DEFAULT '',
    visible CHAR(1) DEFAULT '0',
    status CHAR(1) DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='菜单表';

CREATE TABLE sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_name VARCHAR(64) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    order_num INT DEFAULT 0,
    leader VARCHAR(64),
    phone VARCHAR(20),
    email VARCHAR(128),
    status CHAR(1) DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='部门表';

-- ==============================
-- 会员管理
-- ==============================
CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    gender CHAR(1),
    birthday DATE,
    phone VARCHAR(20),
    id_card VARCHAR(20),
    address VARCHAR(256),
    blood_type VARCHAR(8),
    height DECIMAL(5,2),
    weight DECIMAL(5,2),
    allergy_history TEXT,
    family_history TEXT,
    smoking_status VARCHAR(32),
    drinking_status VARCHAR(32),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='会员表';

CREATE TABLE exam_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    plan_name VARCHAR(128),
    plan_date DATE,
    package_id BIGINT,
    status VARCHAR(32) DEFAULT 'PENDING',
    report_path VARCHAR(256),
    conclusion TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='体检计划表';

-- ==============================
-- 预约管理
-- ==============================
CREATE TABLE appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    package_id BIGINT,
    appointment_date DATE NOT NULL,
    appointment_time TIME,
    status VARCHAR(32) DEFAULT 'PENDING',
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='预约表';

CREATE TABLE package_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_name VARCHAR(128) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    suitable_for VARCHAR(256),
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='套餐表';

CREATE TABLE package_item_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='套餐项目明细表';

CREATE TABLE exam_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(128) NOT NULL,
    item_code VARCHAR(64),
    unit VARCHAR(32),
    price DECIMAL(10,2),
    reference_range VARCHAR(256),
    category_id BIGINT,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='检测项表';

CREATE TABLE exam_item_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='检测项目组表';

-- ==============================
-- 健康评估
-- ==============================
CREATE TABLE assessment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    template_id BIGINT,
    total_score DECIMAL(8,2),
    risk_level VARCHAR(32),
    conclusion TEXT,
    suggestion TEXT,
    assessor_id BIGINT,
    assess_date DATE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='评估记录表';

CREATE TABLE assessment_indicator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_name VARCHAR(128) NOT NULL,
    indicator_type VARCHAR(64),
    unit VARCHAR(32),
    min_value DECIMAL(10,4),
    max_value DECIMAL(10,4),
    risk_level VARCHAR(32),
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='评估指标表';

CREATE TABLE tcm_constitution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    constitution_type VARCHAR(64),
    score INT,
    description TEXT,
    health_advice TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='中医体质辨识表';

CREATE TABLE psychology_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    assessment_type VARCHAR(64),
    total_score INT,
    result_level VARCHAR(64),
    analysis TEXT,
    suggestion TEXT,
    assess_date DATE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='心理评测表';

-- ==============================
-- 健康干预
-- ==============================
CREATE TABLE intervention_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    plan_name VARCHAR(128),
    plan_type VARCHAR(64),
    target_goal TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='干预方案表';

CREATE TABLE crowd_program (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_name VARCHAR(128) NOT NULL,
    target_crowd VARCHAR(256),
    program_content TEXT,
    frequency VARCHAR(128),
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='人群方案表';

CREATE TABLE chronic_disease (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    disease_name VARCHAR(128) NOT NULL,
    diagnosis_date VARCHAR(32),
    severity VARCHAR(32),
    medication TEXT,
    control_status VARCHAR(64),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='慢病管理表';

CREATE TABLE diet_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    log_date DATE NOT NULL,
    meal_type VARCHAR(32),
    food_name VARCHAR(128),
    quantity DECIMAL(10,3),
    calories DECIMAL(10,2),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='膳食日志表';

-- ==============================
-- 知识库
-- ==============================
CREATE TABLE knowledge_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    content LONGTEXT,
    category VARCHAR(64),
    author VARCHAR(64),
    cover_image VARCHAR(256),
    view_count INT DEFAULT 0,
    status VARCHAR(32) DEFAULT 'PUBLISHED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='科普文章表';

CREATE TABLE exercise_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exercise_name VARCHAR(128) NOT NULL,
    exercise_type VARCHAR(64),
    difficulty VARCHAR(32),
    duration INT,
    calories_burn DECIMAL(8,2),
    description TEXT,
    video_url VARCHAR(256),
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='运动项目库';

CREATE TABLE recipe_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_name VARCHAR(128) NOT NULL,
    meal_type VARCHAR(32),
    suitable_for VARCHAR(256),
    total_calories DECIMAL(8,2),
    ingredients TEXT,
    steps TEXT,
    nutrition_info TEXT,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='食谱库';

CREATE TABLE disease_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    disease_name VARCHAR(128) NOT NULL,
    category VARCHAR(64),
    symptoms TEXT,
    causes TEXT,
    treatment TEXT,
    prevention TEXT,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='疾病库';

CREATE TABLE education_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    content LONGTEXT,
    content_type VARCHAR(32),
    target_audience VARCHAR(128),
    word_id BIGINT,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='宣教内容表';

CREATE TABLE education_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word_name VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='宣教词管理表';

-- ==============================
-- AI对话
-- ==============================
CREATE TABLE ai_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(256) DEFAULT '新的对话',
    last_message TEXT,
    message_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='AI对话会话表';

CREATE TABLE ai_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT,
    model VARCHAR(64) DEFAULT 'deepseek-chat',
    tokens INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='AI消息表';

-- ==============================
-- 初始化数据
-- ==============================
INSERT INTO sys_user (user_name, nick_name, password, email, status) VALUES
('admin', '系统管理员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'admin@health.com', '0');

INSERT INTO sys_dept (dept_name, parent_id, order_num, status) VALUES
('总部', 0, 0, '0'),
('内科', 0, 1, '0'),
('外科', 0, 2, '0'),
('体检中心', 0, 3, '0');

INSERT INTO sys_role (role_name, role_key, role_sort, status) VALUES
('超级管理员', 'admin', 1, '0'),
('医生', 'doctor', 2, '0'),
('护士', 'nurse', 3, '0'),
('会员', 'member', 4, '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, icon, menu_type, visible, status) VALUES
('系统管理', 0, 1, '/system', '', 'system', 'M', '0', '0'),
('用户管理', 1, 1, '/system/user', 'system/user/index', 'user', 'C', '0', '0'),
('角色管理', 1, 2, '/system/role', 'system/role/index', 'peoples', 'C', '0', '0'),
('菜单管理', 1, 3, '/system/menu', 'system/menu/index', 'tree-table', 'C', '0', '0'),
('会员管理', 0, 2, '/member', 'member/index', 'people', 'C', '0', '0'),
('预约管理', 0, 3, '/appointment', 'appointment/index', 'date', 'C', '0', '0'),
('健康评估', 0, 4, '/assessment', 'assessment/index', 'chart', 'C', '0', '0'),
('健康干预', 0, 5, '/intervention', 'intervention/index', 'guide', 'C', '0', '0'),
('知识库', 0, 6, '/knowledge', 'knowledge/index', 'education', 'C', '0', '0'),
('AI助手', 0, 7, '/ai-agent', 'ai/index', 'cpu', 'C', '0', '0');
