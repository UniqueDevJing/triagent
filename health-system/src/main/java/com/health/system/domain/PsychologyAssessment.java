package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("psychology_assessment")
public class PsychologyAssessment extends BaseEntity {
    private Long memberId;
    private String assessmentType;
    private Integer totalScore;
    private String resultLevel;
    private String analysis;
    private String suggestion;
    private LocalDate assessDate;
}
