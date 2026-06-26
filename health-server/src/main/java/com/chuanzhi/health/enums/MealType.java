package com.chuanzhi.health.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MealType {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐"),
    SNACK("加餐");

    @EnumValue
    private final String label;

    MealType(String label) { this.label = label; }

    public String getLabel() { return label; }

    @JsonValue
    public String getValue() { return name(); }

    @JsonCreator
    public static MealType fromValue(String value) {
        if (value == null) return null;
        for (MealType t : values()) {
            if (t.name().equals(value) || t.label.equals(value)) return t;
        }
        throw new IllegalArgumentException("无效餐次: " + value);
    }
}
