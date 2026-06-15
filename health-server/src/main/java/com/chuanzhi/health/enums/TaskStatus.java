package com.chuanzhi.health.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {
    PENDING("待执行"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    OVERDUE("已逾期");

    @EnumValue
    @JsonValue
    private final String label;

    TaskStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
