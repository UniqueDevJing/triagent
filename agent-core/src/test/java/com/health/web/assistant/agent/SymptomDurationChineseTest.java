package com.health.web.assistant.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文时长识别回归（线上实测发现的真实缺陷）。
 *
 * <p>现象：用户说"我头痛三天了"，系统仍回"这个症状持续或反复多久了？"——反复追问同一个问题。
 * 根因：DURATION_PATTERN 里的中文分支出只覆盖"…以内"句式，裸的"三天/两周/半个月"匹配不到。
 *
 * <p>这里锁死：中文数词 + 时间单位必须被认定"时长已知"，不再追问 duration。
 */
class SymptomDurationChineseTest {

    private final SymptomStateMachine sm = new SymptomStateMachine();

    @ParameterizedTest(name = "「{0}」应识别出时长")
    @ValueSource(strings = {
            "我头痛三天了",
            "头痛三天，还有点恶心",
            "咳嗽两周了",
            "半个月了",
            "头晕一个月",
            "腰痛一年多了",
            "发烧三小时",
            "嗓子疼五天了",
            "腹泻两天",
    })
    @DisplayName("中文数词时长 → 不再追问 duration")
    void chineseNumeralDurationIsRecognized(String text) {
        SymptomStateMachine.Decision d = sm.decide("cn-" + text.hashCode(), text);
        assertThat(d.missingFields())
                .as("已经说了时长，就不该再追问 duration")
                .doesNotContain("duration");
    }

    @Test
    @DisplayName("没给时长 → 仍然要追问 duration（不能把正常追问也改坏）")
    void missingDurationStillAsks() {
        SymptomStateMachine.Decision d = sm.decide("cn-none", "我头痛");
        assertThat(d.missingFields()).contains("duration");
    }

    @Test
    @DisplayName("阿拉伯数字时长保持可用（不回退）")
    void arabicDurationStillWorks() {
        SymptomStateMachine.Decision d = sm.decide("cn-arabic", "头痛3天");
        assertThat(d.missingFields()).doesNotContain("duration");
    }
}