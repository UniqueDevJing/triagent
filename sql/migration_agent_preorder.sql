-- ============================================================
-- Phase 2: AI 预约预订单（幂等）
-- 说明：Agent 发起预约先落 PENDING 预订单，用户确认后再转正式预约；
--       idempotency_key 唯一索引兜底，保证同一预约意图只产生一单。
-- 可重复执行（IF NOT EXISTS）。
-- ============================================================

CREATE TABLE IF NOT EXISTS agent_preorder (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预订单ID',
    user_id         BIGINT       NOT NULL                COMMENT '操作人 sys_user.id',
    member_id       BIGINT       NULL                    COMMENT '关联会员 member.id（可为空，人工确认时补）',
    department      VARCHAR(64)  NOT NULL                COMMENT '拟就诊科室',
    appointment_date DATE        NOT NULL                COMMENT '拟就诊日期',
    symptom_summary VARCHAR(500) NULL                    COMMENT '症状/主诉摘要（幂等输入，便于人工核对）',
    idempotency_key VARCHAR(64)  NOT NULL                COMMENT '幂等键 sha256(userId|memberId|department|date)',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING'
                    COMMENT 'PENDING=待确认 CONFIRMED=已确认 CANCELLED=已取消 EXPIRED=已过期',
    confirm_time    DATETIME     NULL                    COMMENT '确认时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    KEY idx_user_status (user_id, status),
    KEY idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 预约预订单（幂等预占）';
