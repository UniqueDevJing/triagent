package com.health.web.assistant.service;

import com.health.system.domain.AgentPreOrder;
import com.health.system.mapper.AgentPreOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 不可用时的幂等降级验证。
 *
 * <p>背景：预订单幂等有两层 —— Redis SETNX 预占（加速）与 DB 唯一索引（正确性保证）。
 * 原实现里 Redis 调用没有任何保护，一旦 Redis 连接失败，异常会直接穿透 create()，
 * 造成「Redis 挂 → 预约功能整体不可用」，而第二层 DB 兜底<b>永远走不到</b>，
 * 这正是两层设计失去意义的场景。
 *
 * <p>修复后：Redis 视为加速层而非正确性层，异常时降级为仅依赖 DB 唯一索引，
 * 业务仍可下单，且并发场景依旧被唯一键拦截。
 * 降级约定与 RedisSessionFactStore 保持一致（同一项目内不出现两套 Redis 异常策略）。
 */
@ExtendWith(MockitoExtension.class)
class PreOrderServiceRedisDegradationTest {

    @Mock
    private AgentPreOrderMapper mapper;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;

    private PreOrderService service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        service = new PreOrderService(mapper, redis);
    }

    private AgentPreOrder existing(long id, String key) {
        AgentPreOrder po = new AgentPreOrder();
        po.setId(id);
        po.setUserId(1L);
        po.setMemberId(3L);
        po.setDepartment("呼吸内科");
        po.setStatus("PENDING");
        po.setIdempotencyKey(key);
        return po;
    }

    @Test
    @DisplayName("Redis 预占抛连接异常时仍能正常落单（不被异常中断）")
    void createSucceedsWhenRedisUnavailable() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("Redis 连不上"));

        AgentPreOrder result = service.create(1L, 3L, "呼吸内科", "2026-09-20", "咳嗽三天");

        assertThat(result).isNotNull();
        assertThat(result.getDepartment()).isEqualTo("呼吸内科");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        // 必须真的落库了 —— 这是降级的核心目的
        verify(mapper).insert(any(AgentPreOrder.class));
    }

    @Test
    @DisplayName("Redis 不可用时不回填占位（避免二次异常）")
    void doesNotWriteBackWhenRedisUnavailable() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("Redis 连不上"));

        service.create(1L, 3L, "呼吸内科", "2026-09-20", "咳嗽");

        // 预占失败后不应再尝试回填占位
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Redis 不可用 + DB 唯一键冲突 → 仍返回已有单（并发幂等不失效）")
    void dbUniqueIndexStillGuardsWhenRedisDown() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("Redis 连不上"));

        String key = PreOrderService.idempotencyKey(1L, 3L, "呼吸内科",
                java.time.LocalDate.parse("2026-09-20"));
        when(mapper.selectOne(any())).thenReturn(existing(88L, key));
        // 第一次 insert 抛唯一键冲突（并发下已有记录）
        when(mapper.insert(any(AgentPreOrder.class)))
                .thenThrow(new DuplicateKeyException("uk_idempotency"));

        AgentPreOrder result = service.create(1L, 3L, "呼吸内科", "2026-09-20", "咳嗽");

        // 关键断言：Redis 挂掉时并发幂等依然生效，返回的是已存在的单而非新建
        assertThat(result.getId()).isEqualTo(88L);
    }

    @Test
    @DisplayName("落库成功但占位回填失败 → 订单有效，不抛异常")
    void toleratesWriteBackFailure() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        // 预占成功后，回填占位时失败
        when(valueOps.set(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("Redis 写入时掉线"));

        AgentPreOrder result = service.create(1L, 3L, "呼吸内科", "2026-09-20", "咳嗽");

        assertThat(result).isNotNull();
        verify(mapper).insert(any(AgentPreOrder.class));
    }

    @Test
    @DisplayName("Redis 正常：预占命中 → 直接返回已有单，不重复落库")
    void redisHitReturnsExistingWithoutInsert() {
        String key = PreOrderService.idempotencyKey(1L, 3L, "呼吸内科",
                java.time.LocalDate.parse("2026-09-20"));
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);
        when(mapper.selectOne(any())).thenReturn(existing(66L, key));

        AgentPreOrder result = service.create(1L, 3L, "呼吸内科", "2026-09-20", "咳嗽");

        assertThat(result.getId()).isEqualTo(66L);
        verify(mapper, never()).insert(any(AgentPreOrder.class));
    }

    @Test
    @DisplayName("幂等键在同一用户同科室同日下稳定（Redis 挂时也稳定）")
    void idempotencyKeyIsStableEvenWhenRedisDown() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("Redis 连不上"));
        java.time.LocalDate date = java.time.LocalDate.parse("2026-09-20");

        String k1 = PreOrderService.idempotencyKey(1L, 3L, "呼吸内科", date);
        String k2 = PreOrderService.idempotencyKey(1L, 3L, "呼吸内科", date);
        String k3 = PreOrderService.idempotencyKey(1L, 3L, "呼吸内科",
                date.plusDays(1));

        assertThat(k1).isEqualTo(k2);
        assertThat(k1).isNotEqualTo(k3);
    }
}
