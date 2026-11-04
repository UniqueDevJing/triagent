package com.health.web.assistant.tool;

import com.health.web.assistant.agent.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 工具越权审计日志（T2）：记录每次工具放行/拦截决策，供治理与单测覆盖。
 * 字段：ts / sessionId / userId / role / tool / allowed（是否放行）。
 * 内存滚动窗口（默认 1000 条）供实时看板；另可按需双写本地 JSONL 落盘（见 {@code assistant.tool-audit-log}），
 * 满足医疗场景「越权审计可查历史」的诉求（修复协同评审 P0-2）。
 */
@Component
public class ToolAuditLog {

    private static final Logger log = LoggerFactory.getLogger(ToolAuditLog.class);

    /** 滚动窗口容量 */
    private static final int WINDOW = 1000;

    /** 单条审计记录 */
    public record Entry(long ts, String sessionId, Long userId, String role, String tool, boolean allowed) {
    }

    private final Deque<Entry> recent = new ConcurrentLinkedDeque<>();

    /** 审计日志落盘路径（配置项 assistant.tool-audit-log；为空则不落盘） */
    @Value("${assistant.tool-audit-log:}")
    private String logPath;

    private volatile Path logFile;
    private final Object logLock = new Object();

    /** 记录一次工具放行/拦截决策（同时按需落盘 JSONL） */
    public void record(String sessionId, Long userId, AgentType role, String tool, boolean allowed) {
        recent.addLast(new Entry(System.currentTimeMillis(), sessionId, userId,
                role == null ? "-" : role.name(), tool, allowed));
        while (recent.size() > WINDOW) {
            recent.pollFirst();
        }
        appendToDisk(sessionId, userId, role, tool, allowed);
    }

    private void appendToDisk(String sessionId, Long userId, AgentType role, String tool, boolean allowed) {
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
                Files.writeString(logFile,
                        toJsonLine(System.currentTimeMillis(), sessionId, userId,
                                role == null ? "-" : role.name(), tool, allowed) + "\n",
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ex) {
                log.warn("[audit] 审计日志落盘失败（忽略，不影响主流程）: {}", ex.getMessage());
            }
        }
    }

    private static String toJsonLine(long ts, String sessionId, Long userId, String role,
                                     String tool, boolean allowed) {
        return "{\"ts\":" + ts
                + ",\"sessionId\":" + q(sessionId)
                + ",\"userId\":" + (userId == null ? "null" : userId)
                + ",\"role\":" + q(role)
                + ",\"tool\":" + q(tool)
                + ",\"allowed\":" + allowed + "}";
    }

    private static String q(String s) {
        if (s == null) {
            return "\"\"";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public List<Entry> recent() {
        return new ArrayList<>(recent);
    }

    /**
     * 审计查询：最近 limit 条；sessionId 非空时按会话过滤。
     * 供只读接口 /api/v1/assistant/tool-audit 使用（协同评审 P0-2）。
     */
    public List<Entry> query(String sessionId, int limit) {
        List<Entry> all = new ArrayList<>(recent);
        List<Entry> filtered = (sessionId == null || sessionId.isBlank())
                ? all
                : all.stream().filter(e -> sessionId.equals(e.sessionId())).toList();
        int lim = Math.max(1, limit);
        int from = Math.max(0, filtered.size() - lim);
        return new ArrayList<>(filtered.subList(from, filtered.size()));
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
