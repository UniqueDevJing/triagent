package com.health.web.assistant.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 可观测指标（Phase4）：内存滚动窗口（默认最近 500 轮）+ 聚合统计。
 * 每轮在 AuditAdvice.after 落一条 TurnMetric；summary() 供管理端查询。
 * 生产可替换为时序库/日志采集（如按窗口定时刷出），此处保持零依赖、可观测闭环。
 */
@Service
public class AgentMetricsService {

    /** 滚动窗口容量 */
    private static final int WINDOW = 500;

    /** 单轮指标 */
    public record TurnMetric(
            long ts,
            String sessionId,
            String agent,
            String turnType,
            String urgency,
            long elapsedMs,
            int toolCalls,
            int toolResults,
            int sources,
            String blockReason) {
    }

    private final Deque<TurnMetric> recent = new ArrayDeque<>(WINDOW + 1);

    public synchronized void record(TurnMetric metric) {
        recent.addLast(metric);
        while (recent.size() > WINDOW) {
            recent.removeFirst();
        }
    }

    /** 聚合快照（窗口内重算，O(n)，管理端低频调用足够） */
    public synchronized Map<String, Object> summary() {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Long> byAgent = new LinkedHashMap<>();
        Map<String, Long> byUrgency = new LinkedHashMap<>();
        Map<String, Long> byTurnType = new LinkedHashMap<>();
        long totalMs = 0;
        int toolCalls = 0;
        int toolResults = 0;
        int blocked = 0;
        List<Map<String, Object>> recentList = new ArrayList<>();

        for (TurnMetric m : recent) {
            totalMs += m.elapsedMs();
            toolCalls += m.toolCalls();
            toolResults += m.toolResults();
            if (m.blockReason() != null && !m.blockReason().isBlank()) {
                blocked++;
            }
            byAgent.merge(nz(m.agent()), 1L, Long::sum);
            byUrgency.merge(nz(m.urgency()), 1L, Long::sum);
            byTurnType.merge(nz(m.turnType()), 1L, Long::sum);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ts", m.ts());
            row.put("sessionId", m.sessionId());
            row.put("agent", m.agent());
            row.put("turnType", m.turnType());
            row.put("urgency", m.urgency());
            row.put("elapsedMs", m.elapsedMs());
            row.put("toolCalls", m.toolCalls());
            row.put("toolResults", m.toolResults());
            row.put("sources", m.sources());
            row.put("blockReason", m.blockReason());
            recentList.add(row);
        }

        out.put("total", recent.size());
        out.put("avgMs", recent.isEmpty() ? 0 : Math.round(totalMs * 10.0 / recent.size()) / 10.0);
        out.put("toolCalls", toolCalls);
        out.put("toolResults", toolResults);
        out.put("blocked", blocked);
        out.put("byAgent", byAgent);
        out.put("byUrgency", byUrgency);
        out.put("byTurnType", byTurnType);
        out.put("recent", recentList);
        return out;
    }

    private static String nz(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }
}
