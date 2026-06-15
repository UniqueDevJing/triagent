package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.chuanzhi.health.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("intervention_tasks")
public class InterventionTask {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String description;

    private LocalDate dueDate;

    private TaskStatus status;

    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
