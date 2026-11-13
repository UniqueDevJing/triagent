package com.health.web.assistant.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 「无伴随症状」否定语义的边界。
 *
 * <p>状态机原先用 {@code text.contains("无")} 识别用户回答「无伴随症状」，
 * 以免陷入无限追问。但「无」是高频字且大量出现在**症状描述本身** ——
 * 「无力 / 无法呼吸 / 无意识 / 无关节痛」都含「无」。
 *
 * <p>误判后果：{@code accompanyKnown} 被错误置为 true → 系统认为三字段已齐
 * → 直接 PROCEED 交给模型，**跳过了本该进行的「有无伴随症状」追问**。
 * 用户描述的恰恰是需要评估的症状，系统却以为信息收集已完成。
 * 在分诊场景里这是安全性问题，不是体验问题。
 *
 * <p>修复为「否定词 + 被否定对象白名单」判定，并单独排除症状转归描述
 * （「无明显缓解」说的是症状变化，不是「没有伴随症状」）。
 */
class SymptomNegationBoundaryTest {

    private final SymptomStateMachine sm = new SymptomStateMachine();

    // ────────────────── 修复点：症状描述含「无」不应被当作否定回答 ──────────────────

    @ParameterizedTest(name = "[{index}] 「{0}」不应判为否定回答")
    @ValueSource(strings = {
            "无力", "无法呼吸", "无意识", "无法站立", "无法行走",
            "无力抬起手臂", "无关节痛", "无痛感", "无法进食",
    })
    @DisplayName("含「无」的症状描述不是「无伴随症状」")
    void symptomContainingWuIsNotNegation(String phrase) {
        assertThat(sm.isNegationAnswer(phrase))
                .as("「%s」是症状描述，不应视为否定回答", phrase)
                .isFalse();
    }

    @ParameterizedTest(name = "[{index}] 「{0}」是症状转归，不是否定回答")
    @ValueSource(strings = {"无明显缓解", "无明显不适感加重", "无加重", "症状无明显变化"})
    @DisplayName("症状转归描述不应判为否定回答")
    void progressionIsNotNegation(String phrase) {
        assertThat(sm.isNegationAnswer(phrase))
                .as("「%s」描述的是症状变化而非「有无伴随症状」", phrase)
                .isFalse();
    }

    // ────────────────── 防回归：真正的否定回答仍须识别 ──────────────────

    @ParameterizedTest(name = "[{index}] 「{0}」应判为否定回答")
    @ValueSource(strings = {
            "无", "没有", "都没", "暂时无", "暂时没有", "没有了", "都没了",
            "无。", "没有！", "  无  ",
            "无发热", "没有其他症状", "无明显伴随症状", "没有伴随症状", "无不适",
            "暂时无其他", "无其他", "没有其他", "没有别的",
    })
    @DisplayName("真正的否定回答仍被识别（不回归无限追问）")
    void genuineNegationStillWorks(String phrase) {
        assertThat(sm.isNegationAnswer(phrase))
                .as("「%s」是明确的否定回答", phrase)
                .isTrue();
    }

    // ────────────────── 端到端：状态机不应因症状含「无」而提前 PROCEED ──────────────────

    @Test
    @DisplayName("用户描述「无力」时不应判定为字段已齐")
    void weaknessDoesNotShortCircuitCollection() {
        // 第 1 轮：给部位与时长，让 accompany 成为下一个待澄清字段
        sm.decide("wu-1", "头痛，3 天了，无力");
        // 第 2 轮：应仍在追问伴随症状，而不是认为已完成
        SymptomStateMachine.Decision d = sm.decide("wu-1", "无力");
        assertThat(d.type())
                .as("「无力」是症状描述，系统应继续收集伴随症状信息")
                .isEqualTo(SymptomStateMachine.DecisionType.CLARIFY);
        assertThat(d.missingFields()).containsExactly("accompany");
    }

    @Test
    @DisplayName("明确回答「没有其他症状」后应正常 PROCEED")
    void explicitNegationProceeds() {
        sm.decide("neg-e2e", "头痛，3 天了");
        SymptomStateMachine.Decision d = sm.decide("neg-e2e", "没有其他症状");
        assertThat(d.type()).isEqualTo(SymptomStateMachine.DecisionType.PROCEED);
    }

    // ────────────────── 红旗短路优先级不受影响 ──────────────────

    @ParameterizedTest(name = "[{index}] 「{0}」必须触发急诊短路")
    @ValueSource(strings = {"胸痛", "呼吸困难", "昏迷", "一侧肢体无力", "大量出血"})
    @DisplayName("危急征象仍短路为 EMERGENCY（否定识别不得吞掉红旗）")
    void emergencyStillShortCircuits(String red) {
        SymptomStateMachine.Decision d = sm.decide("em-" + red, "我" + red + "，很不舒服");
        assertThat(d.type()).isEqualTo(SymptomStateMachine.DecisionType.EMERGENCY);
    }

    // ────────────────── 空值安全 ──────────────────

    @Test
    @DisplayName("null / 空白不判为否定回答")
    void blankIsNotNegation() {
        for (String s : new String[]{null, "", "   ", "。", "，"}) {
            assertThat(sm.isNegationAnswer(s))
                    .as("「%s」不应判为否定回答", s)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("多轮会话中的红旗词仍可被查询接口复用")
    void redFlagsInReusable() {
        List<String> hits = sm.redFlagsIn("突然胸痛，伴随出汗");
        assertThat(hits).contains("胸痛");
    }
}
