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

    /** 评估类型：PHYSICAL(体检), TCM(中医), PSYCHOLOGY(心理) */
    private String type;

    /** 关联指标ID集合（JSON数组） */
    private String indicatorIds;

    /** 关联中医体质类型ID集合（JSON数组） */
    private String tcmTypeIds;

    /** 关联心理评估ID集合（JSON数组） */
    private String psychologyIds;

    /** 状态：0-草稿 1-启用 2-停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
