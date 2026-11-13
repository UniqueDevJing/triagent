package com.health.web.assistant.a2a;

/**
 * A2A 交接任务状态机。
 *
 * <p>协议里 Task 有明确生命周期；这里把 Supervisor → 子 Agent 的 handoff
 * 从「一句字符串下发」升级为可校验、可留痕、可回放的状态流转。
 * 只允许下列转移，任何非法跳跃都会被 {@link AgentHandoff#transition} 拒绝，
 * 避免出现「未评估就完成」这类无法审计的假状态。
 */
public enum HandoffStatus {
    CREATED,
    VALIDATED,
    DISPATCHED,
    FULFILLED,
    REJECTED;

    public boolean canTransitionTo(HandoffStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case CREATED -> next == VALIDATED || next == REJECTED;
            case VALIDATED -> next == DISPATCHED || next == REJECTED;
            case DISPATCHED -> next == FULFILLED || next == REJECTED;
            case FULFILLED, REJECTED -> false;
        };
    }
}
