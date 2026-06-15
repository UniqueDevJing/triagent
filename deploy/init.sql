-- 传智健康管理系统 初始化 SQL
CREATE DATABASE IF NOT EXISTS health_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE health_management;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码(BCrypt)',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender TINYINT DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    age INT DEFAULT 0 COMMENT '年龄',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    address VARCHAR(200) COMMENT '地址',
    emergency_contact VARCHAR(50) COMMENT '紧急联系人',
    emergency_phone VARCHAR(20) COMMENT '紧急联系电话',
    blood_type VARCHAR(5) COMMENT '血型',
    height DECIMAL(5,2) COMMENT '身高(cm)',
    weight DECIMAL(5,2) COMMENT '体重(kg)',
    medical_history JSON COMMENT '既往病史',
    allergies JSON COMMENT '过敏史',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='用户表';

-- 健康档案表
CREATE TABLE IF NOT EXISTS health_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    type VARCHAR(20) NOT NULL COMMENT '类型: EXAM/CLINIC/SELF',
    metrics JSON COMMENT '健康指标',
    report_url VARCHAR(500) COMMENT '报告URL',
    doctor_notes TEXT COMMENT '医生备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_record_date (record_date),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB COMMENT='健康档案表';

-- 评估量表模板表
CREATE TABLE IF NOT EXISTS assessment_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL COMMENT '量表标题',
    description VARCHAR(500) COMMENT '描述',
    category VARCHAR(50) COMMENT '分类',
    questions JSON NOT NULL COMMENT '问题列表',
    scoring_rules JSON NOT NULL COMMENT '评分规则',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='评估量表模板表';

-- 评估记录表
CREATE TABLE IF NOT EXISTS assessment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    template_id BIGINT NOT NULL COMMENT '量表ID',
    answers JSON COMMENT '答案',
    total_score DECIMAL(5,1) COMMENT '总分',
    risk_level VARCHAR(20) COMMENT '风险等级: LOW/MEDIUM/HIGH',
    report_text TEXT COMMENT '评估报告',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_template_id (template_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (template_id) REFERENCES assessment_templates(id)
) ENGINE=InnoDB COMMENT='评估记录表';

-- 干预计划表
CREATE TABLE IF NOT EXISTS intervention_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(100) NOT NULL COMMENT '计划标题',
    goal TEXT COMMENT '目标',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/COMPLETED/CANCELLED',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB COMMENT='干预计划表';

-- 干预任务表
CREATE TABLE IF NOT EXISTS intervention_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL COMMENT '计划ID',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description TEXT COMMENT '描述',
    due_date DATE COMMENT '截止日期',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/DONE',
    completed_at DATETIME COMMENT '完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plan_id (plan_id),
    FOREIGN KEY (plan_id) REFERENCES intervention_plans(id)
) ENGINE=InnoDB COMMENT='干预任务表';

-- 知识库分类表
CREATE TABLE IF NOT EXISTS knowledge_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB COMMENT='知识库分类表';

-- 知识库文章表
CREATE TABLE IF NOT EXISTS knowledge_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL COMMENT '分类ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    summary VARCHAR(500) COMMENT '摘要',
    content TEXT COMMENT '内容',
    author VARCHAR(50) COMMENT '作者',
    view_count INT DEFAULT 0 COMMENT '阅读数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_id (category_id),
    FOREIGN KEY (category_id) REFERENCES knowledge_categories(id)
) ENGINE=InnoDB COMMENT='知识库文章表';

-- AI对话记录表
CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '用户ID',
    session_id VARCHAR(50) COMMENT '会话ID',
    role VARCHAR(10) NOT NULL COMMENT '角色: USER/AI',
    content TEXT NOT NULL COMMENT '内容',
    feature_type VARCHAR(30) COMMENT '功能类型: CHAT/ANALYSIS/MEDICATION/COMPANION/BEHAVIOR',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB COMMENT='AI对话记录表';

-- ============ 测试数据 ============

-- 管理员账号: admin / admin123 (BCrypt编码)
INSERT INTO users (username, password, role, name, gender, age, phone, email, address, blood_type, height, weight) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKvZQ7q', 'ADMIN', '系统管理员', 1, 30, '13800000000', 'admin@health.com', '总部', 'A', 175.0, 70.0),
('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKvZQ7q', 'USER', '张三', 1, 65, '13800138001', 'zhangsan@test.com', '北京市朝阳区', 'A', 172.5, 70.0),
('lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKvZQ7q', 'USER', '李四', 2, 58, '13800138002', 'lisi@test.com', '上海市浦东新区', 'B', 160.0, 55.0),
('wangwu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKvZQ7q', 'USER', '王五', 1, 72, '13800138003', 'wangwu@test.com', '广州市天河区', 'O', 168.0, 68.0),
('zhaoliu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKvZQ7q', 'USER', '赵六', 2, 80, '13800138004', 'zhaoliu@test.com', '深圳市南山区', 'AB', 155.0, 50.0);

