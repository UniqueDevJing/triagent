package com.health.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.health.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 预约预订单（幂等预占）。
 * Agent 发起预约先落 PENDING 预订单，用户确认后再转正式预约；
 * idempotency_key 唯一索引兜底，保证同一预约意图只产生一单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_preorder")
public class AgentPreOrder extends BaseEntity {

    /** 操作人 sys_user.id */
    private Long userId;

    /** 关联会员 member.id（可为空，人工确认时补） */
    private Long memberId;

    /** 拟就诊科室 */
    private String department;

    /** 拟就诊日期 */
    private LocalDate appointmentDate;

    /** 症状/主诉摘要（幂等输入） */
    private String symptomSummary;

    /** 幂等键 sha256(userId|memberId|department|date) */
    private String idempotencyKey;

    /** PENDING / CONFIRMED / CANCELLED / EXPIRED */
    private String status;

    /** 确认时间 */
    private LocalDateTime confirmTime;
}
