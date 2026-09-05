package com.health.web.controller.realtime;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.health.framework.realtime.RedisSubscriber;
import com.health.framework.realtime.SseEmitterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/api/sse")
public class SseController {

    private static final Logger log = LoggerFactory.getLogger(SseController.class);
    private static final Set<String> TOPICS = Set.of(
            "dashboard", "users", "health_records", "assessments",
            "interventions", "knowledge", "notifications");
    private static final long TIMEOUT = 0L;

    private final RedisSubscriber subscriber;

    public SseController(RedisSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    @SaCheckLogin
    @GetMapping(value = "/subscribe/{topic}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String topic) {
        if (!TOPICS.contains(topic)) {
            SseEmitter err = new SseEmitter();
            try {
                err.send(SseEmitter.event().name("error").data("{\"msg\":\"不支持的 topic\"}"));
                err.complete();
            } catch (IOException ignored) {}
            return err;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        String key = topic + ":" + userId + ":" + System.nanoTime();
        SseEmitterHolder holder = new SseEmitterHolder(topic, userId, TIMEOUT);
        SseEmitter emitter = holder.getEmitter();

        emitter.onCompletion(() -> subscriber.unregister(key));
        emitter.onTimeout(() -> subscriber.unregister(key));
        emitter.onError(e -> subscriber.unregister(key));

        subscriber.register(key, holder);
        try { holder.sendConnected(); }
        catch (IOException e) { subscriber.unregister(key); }

        log.info("用户 {} 订阅 topic={} key={}", userId, topic, key);
        return emitter;
    }
}