-- 健康档案测试数据
INSERT INTO health_records (user_id, record_date, type, metrics, report_url, doctor_notes, created_at) VALUES
(2, '2026-06-10', '体检', '{"血压":"150/95","空腹血糖":6.3,"总胆固醇":5.8,"甘油三酯":2.1,"高密度脂蛋白":1.2,"低密度脂蛋白":3.8,"心率":78,"BMI":25.4}', '', '血压偏高，建议低盐饮食并定期监测。血糖处于糖尿病前期，需控制碳水化合物摄入。', NOW()),
(2, '2026-03-15', '体检', '{"血压":"145/90","空腹血糖":5.9,"总胆固醇":5.5,"甘油三酯":1.8,"高密度脂蛋白":1.3,"低密度脂蛋白":3.5,"心率":75,"BMI":25.1}', '', '与上次相比血压略有下降，继续保持。', NOW()),
(3, '2026-06-08', '自测', '{"血压":"135/85","空腹血糖":5.2,"心率":72}', '', '日常监测数据正常，继续保持健康生活方式。', NOW()),
(4, '2026-06-05', '体检', '{"血压":"165/100","空腹血糖":7.8,"总胆固醇":6.2,"甘油三酯":2.8,"心率":82,"BMI":27.8}', '', '血压血糖均偏高，建议立即开始药物治疗。血脂异常需控制饮食。', NOW()),
(5, '2026-06-01', '体检', '{"血压":"130/80","空腹血糖":5.1,"总胆固醇":5.0,"心率":70,"BMI":22.5}', '', '各项指标基本正常，建议保持运动习惯。', NOW());

-- 评估量表测试数据
INSERT INTO assessment_templates (title, description, category, questions, scoring_rules) VALUES
('老年人健康综合评估', '用于评估老年人的整体健康状况', '综合评估',
 '[{"id":1,"text":"日常生活自理能力","options":[{"label":"完全自理","score":0},{"label":"部分需要帮助","score":5},{"label":"完全依赖","score":10}]},{"id":2,"text":"近三个月跌倒次数","options":[{"label":"0次","score":0},{"label":"1-2次","score":5},{"label":"3次以上","score":10}]},{"id":3,"text":"睡眠质量","options":[{"label":"良好","score":0},{"label":"一般","score":3},{"label":"差","score":6}]},{"id":4,"text":"情绪状态","options":[{"label":"稳定乐观","score":0},{"label":"偶尔低落","score":4},{"label":"长期抑郁","score":8}]}]',
 '{"LOW":{"min":0,"max":8,"desc":"健康状况良好"},"MEDIUM":{"min":9,"max":20,"desc":"需要关注"},"HIGH":{"min":21,"max":100,"desc":"需要重点干预"}}');

-- 知识库分类
INSERT INTO knowledge_categories (name, icon, sort_order) VALUES
('慢病管理', 'fa-heartbeat', 1),
('营养饮食', 'fa-cutlery', 2),
('运动康复', 'fa-bicycle', 3),
('心理健康', 'fa-smile-o', 4),
('养老护理', 'fa-user-md', 5);

-- 知识库文章
INSERT INTO knowledge_articles (category_id, title, summary, content, author, view_count) VALUES
(1, '高血压日常管理指南', '介绍高血压患者日常饮食、运动和用药注意事项', '# 高血压日常管理指南\n\n## 饮食建议\n- 低盐饮食，每日食盐不超过6g\n- 多摄入蔬菜水果\n\n## 运动建议\n- 每周至少150分钟中等强度运动\n- 避免剧烈运动\n\n## 用药提醒\n- 按时服药，不可自行停药', '李医生', 128),
(2, '老年人营养搭配方案', '针对老年人的科学饮食方案', '# 老年人营养搭配方案\n\n## 蛋白质\n每日摄入1.0-1.2g/kg体重的优质蛋白\n\n## 钙质\n多摄入牛奶、豆制品，预防骨质疏松', '王营养师', 96);
