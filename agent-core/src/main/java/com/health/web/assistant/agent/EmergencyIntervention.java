package com.health.web.assistant.agent;

import com.health.web.assistant.memory.SessionFactStore;
import com.health.web.assistant.model.AgentResponse;
import com.health.web.assistant.model.SseEvent;
import com.health.web.assistant.model.TriageResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 急诊协作拦截话术（T1 从 AssistantOrchestrator 拆出的策略组件）。
 *
 * <p>预约管家落单前发现本会话已有 EMERGENCY 分诊事实：确定性返回（不消耗 LLM），
 * 告知先急诊并引导澄清放行。填充 {@link AgentResponse} 供编排层收尾留痕。
 */
@Component
public class EmergencyIntervention {

    /** 构建拦截响应：填充 response，返回 DONE 事件（含建议科室与放行引导） */
    public SseEvent blockedSchedulerEvent(SessionFactStore.EmergencyFact ef,
                                          List<Map<String, Object>> sources,
                                          AgentResponse resp) {
        TriageResult tri = new TriageResult();
        tri.setUrgency("EMERGENCY");
        tri.setDepartments(ef.departments());
        tri.setHospitalLevel("立即前往就近急诊或拨打 120");
        tri.setConfidence(0.9);
        tri.setFollowUp(List.of(
                "立即前往就近急诊或拨打 120，不要等待门诊预约",
                "急诊处理后，如有复查需要可再回来预约对应专科门诊"));
        tri.setDisclaimer(ef.disclaimer());

        String depts = String.join("/", ef.departments() == null ? List.of("急诊科") : ef.departments());
        String answer = "系统已暂停本次门诊预约预占。本会话此前分诊结论为【需立即急诊】（建议科室：" + depts + "）。"
                + "\n\n为你的安全考虑，预约管家不能代你预占门诊——请先前往急诊评估。"
                + "\n\n如果你已完成急诊评估、确认并非急症，请回复「已排除 / 看过急诊」，即可恢复预约。"
                + "\n\n" + (ef.disclaimer() == null ? "" : ef.disclaimer());

        resp.setFullText(answer);
        resp.setTriage(tri);
        resp.setTurnType("EMERGENCY");
        return SseEvent.done(answer, tri, "预约管家", sources);
    }
}
