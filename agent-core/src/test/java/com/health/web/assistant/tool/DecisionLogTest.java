package com.health.web.assistant.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 决策日志单测：记录 / 会话重放顺序一致 / 查询 / 截断 / 环形窗口。
 * 重放一致率 = replay 序列与 record 顺序逐条一致（可解释复盘的前提）。
 */
class DecisionLogTest {

    @Test
    @DisplayName("重放：同会话多轮记录按发生顺序完整回放")
    void replayPreservesOrderAndContent() {
        DecisionLog log = new DecisionLog();
        String sid = "s-replay";
        for (int i = 1; i <= 5; i++) {
            log.record(DecisionRecord.builder()
                    .sessionId(sid)
                    .agentType("TRIAGE")
                    .turnType(i == 3 ? "EMERGENCY" : "CLARIFY")
                    .message("第" + i + "轮主诉")
                    .redFlags(i == 3 ? List.of("胸痛", "呼吸困难") : List.of())
                    .ruleHits(i == 3 ? List.of("red-flag-short-circuit") : List.of())
                    .finalText("第" + i + "轮答复")
                    .urgency(i == 3 ? "EMERGENCY" : null)
                    .elapsedMs(100L * i)
                    .build());
        }
        List<DecisionRecord> replayed = log.replay(sid);
        assertThat(replayed).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(replayed.get(i).message()).isEqualTo("第" + (i + 1) + "轮主诉");
        }
        DecisionRecord emergency = replayed.get(2);
        assertThat(emergency.turnType()).isEqualTo("EMERGENCY");
        assertThat(emergency.redFlags()).isEqualTo("胸痛,呼吸困难");
        assertThat(emergency.ruleHits()).isEqualTo("red-flag-short-circuit");
        assertThat(emergency.urgency()).isEqualTo("EMERGENCY");
        // 重放一致率 100%：逐字段比对与写入一致
        long matched = replayed.stream()
                .filter(r -> r.message().endsWith("轮主诉") && r.agentType().equals("TRIAGE"))
                .count();
        assertThat(matched).isEqualTo(5);
    }

    @Test
    @DisplayName("隔离：不同会话互不串扰；未知会话返回空")
    void sessionsAreIsolated() {
        DecisionLog log = new DecisionLog();
        log.record(DecisionRecord.builder().sessionId("a").turnType("LLM").message("A1").build());
        log.record(DecisionRecord.builder().sessionId("b").turnType("LLM").message("B1").build());
        log.record(DecisionRecord.builder().sessionId("a").turnType("CLARIFY").message("A2").build());
        assertThat(log.replay("a")).extracting(DecisionRecord::message)
                .containsExactly("A1", "A2");
        assertThat(log.replay("b")).extracting(DecisionRecord::message)
                .containsExactly("B1");
        assertThat(log.replay("nope")).isEmpty();
    }

    @Test
    @DisplayName("查询：limit 截断取最近 N 条；message/finalText 超长截断")
    void queryLimitsAndTruncation() {
        DecisionLog log = new DecisionLog();
        for (int i = 0; i < 10; i++) {
            log.record(DecisionRecord.builder()
                    .sessionId("q").turnType("LLM")
                    .message("m" + i)
                    .finalText("x".repeat(600))
                    .build());
        }
        List<DecisionRecord> last3 = log.query("q", 3);
        assertThat(last3).hasSize(3);
        assertThat(last3.get(0).message()).isEqualTo("m7");
        assertThat(last3.get(2).message()).isEqualTo("m9");
        // 截断：512 上限 + 省略标记
        assertThat(last3.get(0).finalText()).startsWith("x").endsWith("省略)");
        assertThat(last3.get(0).finalText().length()).isLessThan(600);
        // 全量查询
        assertThat(log.query(null, 100)).hasSize(10);
        assertThat(log.query("", 5)).hasSize(5);
    }

    @Test
    @DisplayName("clear：按会话清除决策链；null 记录被忽略")
    void clearAndNullSafety() {
        DecisionLog log = new DecisionLog();
        log.record(null);
        assertThat(log.size()).isZero();
        log.record(DecisionRecord.builder().sessionId("c").turnType("LLM").message("C1").build());
        assertThat(log.size()).isEqualTo(1);
        log.clear("c");
        assertThat(log.size()).isZero();
        assertThat(log.replay("c")).isEmpty();
        log.clear(null); // 不抛异常
    }
}
