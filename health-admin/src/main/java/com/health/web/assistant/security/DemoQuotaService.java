package com.health.web.assistant.security;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 演示账号每日聊天额度（T8）。
 * 纯内存实现，按 自然日 滚动重置；只对演示账号生效，调用方见 DemoAccessGuard。
 */
public class DemoQuotaService {

    /** 每日上限 */
    private final int dailyLimit;

    /** 当前日期供应器，测试可注入固定日期验证跨日重置 */
    private final Supplier<LocalDate> today;

    private static final class DayCounter {
        final LocalDate date;
        final AtomicInteger used;

        DayCounter(LocalDate date) {
            this.date = date;
            this.used = new AtomicInteger();
        }
    }

    private final Map<Long, DayCounter> counters = new ConcurrentHashMap<>();

    public DemoQuotaService(int dailyLimit) {
        this(dailyLimit, LocalDate::now);
    }

    DemoQuotaService(int dailyLimit, Supplier<LocalDate> today) {
        this.dailyLimit = dailyLimit;
        this.today = today;
    }

    /**
     * 尝试消耗一次聊天额度。成功返回 true；当日已满或达到上限返回 false。
     * 被拒绝的请求不累计计数，used 始终表示已实际消耗的额度。
     */
    public boolean tryAcquire(Long userId) {
        DayCounter counter = counters.compute(userId, (id, existing) -> {
            LocalDate now = today.get();
            if (existing == null || !existing.date.equals(now)) {
                return new DayCounter(now);
            }
            return existing;
        });
        while (true) {
            int current = counter.used.get();
            if (current >= dailyLimit) {
                return false;
            }
            if (counter.used.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    /** 当日已使用次数（诊断用） */
    public int used(Long userId) {
        DayCounter counter = counters.get(userId);
        LocalDate now = today.get();
        if (counter == null || !counter.date.equals(now)) {
            return 0;
        }
        return counter.used.get();
    }

    /** 剩余额度，永不小于 0 */
    public int remaining(Long userId) {
        return Math.max(0, dailyLimit - used(userId));
    }

    /** 手动清空全部计数，运维重置用 */
    public void resetAll() {
        counters.clear();
    }
}
