package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("assessment_indicators")
public class AssessmentIndicator {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String unit;

    private BigDecimal referenceMin;

    private BigDecimal referenceMax;

    private String category;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
