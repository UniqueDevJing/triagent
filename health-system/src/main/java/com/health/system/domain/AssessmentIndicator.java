package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_indicator")
public class AssessmentIndicator extends BaseEntity {
    private String indicatorName;
    private String indicatorType;
    private String unit;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String riskLevel;
    private String status;
}
