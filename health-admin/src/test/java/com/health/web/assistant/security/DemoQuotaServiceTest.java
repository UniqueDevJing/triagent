package com.health.web.assistant.security;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T8 演示额度守卫测试：上限、跨日重置、用户隔离、手动清零。
 */
class DemoQuotaServiceTest {

    @Test
    void withinLimitAllowed() {
        DemoQuotaService quota = new DemoQuotaService(3);
        assertTrue(quota.tryAcquire(1L));
        assertTrue(quota.tryAcquire(1L));
        assertTrue(quota.tryAcquire(1L));
        assertEquals(0, quota.remaining(1L));
    }

    @Test
    void exceedingLimitRejected() {
        DemoQuotaService quota = new DemoQuotaService(2);
        assertTrue(quota.tryAcquire(1L));
        assertTrue(quota.tryAcquire(1L));
        assertFalse(quota.tryAcquire(1L));
        assertFalse(quota.tryAcquire(1L), "超限后继续拒绝");
        assertEquals(2, quota.used(1L));
    }

    @Test
    void dateRolloverResetsQuota() {
        LocalDate day1 = LocalDate.of(2026, 1, 1);
        LocalDate day2 = LocalDate.of(2026, 1, 2);
        AtomicInteger day = new AtomicInteger(0);
        DemoQuotaService quota = new DemoQuotaService(1,
                () -> day.get() == 0 ? day1 : day2);
        assertTrue(quota.tryAcquire(1L));
        assertFalse(quota.tryAcquire(1L), "同日第二次拒绝");
        day.set(1);
        assertTrue(quota.tryAcquire(1L), "跨日自动重置");
        assertEquals(1, quota.used(1L));
    }

    @Test
    void usersAreIndependent() {
        DemoQuotaService quota = new DemoQuotaService(1);
        assertTrue(quota.tryAcquire(1L));
        assertFalse(quota.tryAcquire(1L));
        assertTrue(quota.tryAcquire(2L), "不同账号额度互不影响");
    }

    @Test
    void resetAllClearsCounters() {
        DemoQuotaService quota = new DemoQuotaService(1);
        assertTrue(quota.tryAcquire(1L));
        assertFalse(quota.tryAcquire(1L));
        quota.resetAll();
        assertTrue(quota.tryAcquire(1L), "手动重置后额度恢复");
    }

    @Test
    void usedIsZeroBeforeAnyRequest() {
        DemoQuotaService quota = new DemoQuotaService(5);
        assertEquals(0, quota.used(9L));
        assertEquals(5, quota.remaining(9L));
    }
}
