package com.health.web.assistant.trace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T6 验收：脱敏规则不可逆且幂等；单次请求 Trace 可还原完整决策链；
 * ToolGateway 工具 span 挂到会话活跃 trace。
 */
class TraceRedactorTest {

    @Test
    @DisplayName("手机号脱敏：保留前3后4")
    void redactsPhone() {
        assertThat(SensitiveRedactor.redact("请核对手机号 13812345678 是否正确"))
                .isEqualTo("请核对手机号 138****5678 是否正确");
    }

    @Test
    @DisplayName("身份证脱敏：保留前4后2")
    void redactsIdCard() {
        assertThat(SensitiveRedactor.redact("身份证 410401199901011234 就诊"))
                .isEqualTo("身份证 4104**********34 就诊");
    }

    @Test
    @DisplayName("邮箱脱敏：保留首字符与域名")
    void redactsEmail() {
        assertThat(SensitiveRedactor.redact("发送到 user@example.com"))
                .isEqualTo("发送到 u***@example.com");
    }

    @Test
    @DisplayName("JWT 整体打码")
    void redactsJwt() {
        assertThat(SensitiveRedactor.redact("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJVadQssw5c"))
                .isEqualTo("Bearer ***JWT***");
    }

    @Test
    @DisplayName("敏感字段名：值整体打码（key/token/secret/password）")
    void redactsSensitiveFieldValues() {
        assertThat(SensitiveRedactor.redactField("apiKey", "sk-abc123def456")).isEqualTo("***REDACTED***");
        assertThat(SensitiveRedactor.redactField("password", "whatever")).isEqualTo("***REDACTED***");
        assertThat(SensitiveRedactor.redactField("keyword", "头痛 13812345678")).isEqualTo("头痛 138****5678");
    }

    @Test
    @DisplayName("脱敏幂等：对已打码文本重复处理不变")
    void redactIsIdempotent() {
        String once = SensitiveRedactor.redact("手机 13812345678");
        assertThat(SensitiveRedactor.redact(once)).isEqualTo(once);
    }

    @Test
    @DisplayName("Trace：单次请求可还原完整决策链（路由→工具→收尾）")
    void traceReconstructsDecisionChain() {
        TraceStore store = new TraceStore();
        RequestTrace trace = store.begin("sess-t6");
        RequestTrace.Span route = trace.begin("route");
        route.attr("decided", "TRIAGE");
        route.ok();
        RequestTrace.Span tool = trace.begin("tool.searchDiseases", route.spanId());
        tool.attr("role", "TRIAGE");
        tool.ok();
        RequestTrace.Span llm = trace.begin("llm-stream");
        llm.ok();
        store.finish(trace);

        String json = store.recentJson("sess-t6", 5);
        assertThat(json).contains("\"name\":\"route\"")
                .contains("\"name\":\"tool.searchDiseases\"")
                .contains("\"name\":\"llm-stream\"")
                .contains("\"parentSpanId\":\"" + route.spanId() + "\"")
                .contains("\"finished\":true");
        assertThat(store.count("sess-t6")).isEqualTo(1);
    }

    @Test
    @DisplayName("TraceStore：活跃 trace 请求结束后解绑")
    void activeUnboundAfterFinish() {
        TraceStore store = new TraceStore();
        RequestTrace trace = store.begin("sess-t6b");
        assertThat(store.active("sess-t6b")).isSameAs(trace);
        store.finish(trace);
        assertThat(store.active("sess-t6b")).isNull();
    }

    @Test
    @DisplayName("Trace：attributes 写入即脱敏（手机号不落盘）")
    void traceAttributesRedacted() {
        TraceStore store = new TraceStore();
        RequestTrace trace = store.begin("sess-t6c");
        trace.begin("route").attr("contact", "13812345678").ok();
        store.finish(trace);
        assertThat(store.recentJson("sess-t6c", 1)).doesNotContain("13812345678").contains("138****5678");
    }
}
