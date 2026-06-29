package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chronic_disease")
public class ChronicDisease extends BaseEntity {
    private Long memberId;
    private String diseaseName;
    private String diagnosisDate;
    private String severity;
    private String medication;
    private String controlStatus;
    private String remark;
}
