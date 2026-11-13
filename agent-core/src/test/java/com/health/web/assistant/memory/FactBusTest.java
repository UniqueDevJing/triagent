package com.health.web.assistant.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** agent-core：事实总线发布-订阅-重放语义（纯单测，无容器）。 */
class FactBusTest {

    @Test
    @DisplayName("发布后重放按序返回全部事实")
    void publishThenReplayInOrder() {
        FactBus<String> bus = new InMemoryFactBus<>();
        bus.publish("t1", "a");
        bus.publish("t1", "b");
        List<String> all = bus.replay("t1");
        assertThat(all).containsExactly("a", "b");
    }

    @Test
    @DisplayName("订阅者收到发布后的事实")
    void subscriberReceivesFutureFacts() {
        FactBus<String> bus = new InMemoryFactBus<>();
        AtomicInteger got = new AtomicInteger(0);
        bus.subscribe("t2", f -> got.incrementAndGet());
        bus.publish("t2", "x");
        bus.publish("t2", "y");
        assertThat(got.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("订阅即补发历史，清空后不再有事实")
    void subscribeReplaysHistoryAndClearWorks() {
        FactBus<String> bus = new InMemoryFactBus<>();
        bus.publish("t3", "h1");
        AtomicInteger got = new AtomicInteger(0);
        bus.subscribe("t3", f -> got.incrementAndGet()); // 应补发 1 条历史
        assertThat(got.get()).isEqualTo(1);

        bus.clear("t3");
        assertThat(bus.replay("t3")).isEmpty();
    }

    @Test
    @DisplayName("null 主题/事实被安全忽略")
    void nullSafe() {
        FactBus<String> bus = new InMemoryFactBus<>();
        bus.publish(null, "x");
        bus.publish("t", null);
        assertThat(bus.replay("t")).isEmpty();
    }
}
