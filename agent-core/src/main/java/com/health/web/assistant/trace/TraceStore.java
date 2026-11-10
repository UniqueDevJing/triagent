package com.health.web.assistant.trace;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * T6 Trace 存储（会话粒度内存环形缓冲）：
 * <ul>
 *   <li>每个会话保留最近 {@value #MAX_PER_SESSION} 条请求 trace；</li>
 *   <li>{@link #active(String)} 返回会话当前未完成的 trace——供 ToolGateway 在任意线程
 *       （reactor 流式线程）挂工具子 span，不依赖 ThreadLocal；</li>
 *   <li>查询返回已脱敏 JSON，单次请求可还原完整决策链。</li>
 * </ul>
 */
@Component
public class TraceStore {

    static final int MAX_PER_SESSION = 20;
    static final int MAX_SESSIONS = 2000;

    private final Map<String, Deque<RequestTrace>> bySession = new ConcurrentHashMap<>();
    private final Map<String, RequestTrace> activeBySession = new ConcurrentHashMap<>();

    /** 请求开始：创建并绑定为会话活跃 trace */
    public RequestTrace begin(String sessionId) {
        RequestTrace trace = new RequestTrace(sessionId);
        activeBySession.put(sessionId, trace);
        Deque<RequestTrace> deque = bySession.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(trace);
            while (deque.size() > MAX_PER_SESSION) {
                deque.removeFirst();
            }
        }
        // 防会话数无限膨胀
        if (bySession.size() > MAX_SESSIONS) {
            bySession.clear();
            activeBySession.clear();
        }
        return trace;
    }

    /** 会话当前活跃（未完成）trace；无则返回 null */
    public RequestTrace active(String sessionId) {
        return activeBySession.get(sessionId);
    }

    /** 请求结束：收口全部 span 并解除活跃绑定 */
    public void finish(RequestTrace trace) {
        trace.finish();
        activeBySession.remove(trace.sessionId(), trace);
    }

    /** 查询会话最近的 traces（JSON 数组，已脱敏） */
    public String recentJson(String sessionId, int limit) {
        Deque<RequestTrace> deque = bySession.get(sessionId);
        if (deque == null) {
            return "[]";
        }
        List<RequestTrace> snapshot;
        synchronized (deque) {
            snapshot = new ArrayList<>(deque);
        }
        int from = Math.max(0, snapshot.size() - Math.max(1, limit));
        StringBuilder sb = new StringBuilder(1024);
        sb.append('[');
        for (int i = from; i < snapshot.size(); i++) {
            if (i > from) {
                sb.append(',');
            }
            sb.append(snapshot.get(i).toJson());
        }
        sb.append(']');
        return sb.toString();
    }

    /** 会话 trace 总数（观测用） */
    public int count(String sessionId) {
        Deque<RequestTrace> deque = bySession.get(sessionId);
        return deque == null ? 0 : deque.size();
    }
}
