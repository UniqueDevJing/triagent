package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("assessment_templates")
public class AssessmentTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "量表标题不能为空")
    private String title;

    private String description;
    private String category;
    private String questions;
    private String scoringRules;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
