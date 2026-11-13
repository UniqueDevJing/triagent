-- V2__system_settings.sql
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室表';

INSERT INTO departments (name, code, sort_order) VALUES
('内科', 'NEIKE', 1), ('外科', 'WAIKE', 2), ('体检中心', 'TIJIAN', 3),
('中医科', 'ZHONGYI', 4), ('心理科', 'XINLI', 5), ('营养科', 'YINGYANG', 6);

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    menus JSON,
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

INSERT INTO roles (name, code, menus) VALUES
('系统管理员', 'ADMIN', '["dashboard","members","appointments","assessments","interventions","knowledge","statistics","system"]'),
('医生', 'DOCTOR', '["dashboard","members","assessments","interventions","knowledge"]'),
('护士', 'NURSE', '["dashboard","appointments","health-records","knowledge"]');

CREATE TABLE IF NOT EXISTS menus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    path VARCHAR(100),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

INSERT INTO menus (parent_id, name, path, icon, sort_order) VALUES
(0, '工作台', '/dashboard', 'DataAnalysis', 1),
(0, '会员管理', '/members', 'User', 2),
(0, '预约管理', '/appointments', 'Calendar', 3),
(0, '健康评估', '/assessments', 'DocumentChecked', 4),
(0, '健康干预', '/interventions', 'SetUp', 5),
(0, '知识库', '/knowledge', 'Reading', 6),
(0, '统计分析', '/statistics', 'TrendCharts', 7),
(0, '系统设置', '/system', 'Setting', 8);

-- 扩展 users 表: 添加 department_id
ALTER TABLE users ADD COLUMN department_id BIGINT DEFAULT NULL AFTER email,
ADD COLUMN avatar VARCHAR(200) DEFAULT NULL AFTER department_id,
ADD COLUMN status TINYINT DEFAULT 1 AFTER avatar;
