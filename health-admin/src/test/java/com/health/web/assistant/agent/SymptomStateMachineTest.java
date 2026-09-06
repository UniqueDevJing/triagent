package com.health.web.assistant.agent;

import com.health.web.assistant.model.TriageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 分诊状态机：红旗短路 / 澄清顺序 / 否定语义 / 应急结果组装 */
class SymptomStateMachineTest {

    private final SymptomStateMachine sm = new SymptomStateMachine();

    @ParameterizedTest
    @ValueSource(strings = {"突然胸痛伴大汗", "一侧肢体无力口角歪斜", "外伤后大量出血",
            "高热后抽搐不止", "儿童发热39℃伴惊厥", "突发晕厥倒地"})
    @DisplayName("红旗词命中 → 确定性 EMERGENCY（优先于字段缺失）")
    void redFlagShortCircuits(String msg) {
        SymptomStateMachine.Decision d = sm.decide("s-" + msg.hashCode(), msg);
        assertThat(d.type()).isEqualTo(SymptomStateMachine.DecisionType.EMERGENCY);
        assertThat(d.redFlagWords()).isNotEmpty();
    }

    @Test
    @DisplayName("多红旗命中 → 科室取并集")
    void multipleRedFlagsUnion() {
        TriageResult t = sm.buildEmergencyTriage(List.of("胸痛", "呼吸困难"));
        assertThat(t.getUrgency()).isEqualTo("EMERGENCY");
        assertThat(t.getDepartments()).contains("急诊科", "心内科", "呼吸内科");
        assertThat(t.getDisclaimer()).isNotBlank();
        assertThat(t.getHospitalLevel()).contains("120");
    }

    @Test
    @DisplayName("澄清顺序：部位 → 时长 → 伴随症状，会话内聚合不重复追问")
    void clarifyOrderAndAggregation() {
        String sid = "clarify-order";
        // 第一轮：无部位字段（「最近」本身即命中时长正则）→ 追问部位
        assertThat(sm.decide(sid, "我最近不太舒服").type())
                .isEqualTo(SymptomStateMachine.DecisionType.CLARIFY);
        // 第二轮：补部位 → 追问伴随
        SymptomStateMachine.Decision d2 = sm.decide(sid, "头痛");
        assertThat(d2.type()).isEqualTo(SymptomStateMachine.DecisionType.CLARIFY);
        assertThat(d2.missingFields()).containsExactly("accompany");
        // 第三轮：补伴随 → PROCEED 交给 LLM
        assertThat(sm.decide(sid, "有点恶心").type())
                .isEqualTo(SymptomStateMachine.DecisionType.PROCEED);
    }

    @Test
    @DisplayName("否定语义：回答「无」也能完成伴随症状采集（修复无限追问）")
    void denyAnswerCompletesAccompany() {
        String sid = "deny";
        sm.decide(sid, "上腹痛");            // 部位
        sm.decide(sid, "反复 2 个月");        // 时长
        SymptomStateMachine.Decision d = sm.decide(sid, "无");
        assertThat(d.type()).isEqualTo(SymptomStateMachine.DecisionType.PROCEED);
    }

    @Test
    @DisplayName("会话隔离：不同 sessionId 状态互不影响")
    void sessionIsolation() {
        sm.decide("iso-a", "头痛");
        SymptomStateMachine.Decision d = sm.decide("iso-b", "我最近不太舒服");
        assertThat(d.missingFields()).containsExactly("bodyPart");
    }

    @Test
    @DisplayName("红旗无副作用检测：redFlagsIn 不污染会话状态")
    void redFlagsInIsPure() {
        assertThat(sm.redFlagsIn("我胸痛还呼吸困难")).containsExactlyInAnyOrder("胸痛", "呼吸困难");
        assertThat(sm.redFlagsIn("今天天气不错")).isEmpty();
        assertThat(sm.redFlagsIn(null)).isEmpty();
        // 检测后同会话仍按正常澄清流程走（未被短路）
        assertThat(sm.decide("pure-1", "我最近不太舒服").type())
                .isEqualTo(SymptomStateMachine.DecisionType.CLARIFY);
    }

    @ParameterizedTest
    @CsvSource({
            "突然胸痛, 胸痛, 急诊科|心内科",
            "口角歪斜, 口角歪斜, 急诊科|神经内科",
            "大量出血, 大量出血, 急诊科|普外科"
    })
    @DisplayName("buildEmergencyTriage 按红旗映射科室")
    void emergencyTriageMapping(String msg, String expectedWord, String expectedDepts) {
        SymptomStateMachine.Decision d = sm.decide("map-" + msg, msg);
        TriageResult t = sm.buildEmergencyTriage(d.redFlagWords());
        for (String dept : expectedDepts.split("\\|")) {
            assertThat(t.getDepartments()).contains(dept);
        }
    }
}
