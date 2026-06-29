package com.chuanzhi.health.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum RiskLevel {
    LOW("低风险"),
    MEDIUM("中风险"),
    HIGH("高风险");

    @EnumValue
    private final String label;

    RiskLevel(String label) { this.label = label; }
    public String getLabel() { return label; }

    @JsonValue
    public String getValue() { return name(); }

    @JsonCreator
    public static RiskLevel fromValue(String value) {
        if (value == null) return null;
        for (RiskLevel rl : values()) {
            if (rl.name().equals(value) || rl.label.equals(value)) return rl;
        }
        throw new IllegalArgumentException("无效风险等级: " + value);
    }
}
