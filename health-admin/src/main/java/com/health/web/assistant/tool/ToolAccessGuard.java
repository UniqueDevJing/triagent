package com.health.web.assistant.tool;

import com.health.web.assistant.advisor.AgentBlockedException;
import com.health.web.assistant.agent.AgentType;
import org.springframework.stereotype.Component;

/**
 * 工具越权守卫（T2 加固 / 协同 T4 红队）：在 {@link AgentToolkit} 各 @Tool 方法入口
 * 校验「当前角色是否有权调用该工具」。
 *
 * 设计要点（修复协同评审 P0-1）：
 * 1. 越权判定以声明式 {@link ToolAccessPolicy} 为唯一权威来源；
 * 2. 每次调用（放行或拦截）都向 {@link ToolAuditLog} 记一条决策，使越权拦截成为
 *    「可观测、可审计、可单测」的真实事件，而非仅存在于静态过滤名单里；
 * 3. 越权时抛出 {@link AgentBlockedException}，由编排层 before 链捕获并短路返回。
 *
 * 与 AssistantOrchestrator 的 callback 过滤形成「纵深防御」：过滤把越权工具从模型可见
 * 列表摘除（模型不会去调），守卫则在方法体内兜底——即便过滤被绕过或模型幻觉出工具名，
 * 真实调用仍会被拦截并留痕。红队（T4）可直接打此入口产生真实 deny 记录。
 */
@Component
public class ToolAccessGuard {

    private final ToolAccessPolicy policy;
    private final ToolAuditLog audit;

    public ToolAccessGuard(ToolAccessPolicy policy, ToolAuditLog audit) {
        this.policy = policy;
        this.audit = audit;
    }

    /**
     * 校验当前角色是否有权调用 tool。越权抛 AgentBlockedException，且始终记录审计。
     */
    public void check(AgentType role, String tool, String sessionId, Long userId) {
        boolean allowed = policy.isAllowed(role, tool);
        audit.record(sessionId, userId, role, tool, allowed);
        if (!allowed) {
            throw new AgentBlockedException("tool-not-allowed:" + tool);
        }
    }
}
