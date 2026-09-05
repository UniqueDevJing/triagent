package com.health.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.health.common.core.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("intervention_task")
public class InterventionTask extends BaseEntity {
    private Long planId;
    private String title;
    private String description;
    private LocalDate dueDate;
    private String status;
    private LocalDateTime completedAt;
}
