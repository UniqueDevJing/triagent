package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("intervention_plan")
public class InterventionPlan extends BaseEntity {
    private Long memberId;
    private String planName;
    private String planType;
    private String targetGoal;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long creatorId;
}
