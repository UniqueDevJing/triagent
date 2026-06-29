package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_record")
public class AssessmentRecord extends BaseEntity {
    private Long memberId;
    private Long templateId;
    private BigDecimal totalScore;
    private String riskLevel;
    private String conclusion;
    private String suggestion;
    private Long assessorId;
    private LocalDate assessDate;
}
