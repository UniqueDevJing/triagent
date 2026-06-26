package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.chuanzhi.health.enums.PlanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("intervention_plans")
public class InterventionPlan {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "计划标题不能为空")
    private String title;

    @NotBlank(message = "计划目标不能为空")
    private String goal;

    /** 关联会员ID */
    private Long memberId;

    /** 干预类型：CROWD(群体)/CHRONIC(慢病)/DIET(膳食) */
    private String type;

    /** 计划描述 */
    private String description;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    private LocalDate endDate;

    private PlanStatus status;

    /** 干预结果（JSON） */
    private String result;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
