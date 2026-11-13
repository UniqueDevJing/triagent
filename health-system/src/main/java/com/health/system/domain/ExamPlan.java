package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_plan")
public class ExamPlan extends BaseEntity {
    private Long memberId;
    private String planName;
    private LocalDate planDate;
    private Long packageId;
    private String status;
    private String reportPath;
    private String conclusion;
}
