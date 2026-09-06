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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 预约两层幂等 + 越权/状态机校验（Mock Mapper 与 Redis，纯单测无容器） */
@ExtendWith(MockitoExtension.class)
class PreOrderServiceTest {

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

    private AgentPreOrder existing(long id, long userId, String status) {
        AgentPreOrder po = new AgentPreOrder();
        po.setId(id);
        po.setUserId(userId);
        po.setMemberId(3L);
        po.setDepartment("呼吸内科");
        po.setStatus(status);
        return po;
    }

    @Test
    @DisplayName("创建：空科室拒绝")
    void createRejectsBlankDepartment() {
        assertThatThrownBy(() -> service.create(1L, 3L, "  ", "2026-09-20", "咳嗽"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("科室");
    }

    @Test
    @DisplayName("创建：Redis 预占失败但记录已存在 → 幂等返回已有单，不再 insert")
    void createIdempotentHitViaRedis() {
        AgentPreOrder old = existing(5L, 1L, "PENDING");
        when(valueOps.setIfAbsent(anyString(), anyString(), eq(Duration.ofMinutes(5)))).thenReturn(false);
        when(mapper.selectOne(any())).thenReturn(old);

        AgentPreOrder result = service.create(1L, 3L, "呼吸内科", "2026-09-20", "咳嗽");

        assertThat(result.getId()).isEqualTo(5L);
        verify(mapper, never()).insert(any(AgentPreOrder.class));
    }

    @Test
    @DisplayName("创建：首次成功落库并回写 Redis 预占值")
    void createFirstTime() {
        when(valueOps.setIfAbsent(anyString(), anyString(), eq(Duration.ofMinutes(5)))).thenReturn(true);
        when(mapper.insert(any(AgentPreOrder.class))).thenAnswer(inv -> {
            AgentPreOrder po = inv.getArgument(0);
            po.setId(9L);
            return 1;
        });

        AgentPreOrder result = service.create(1L, 3L, "呼吸内科", "2026-09-20", "咳嗽");

        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(valueOps).set(anyString(), eq("9"), eq(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("创建：Redis 预占成功但并发撞唯一索引 → 兜底返回已有单")
    void createDuplicateKeyFallback() {
        when(valueOps.setIfAbsent(anyString(), anyString(), eq(Duration.ofMinutes(5)))).thenReturn(true);
        when(mapper.insert(any(AgentPreOrder.class)))
                .thenThrow(new DuplicateKeyException("uk_idempotency_key"));
        AgentPreOrder old = existing(5L, 1L, "PENDING");
        when(mapper.selectOne(any())).thenReturn(old);

        AgentPreOrder result = service.create(1L, 3L, "呼吸内科", "2026-09-20", "咳嗽");

        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("创建：无日期默认明天")
    void createDefaultsToTomorrow() {
        when(valueOps.setIfAbsent(anyString(), anyString(), eq(Duration.ofMinutes(5)))).thenReturn(true);
        when(mapper.insert(any(AgentPreOrder.class))).thenAnswer(inv -> {
            inv.getArgument(0, AgentPreOrder.class).setId(1L);
            return 1;
        });

        AgentPreOrder result = service.create(1L, 3L, "呼吸内科", null, "咳嗽");

        assertThat(result.getAppointmentDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    @DisplayName("确认：不存在 / 越权 / 非 PENDING 均拒绝")
    void confirmRejects() {
        when(mapper.selectById(404L)).thenReturn(null);
        when(mapper.selectById(1L)).thenReturn(existing(1L, 999L, "PENDING"));
        when(mapper.selectById(2L)).thenReturn(existing(2L, 1L, "CONFIRMED"));

        assertThatThrownBy(() -> service.confirm(1L, 404L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.confirm(1L, 1L)).hasMessageContaining("无权");
        assertThatThrownBy(() -> service.confirm(1L, 2L)).hasMessageContaining("CONFIRMED");
    }

    @Test
    @DisplayName("确认：PENDING → CONFIRMED，写入确认时间")
    void confirmSuccess() {
        AgentPreOrder po = existing(3L, 1L, "PENDING");
        when(mapper.selectById(3L)).thenReturn(po);

        AgentPreOrder result = service.confirm(1L, 3L);

        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        assertThat(result.getConfirmTime()).isNotNull();
        verify(mapper).updateById(po);
    }
}
