package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.chuanzhi.health.enums.RiskLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("assessment_records")
public class AssessmentRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "量表ID不能为空")
    private Long templateId;

    private String answers;

    private BigDecimal totalScore;

    private RiskLevel riskLevel;

    private String reportText;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
