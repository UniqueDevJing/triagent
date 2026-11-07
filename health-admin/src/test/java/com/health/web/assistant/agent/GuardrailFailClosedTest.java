package com.health.web.assistant.agent;

import com.health.web.assistant.advisor.AgentAdvice;
import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 输出护栏 fail-closed 回归（全面质检发现的安全缺陷）。
 *
 * <p>原实现：{@code Advisor.after} 抛异常时只打 warn 就继续放行。
 * 而 {@code after} 正是"处方/剂量输出拦截"的落点——它崩了却放行，等于护栏被静默绕过。
 *
 * <p>这里锁死三条：护栏抛异常 → 必须拦；理由要能审计；正常护栏不能被误伤。
 */
class GuardrailFailClosedTest {

    /** 只关心 after() 的测试基类：before/order 用默认实现。 */
    private abstract static class TestAdvice implements AgentAdvice {
        @Override
        public AgentRequest before(AgentRequest request) {
            return request;
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }

    private static AgentRequest request() {
        return new AgentRequest("sess-fail-closed", "我该吃几片布洛芬");
    }

    @Test
    @DisplayName("护栏抛异常 → 拦截回答，且理由可审计")
    void failingAdviceBlocksAnswer() {
        AgentAdvice broken = new TestAdvice() {
            @Override
            public void after(AgentRequest request, AgentResponse response) {
                throw new IllegalStateException("guard exploded");
            }
        };

        AgentResponse response = new AgentResponse();
        response.setFullText("每次吃两片布洛芬，连服三天");   // 假设模型给了危险内容

        boolean blocked = AssistantOrchestrator.applyAfterAdvices(
                List.of(broken), request(), response);

        assertThat(blocked).isTrue();
        assertThat(response.isBlocked()).isTrue();
        assertThat(response.getBlockReason()).startsWith("guardrail-failure:");
        assertThat(response.getFullText())
                .as("危险原文必须被替换掉")
                .doesNotContain("两片")
                .contains("未通过安全校验");
    }

    @Test
    @DisplayName("护栏正常时不误伤：回答原样保留")
    void healthyAdviceKeepsAnswer() {
        AgentAdvice healthy = new TestAdvice() {
            @Override
            public void after(AgentRequest request, AgentResponse response) {
                // 代表"这段输出是安全的"
            }
        };

        AgentResponse response = new AgentResponse();
        response.setFullText("建议尽快到正规医院就诊，由医生面诊判断。");

        boolean blocked = AssistantOrchestrator.applyAfterAdvices(
                List.of(healthy), request(), response);

        assertThat(blocked).isFalse();
        assertThat(response.isBlocked()).isFalse();
        assertThat(response.getFullText()).contains("正规医院");
    }

    @Test
    @DisplayName("多个护栏：前一个失败即停止，后面的护栏不再执行")
    void stopsAtFirstFailure() {
        boolean[] secondRan = {false};
        AgentAdvice broken = new TestAdvice() {
            @Override
            public void after(AgentRequest request, AgentResponse response) {
                throw new IllegalStateException("boom");
            }
        };
        AgentAdvice second = new TestAdvice() {
            @Override
            public void after(AgentRequest request, AgentResponse response) {
                secondRan[0] = true;
            }
        };

        AgentResponse response = new AgentResponse();
        response.setFullText("危险内容");

        assertThat(AssistantOrchestrator.applyAfterAdvices(
                List.of(broken, second), request(), response)).isTrue();
        assertThat(secondRan[0]).as("失败后不应继续执行后续护栏").isFalse();
    }
}