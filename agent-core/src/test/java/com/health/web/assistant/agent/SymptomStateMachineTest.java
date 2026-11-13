package com.health.web.assistant.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** agent-core：分诊状态机（确定性规则）核心行为单测（纯单测，无容器）。 */
class SymptomStateMachineTest {

    private final SymptomStateMachine sm = new SymptomStateMachine();

    @Test
    @DisplayName("红旗征象 → 强制 EMERGENCY（不依赖 LLM）")
    void redFlagTriggersEmergency() {
        SymptomStateMachine.Decision d = sm.decide("s1", "我突然胸痛呼吸困难");
        assertThat(d.type()).isEqualTo(SymptomStateMachine.DecisionType.EMERGENCY);
        assertThat(d.redFlagWords()).contains("胸痛", "呼吸困难");
    }

    @Test
    @DisplayName("矛盾症状（描述症状 + 自述无需就医）→ 强制 CLARIFY 防诱导降级")
    void contradictionForcesClarify() {
        SymptomStateMachine.Decision d = sm.decide("s2", "我头痛，但没事了不用去医院");
        assertThat(d.type()).isEqualTo(SymptomStateMachine.DecisionType.CLARIFY);
        assertThat(d.missingFields()).contains("severityConfirm");
    }

    @Test
    @DisplayName("常规流程按 部位→时长→伴随 依次追问")
    void routineClarifiesFieldsInOrder() {
        SymptomStateMachine.Decision d1 = sm.decide("s3", "头痛");
        assertThat(d1.type()).isEqualTo(SymptomStateMachine.DecisionType.CLARIFY);
        assertThat(d1.missingFields()).contains("duration"); // 部位已知，追问时长

        SymptomStateMachine.Decision d2 = sm.decide("s3", "头痛3天");
        assertThat(d2.type()).isEqualTo(SymptomStateMachine.DecisionType.CLARIFY);
        assertThat(d2.missingFields()).contains("accompany");

        SymptomStateMachine.Decision d3 = sm.decide("s3", "头痛3天，伴恶心");
        assertThat(d3.type()).isEqualTo(SymptomStateMachine.DecisionType.PROCEED);
    }

    @Test
    @DisplayName("否定回答（无/没有伴随症状）应被识别，避免无限追问")
    void negationAnswerRecognized() {
        assertThat(sm.isNegationAnswer("无")).isTrue();
        assertThat(sm.isNegationAnswer("没有发热")).isTrue();
        assertThat(sm.isNegationAnswer("无力")).isFalse();   // “无力”是症状本身，不是否定回答
        assertThat(sm.isNegationAnswer("无明显缓解")).isFalse(); // 症状转归描述，非否定回答
    }

    @Test
    @DisplayName("急诊 override 仅在无红旗残留时清事实，防急症被诱导降级")
    void emergencyOverrideNeedsNoRedFlag() {
        assertThat(sm.emergencyOverrideClearsFact("我看过急诊了")).isTrue();
        assertThat(sm.emergencyOverrideClearsFact("我胸痛但看过急诊了")).isFalse();
    }

    @Test
    @DisplayName("redFlagsIn 抽取命中征象集合")
    void redFlagsInCollectsHits() {
        assertThat(sm.redFlagsIn("胸痛伴呼吸困难"))
                .contains("胸痛", "呼吸困难");
        assertThat(sm.redFlagsIn("就是有点累")).isEmpty();
    }
}
