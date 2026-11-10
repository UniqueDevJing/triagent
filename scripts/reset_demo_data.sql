-- ============================================
-- T8 演示环境安全化：数据重置
-- 用途：清空演示账号 visitor 产生的业务数据，恢复演示初始状态。
--       演示账号为只读，唯一写入来源是 assistant/chat 触发的
--       预订单预占与评估记录，因此重置只涉及这两张表。
-- 使用：mysql -u root -p health_management < scripts/reset_demo_data.sql
-- ============================================

USE health_management;

-- 1. 清空演示账号的 AI 预订单（预占 / 已确认）
DELETE ap FROM agent_preorder ap
JOIN sys_user u ON u.user_name = 'visitor' AND ap.user_id = u.id;

-- 2. 清空演示账号触发的评估记录（assessor_id 记录操作人）
DELETE ar FROM assessment_record ar
JOIN sys_user u ON u.user_name = 'visitor' AND ar.assessor_id = u.id;

-- 3. Redis 侧（可选，手动执行）：
--    幂等键、会话记忆与决策日志均在内存/Redis，
--    重启后端容器即可一并清空：docker compose restart backend

SELECT '演示数据重置完成' AS result;
