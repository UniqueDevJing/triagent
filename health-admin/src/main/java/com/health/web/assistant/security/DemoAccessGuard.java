package com.health.web.assistant.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 演示账号访问决策核心（T8）。
 *
 * 规则：
 * - 非演示账号：一律放行，由既有 Sa-Token 角色体系管控。
 * - 演示账号：只放行 GET/HEAD 只读请求、白名单写路径
 *   （assistant/chat 聊天演示 + logout），其余 POST/PUT/DELETE 全部拒绝。
 * - chat 是唯一放行的写路径，但受每日额度约束（DemoQuotaService）。
 *
 * 纯逻辑类，不依赖 Servlet API，便于单元测试覆盖决策矩阵。
 */
public class DemoAccessGuard {

    public enum Verdict {
        /** 放行 */
        ALLOW,
        /** 只读账号禁止的写操作 */
        DENY,
        /** 聊天额度已用尽 */
        QUOTA_EXCEEDED
    }

    /** 演示账号放行的写路径白名单：聊天演示与登出 */
    static final Set<String> WRITE_WHITELIST = Set.of(
            "/api/v1/assistant/chat",
            "/api/v1/logout"
    );

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final Set<Long> demoUserIds;
    private final DemoQuotaService quota;

    public DemoAccessGuard(Collection<Long> demoUserIds, DemoQuotaService quota) {
        this.demoUserIds = new HashSet<>(demoUserIds);
        this.quota = quota;
    }

    public boolean isDemoUser(Long userId) {
        return demoUserIds.contains(userId);
    }

    /**
     * 访问决策。userId 为 Sa-Token 登录 id；method/path 来自当前请求。
     */
    public Verdict check(Long userId, String method, String path) {
        if (!isDemoUser(userId)) {
            return Verdict.ALLOW;
        }
        if (READ_METHODS.contains(method.toUpperCase())) {
            return Verdict.ALLOW;
        }
        if (WRITE_WHITELIST.contains(path)) {
            if ("/api/v1/assistant/chat".equals(path)) {
                return quota.tryAcquire(userId) ? Verdict.ALLOW : Verdict.QUOTA_EXCEEDED;
            }
            return Verdict.ALLOW;
        }
        return Verdict.DENY;
    }

    public DemoQuotaService quota() {
        return quota;
    }
}
