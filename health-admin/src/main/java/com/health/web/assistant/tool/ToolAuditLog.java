package com.health.web.assistant.tool;

import com.health.web.assistant.agent.AgentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 工具越权审计日志（T2）：记录每次工具放行/拦截决策，供治理与单测覆盖。
 * 字段：ts / sessionId / userId / role / tool / allowed（是否放行）。
 * 内存滚动窗口（默认 1000 条），零外部依赖；生产可接日志采集或审计库。
 */
@Component
public class ToolAuditLog {

    /** 滚动窗口容量 */
    private static final int WINDOW = 1000;

    /** 单条审计记录 */
    public record Entry(long ts, String sessionId, Long userId, String role, String tool, boolean allowed) {
    }

    private final Deque<Entry> recent = new ConcurrentLinkedDeque<>();

    /** 记录一次工具放行/拦截决策 */
    public void record(String sessionId, Long userId, AgentType role, String tool, boolean allowed) {
        recent.addLast(new Entry(System.currentTimeMillis(), sessionId, userId,
                role == null ? "-" : role.name(), tool, allowed));
        while (recent.size() > WINDOW) {
            recent.pollFirst();
        }
    }

    public List<Entry> recent() {
        return new ArrayList<>(recent);
    }

    public int size() {
        return recent.size();
    }

    /** 越权拦截率 = 被拒决策数 / 总决策数（0~1） */
    public double denialRate() {
        if (recent.isEmpty()) {
            return 0.0;
        }
        long denied = 0;
        for (Entry e : recent) {
            if (!e.allowed()) {
                denied++;
            }
        }
        return (double) denied / recent.size();
    }

    /**
     * 审计覆盖率 = 已记录的工具名集合 / 期望覆盖的工具名集合（T2 单测指标）。
     * 期望集合缺省为四类工具；也可由调用方显式传入。
     */
    public double coverage(Set<String> expectedTools) {
        if (expectedTools == null || expectedTools.isEmpty()) {
            return recent.isEmpty() ? 0.0 : 1.0;
        }
        Set<String> seen = new java.util.HashSet<>();
        for (Entry e : recent) {
            seen.add(e.tool());
        }
        long covered = 0;
        for (String t : expectedTools) {
            if (seen.contains(t)) {
                covered++;
            }
        }
        return (double) covered / expectedTools.size();
    }

    /** 清空（单测用） */
    public void clear() {
        recent.clear();
    }
}
