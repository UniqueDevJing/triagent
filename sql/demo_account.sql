-- ============================================
-- T8 演示环境安全化：只读演示账号
-- 用途：创建公开演示账号 visitor（非管理员角色），
--       配合 application.yml 中 triagent.demo.enabled=true
--       启用 DemoWebSecurityConfig 只读拦截与聊天额度。
-- 使用：mysql -u root -p health_management < sql/demo_account.sql
-- ============================================

USE health_management;

-- 1. 演示角色（无任何管理菜单权限）
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, status, remark)
VALUES ('演示访客', 'visitor', 99, '1', '0', '公开演示只读账号，禁止管理操作')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 2. 演示账号（密码与种子数据一致：admin123，生产环境务必更换）
INSERT INTO sys_user (user_name, nick_name, password, email, phone_number, sex, status, dept_id)
VALUES ('visitor', '演示访客', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
        'visitor@health.com', '', '0', '0', 1)
ON DUPLICATE KEY UPDATE nick_name = VALUES(nick_name);

-- 3. 绑定角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.user_name = 'visitor' AND r.role_key = 'visitor'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

SELECT '演示账号 visitor 创建完成' AS result;
