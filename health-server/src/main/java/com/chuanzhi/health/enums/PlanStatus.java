package com.chuanzhi.health.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum PlanStatus {
    DRAFT("草稿"),
    ACTIVE("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    @EnumValue
    private final String label;

    PlanStatus(String label) { this.label = label; }
    public String getLabel() { return label; }

    @JsonValue
    public String getValue() { return name(); }

    @JsonCreator
    public static PlanStatus fromValue(String value) {
        if (value == null) return null;
        for (PlanStatus ps : values()) {
            if (ps.name().equals(value) || ps.label.equals(value)) return ps;
        }
        throw new IllegalArgumentException("无效计划状态: " + value);
    }
}
