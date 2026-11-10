package com.health.web.assistant.agent;

import com.health.web.assistant.memory.SessionFactStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Supervisor 安全规划器（T1 从 AssistantOrchestrator 拆出的协作组件）。
 *
 * <p>两个确定性决策（医疗安全优先，0 LLM 消耗）：
 * <ol>
 *   <li><b>组合请求规划</b>：同一句话里既预约又报告当前急症红旗（如「胸痛…帮我约…」）
 *       → 本轮应先走 TRIAGE，由红旗状态机确定性输出 EMERGENCY。既往/复诊语境不会触发，避免误伤。</li>
 *   <li><b>预约落单前的协作拦截</b>：会话已有 EMERGENCY 分诊事实 → 返回该事实供编排层拦截；
 *       用户澄清「已排除/看过急诊」且当前消息不再含红旗 → 清除事实放行。</li>
 * </ol>
 */
@Component
public class SupervisorSafetyPlanner {

    private static final Logger log = LoggerFactory.getLogger(SupervisorSafetyPlanner.class);

    private final SymptomStateMachine stateMachine;
    private final ConversationRouter router;
    private final SessionFactStore factStore;

    public SupervisorSafetyPlanner(SymptomStateMachine stateMachine,
                                   ConversationRouter router,
                                   SessionFactStore factStore) {
        this.stateMachine = stateMachine;
        this.router = router;
        this.factStore = factStore;
    }

    /**
     * 预约请求内含当前急症红旗 → 应急分诊优先（跳过预约分支）。
     * 关键防误伤：既往/复诊语境（以前胸痛过、复查）不触发。
     */
    public boolean shouldTriageFirst(String sessionId, String message) {
        if (stateMachine.emergencyOverrideClearsFact(message)
                || factStore.getEmergency(sessionId) != null) {
            return false;
        }
        List<String> reds = stateMachine.redFlagsIn(message);
        boolean hit = !reds.isEmpty() && !router.hasPastTense(message);
        if (hit) {
            log.info("[supervisor] 会话={} 规划决策：预约请求内含急症红旗 {} → 应急分诊优先（跳过预约分支）",
                    sessionId, reds);
        }
        return hit;
    }

    /**
     * 预约落单前的协作拦截检查。
     *
     * @return 需要拦截的 {@link SessionFactStore.EmergencyFact}；null = 放行
     *         （含用户澄清后清除既有事实的情形）。
     */
    public SessionFactStore.EmergencyFact gateScheduler(String sessionId, String message) {
        SessionFactStore.EmergencyFact ef = factStore.getEmergency(sessionId);
        if (stateMachine.emergencyOverrideClearsFact(message)) {
            if (ef != null) {
                factStore.clearEmergency(sessionId);
                log.info("[agent] 会话={} 用户澄清急诊已排除，恢复预约流程", sessionId);
            }
            return null;
        }
        return ef;
    }
}
