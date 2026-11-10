package com.health.web.assistant.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T8 演示访问决策矩阵测试。
 * 验收核心：公开演示账号不能执行高风险管理操作。
 */
class DemoAccessGuardTest {

    private static final long DEMO = 100L;
    private static final long NORMAL = 1L;

    private DemoAccessGuard newGuard(int limit) {
        return new DemoAccessGuard(java.util.Set.of(DEMO), new DemoQuotaService(limit));
    }

    @Test
    void demoUserReadAllowed() {
        DemoAccessGuard guard = newGuard(2);
        assertEquals(DemoAccessGuard.Verdict.ALLOW,
                guard.check(DEMO, "GET", "/api/v1/system/user"));
        assertEquals(DemoAccessGuard.Verdict.ALLOW,
                guard.check(DEMO, "GET", "/api/v1/assistant/metrics"));
    }

    @Test
    void demoUserChatAllowedWithinQuota() {
        DemoAccessGuard guard = newGuard(2);
        assertEquals(DemoAccessGuard.Verdict.ALLOW,
                guard.check(DEMO, "POST", "/api/v1/assistant/chat"));
        assertEquals(DemoAccessGuard.Verdict.ALLOW,
                guard.check(DEMO, "POST", "/api/v1/assistant/chat"));
        assertEquals(DemoAccessGuard.Verdict.QUOTA_EXCEEDED,
                guard.check(DEMO, "POST", "/api/v1/assistant/chat"));
    }

    @Test
    void demoUserManagementWriteDenied() {
        DemoAccessGuard guard = newGuard(2);
        assertEquals(DemoAccessGuard.Verdict.DENY,
                guard.check(DEMO, "POST", "/api/v1/system/user"));
        assertEquals(DemoAccessGuard.Verdict.DENY,
                guard.check(DEMO, "POST", "/api/v1/system/role"));
    }

    @Test
    void demoUserPreOrderConfirmDenied() {
        DemoAccessGuard guard = newGuard(2);
        assertEquals(DemoAccessGuard.Verdict.DENY,
                guard.check(DEMO, "POST", "/api/v1/assistant/preorders/5/confirm"));
    }

    @Test
    void demoUserDeleteAndPutDenied() {
        DemoAccessGuard guard = newGuard(2);
        assertEquals(DemoAccessGuard.Verdict.DENY,
                guard.check(DEMO, "DELETE", "/api/v1/knowledge/article/1"));
        assertEquals(DemoAccessGuard.Verdict.DENY,
                guard.check(DEMO, "PUT", "/api/v1/member/1"));
    }

    @Test
    void demoUserLogoutAllowed() {
        DemoAccessGuard guard = newGuard(2);
        assertEquals(DemoAccessGuard.Verdict.ALLOW,
                guard.check(DEMO, "POST", "/api/v1/logout"));
    }

    @Test
    void normalUserUnrestricted() {
        DemoAccessGuard guard = newGuard(1);
        assertEquals(DemoAccessGuard.Verdict.ALLOW,
                guard.check(NORMAL, "POST", "/api/v1/system/user"));
        assertEquals(DemoAccessGuard.Verdict.ALLOW,
                guard.check(NORMAL, "POST", "/api/v1/assistant/chat"));
        assertTrue(guard.quota().used(NORMAL) == 0, "非演示账号不消耗演示额度");
    }

    @Test
    void deniedWriteDoesNotConsumeQuota() {
        DemoAccessGuard guard = newGuard(1);
        assertEquals(DemoAccessGuard.Verdict.DENY,
                guard.check(DEMO, "POST", "/api/v1/system/user"));
        assertEquals(DemoAccessGuard.Verdict.ALLOW,
                guard.check(DEMO, "POST", "/api/v1/assistant/chat"), "被拒请求不占聊天额度");
    }

    @Test
    void demoUserIdentification() {
        DemoAccessGuard guard = newGuard(2);
        assertTrue(guard.isDemoUser(DEMO));
        assertFalse(guard.isDemoUser(NORMAL));
    }
}
