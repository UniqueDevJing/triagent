USE health_management;

-- 1. 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB COMMENT='用户-角色关联表';

-- 2. 角色-菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB COMMENT='角色-菜单关联表';

-- 3. 系统通知表
CREATE TABLE IF NOT EXISTS sys_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(256) NOT NULL,
    content TEXT,
    notice_type VARCHAR(32) DEFAULT 'SYSTEM',
    is_read TINYINT DEFAULT 0,
    extra VARCHAR(512),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_is_read (is_read)
) ENGINE=InnoDB COMMENT='系统通知表';

-- 4. 智能助手表（中性命名）
CREATE TABLE IF NOT EXISTS intelligent_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(256) DEFAULT '新的对话',
    last_message TEXT,
    message_count INT DEFAULT 0,
    feature_type VARCHAR(32) DEFAULT 'CHAT',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='智能助手会话表';

CREATE TABLE IF NOT EXISTS intelligent_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT,
    model VARCHAR(64),
    tokens INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_conversation_id (conversation_id)
) ENGINE=InnoDB COMMENT='智能助手消息表';

-- 5. 重置密码为 BCrypt 哈希
-- admin/admin123
UPDATE sys_user SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE user_name = 'admin';
-- 其他用户/123456
UPDATE sys_user SET password = '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2' WHERE user_name != 'admin';

-- 6. 角色数据
INSERT INTO sys_role (id, role_name, role_key, role_sort, data_scope, status, create_time, update_time, remark) VALUES
(1, '超级管理员', 'ADMIN', 1, '1', '0', NOW(), NOW(), '拥有所有权限'),
(2, '医生', 'DOCTOR', 2, '2', '0', NOW(), NOW(), '可管理会员、预约、评估'),
(3, '护士', 'NURSE', 3, '3', '0', NOW(), NOW(), '可执行评估、干预'),
(4, '管理员', 'MANAGER', 4, '1', '0', NOW(), NOW(), '系统管理')
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name), role_key=VALUES(role_key);

-- 7. 用户-角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3), (4, 4)
ON DUPLICATE KEY UPDATE user_id=user_id;

-- 8. ADMIN 拥有所有菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu
ON DUPLICATE KEY UPDATE role_id=role_id;

-- 9. 示例通知
INSERT INTO sys_notice (user_id, title, content, notice_type, is_read) VALUES
(0, '欢迎使用传智健康管理系统', '本系统已升级实时通知能力。', 'SYSTEM', 0),
(1, '体检报告待审核', '会员赵建国的体检报告已生成。', 'ASSESSMENT', 0),
(1, '用药提醒', '会员李秀英今日14:00需服用硝苯地平。', 'MEDICATION', 0);
