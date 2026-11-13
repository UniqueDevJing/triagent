package com.health.web.assistant.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 决策日志（T3）：记录每次 Agent 决策的输入特征 / 规则命中 / LLM 输出 / 最终裁决，
 * 支持「同一会话重放」（{@link #replay(String)}），用于可解释、可审计复盘。
 *
 * 内存环形窗口（默认 2000 条）供实时看板；另可按需双写本地 JSONL 落盘
 * （配置项 {@code assistant.decision-log}），满足医疗「决策可溯源」诉求。
 */
@Component
public class DecisionLog {

    private static final Logger log = LoggerFactory.getLogger(DecisionLog.class);
    private static final int WINDOW = 2000;

    private final ConcurrentLinkedDeque<DecisionRecord> recent = new ConcurrentLinkedDeque<>();
    private final Map<String, List<DecisionRecord>> bySession = new ConcurrentHashMap<>();

    /** 决策日志落盘路径（配置项 assistant.decision-log；为空则不落盘） */
    @Value("${assistant.decision-log:}")
    private String logPath;

    private volatile Path logFile;
    private final Object logLock = new Object();

    /** 记录一次决策（同时写入内存窗口、按会话索引、按需落盘 JSONL） */
    public void record(DecisionRecord r) {
        if (r == null) {
            return;
        }
        recent.addLast(r);
        while (recent.size() > WINDOW) {
            recent.pollFirst();
        }
        bySession.computeIfAbsent(r.sessionId(), k -> new CopyOnWriteArrayList<>()).add(r);
        appendToDisk(r);
    }

    /** 重放某会话的完整决策链（按发生顺序） */
    public List<DecisionRecord> replay(String sessionId) {
        List<DecisionRecord> l = bySession.get(sessionId);
        return l == null ? List.of() : List.copyOf(l);
    }

    /** 全量（内存窗口） */
    public List<DecisionRecord> all() {
        return new ArrayList<>(recent);
    }

    /** 审计查询：最近 limit 条；sessionId 非空时按会话过滤 */
    public List<DecisionRecord> query(String sessionId, int limit) {
        List<DecisionRecord> src = (sessionId == null || sessionId.isBlank()) ? all() : replay(sessionId);
        int lim = Math.max(1, limit);
        int from = Math.max(0, src.size() - lim);
        return new ArrayList<>(src.subList(from, src.size()));
    }

    public int size() {
        return recent.size();
    }

    /** 清空某会话的决策链（也同步从内存窗口移除） */
    public void clear(String sessionId) {
        if (sessionId == null) {
            return;
        }
        bySession.remove(sessionId);
        recent.removeIf(r -> sessionId.equals(r.sessionId()));
    }

    private void appendToDisk(DecisionRecord r) {
        if (logFile == null) {
            if (logPath == null || logPath.isBlank()) {
                return;
            }
            synchronized (logLock) {
                if (logFile == null) {
                    logFile = Paths.get(logPath);
                }
            }
        }
        synchronized (logLock) {
            try {
                Files.writeString(logFile, toJsonLine(r) + "\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ex) {
                log.warn("[decision-log] 落盘失败（忽略，不影响主流程）: {}", ex.getMessage());
            }
        }
    }

    private static String q(String s) {
        if (s == null) {
            return "\"\"";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String toJsonLine(DecisionRecord r) {
        return "{\"ts\":" + r.ts()
                + ",\"sessionId\":" + q(r.sessionId())
                + ",\"agentType\":" + q(r.agentType())
                + ",\"turnType\":" + q(r.turnType())
                + ",\"redFlags\":" + q(r.redFlags())
                + ",\"ruleHits\":" + q(r.ruleHits())
                + ",\"urgency\":" + q(r.urgency())
                + ",\"departments\":" + q(r.departments())
                + ",\"blockedReason\":" + q(r.blockedReason())
                + ",\"elapsedMs\":" + r.elapsedMs()
                + ",\"toolCalls\":" + r.toolCalls()
                + ",\"toolResults\":" + r.toolResults()
                + ",\"message\":" + q(r.message())
                + ",\"finalText\":" + q(r.finalText()) + "}";
    }
}
