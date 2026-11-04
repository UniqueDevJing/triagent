package com.health.web.assistant.memory;

import java.util.List;
import java.util.function.Consumer;

/**
 * 事实总线：Agent 内核内部各组件（状态机 / 越权守卫 / 记忆）发布-订阅结构化事实的轻量总线。
 *
 * <p>与 {@link SessionFactStore}（会话级急诊事实 KV）互补——总线是通用 pub/sub 原语，
 * 可被 {@code InMemorySessionFactStore} 等组件内部复用，也可用于解耦
 * 「状态机产出事实 → 编排层消费」的协作链路。
 *
 * <p>设计约束：不依赖任何外部框架或中间件，纯 Java。分布式场景由业务侧适配
 * Redis / Kafka 等实现该接口（health-admin 中的 {@code RedisSessionFactStore}
 * 即对应落地形态的一个参照）。
 *
 * @param <T> 事实载荷类型
 */
public interface FactBus<T> {

    /** 向某主题发布一条事实 */
    void publish(String topic, T fact);

    /** 订阅某主题；订阅后新发布的事实会推送给 listener（订阅即补发该主题历史） */
    void subscribe(String topic, Consumer<T> listener);

    /** 重放某主题已发布的所有事实（按发布顺序） */
    List<T> replay(String topic);

    /** 清空某主题 */
    void clear(String topic);

    /** 清空全部主题 */
    void clearAll();
}
