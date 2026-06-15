package com.chuanzhi.health.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RiskLevel {
    LOW("低风险"),
    MEDIUM("中风险"),
    HIGH("高风险");

    @EnumValue
    @JsonValue
    private final String label;

    RiskLevel(String label) { this.label = label; }
    public String getLabel() { return label; }
}
