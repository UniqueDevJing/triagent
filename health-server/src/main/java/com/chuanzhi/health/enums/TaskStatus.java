package com.chuanzhi.health.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TaskStatus {
    PENDING("待执行"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    OVERDUE("已逾期");

    @EnumValue
    private final String label;

    TaskStatus(String label) { this.label = label; }
    public String getLabel() { return label; }

    @JsonValue
    public String getValue() { return name(); }

    @JsonCreator
    public static TaskStatus fromValue(String value) {
        if (value == null) return null;
        for (TaskStatus ts : values()) {
            if (ts.name().equals(value) || ts.label.equals(value)) return ts;
        }
        throw new IllegalArgumentException("无效任务状态: " + value);
    }
}
