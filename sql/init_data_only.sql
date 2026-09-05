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

-- 用户-角色关联（admin → 超级管理员）
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
