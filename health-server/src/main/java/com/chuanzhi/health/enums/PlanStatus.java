package com.chuanzhi.health.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlanStatus {
    DRAFT("草稿"),
    ACTIVE("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    @EnumValue
    @JsonValue
    private final String label;

    PlanStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
