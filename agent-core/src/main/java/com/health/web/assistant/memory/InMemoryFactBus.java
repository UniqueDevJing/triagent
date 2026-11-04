package com.health.web.assistant.memory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 内存版事实总线（进程内，重启即丢）。
 * 用 {@link CopyOnWriteArrayList} 承载事实与订阅者，读多写少且需安全遍历推送时友好。
 */
public class InMemoryFactBus<T> implements FactBus<T> {

    private final Map<String, List<T>> facts = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<T>>> listeners = new ConcurrentHashMap<>();

    @Override
    public void publish(String topic, T fact) {
        if (topic == null || fact == null) {
            return;
        }
        facts.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(fact);
        List<Consumer<T>> subs = listeners.get(topic);
        if (subs != null) {
            for (Consumer<T> c : subs) {
                c.accept(fact);
            }
        }
    }

    @Override
    public void subscribe(String topic, Consumer<T> listener) {
        if (topic == null || listener == null) {
            return;
        }
        listeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(listener);
        // 订阅即补发历史，保证新订阅者也能拿到已存在的上下文
        List<T> history = facts.get(topic);
        if (history != null) {
            for (T f : history) {
                listener.accept(f);
            }
        }
    }

    @Override
    public List<T> replay(String topic) {
        List<T> l = facts.get(topic);
        return l == null ? List.of() : List.copyOf(l);
    }

    @Override
    public void clear(String topic) {
        if (topic != null) {
            facts.remove(topic);
            listeners.remove(topic);
        }
    }

    @Override
    public void clearAll() {
        facts.clear();
        listeners.clear();
    }
}
