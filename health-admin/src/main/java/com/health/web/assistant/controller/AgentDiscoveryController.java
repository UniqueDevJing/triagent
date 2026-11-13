package com.health.web.assistant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.health.web.assistant.a2a.AgentCardRegistry;
import com.health.web.assistant.a2a.AgentHandoff;
import com.health.web.assistant.a2a.HandoffGate;
import com.health.web.assistant.a2a.HandoffLedger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A2A 能力发现与交接审计端点。
 *
 * <p>{@code /.well-known/agent.json} 对齐 Agent2Agent 的 AgentCard 发现约定：
 * 只暴露静态能力自述（技能、工具白名单、协议版本），不含任何会话/患者数据，
 * 因此在 Sa-Token 白名单里公开是安全的。带会话数据的交接台账查询仍需登录。
 */
@Tag(name = "A2A 多智能体能力发现")
@RestController
public class AgentDiscoveryController {

    private final AgentCardRegistry cards;
    private final HandoffLedger ledger;

    public AgentDiscoveryController(AgentCardRegistry cards, HandoffLedger ledger) {
        this.cards = cards;
        this.ledger = ledger;
    }

    @Operation(summary = "A2A AgentCard 发现文档（静态能力自述，公开）")
    @GetMapping("/.well-known/agent.json")
    public Map<String, Object> agentJson() {
        return cards.discoveryDocument();
    }

    @Operation(summary = "交接评估门规则与统计（需登录）")
    @SaCheckLogin
    @GetMapping("/api/v1/assistant/handoff-rules")
    public Map<String, Object> handoffRules() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rules", List.of(
                HandoffGate.RULE_CONTEXT_REF, HandoffGate.RULE_TARGET_EXISTS,
                HandoffGate.RULE_SKILL_MATCH, HandoffGate.RULE_CAPABILITY_COVER,
                HandoffGate.RULE_EVIDENCE_READY, HandoffGate.RULE_MEDICAL_SAFETY,
                HandoffGate.RULE_ACCEPTANCE));
        out.put("total", ledger.size());
        out.put("accepted", ledger.accepted());
        out.put("rejected", ledger.rejected());
        return out;
    }

    @Operation(summary = "按会话回放 A2A 交接链（需登录）")
    @SaCheckLogin
    @GetMapping("/api/v1/assistant/handoff-log")
    public Map<String, Object> handoffLog(@RequestParam String sessionId) {
        List<Map<String, Object>> chain = ledger.replay(sessionId).stream()
                .map(AgentHandoff::toMap).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sessionId", sessionId);
        out.put("handoffs", chain);
        return out;
    }
}
