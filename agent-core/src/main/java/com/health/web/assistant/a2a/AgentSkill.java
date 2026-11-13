package com.health.web.assistant.a2a;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A2A AgentCard 中的一项技能（skill）。
 *
 * <p>按 Agent2Agent 协议的语义建模：每个 Agent 对外声明「我能接什么任务」，
 * 而不是让调用方靠硬编码 if-else 猜测能力。{@code id} 是交接契约里的意图标识，
 * Supervisor 下发的 {@code intent} 必须命中目标 Agent 的某个 skill id。
 */
public record AgentSkill(
        String id,
        String name,
        String description,
        List<String> tags,
        List<String> examples) {

    public AgentSkill {
        tags = tags == null ? List.of() : List.copyOf(tags);
        examples = examples == null ? List.of() : List.copyOf(examples);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("description", description);
        m.put("tags", tags);
        m.put("examples", examples);
        return m;
    }
}
