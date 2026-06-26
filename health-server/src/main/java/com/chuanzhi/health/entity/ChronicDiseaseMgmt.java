package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("chronic_disease_mgmt")
public class ChronicDiseaseMgmt {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联会员ID */
    private Long memberId;

    /** 疾病类型：高血压/糖尿病/冠心病/COPD */
    private String diseaseType;

    /** 诊断日期 */
    private LocalDate diagnosisDate;

    /** 用药记录（JSON数组） */
    private String medication;

    /** 目标指标（JSON对象） */
    private String targetIndicators;

    /** 监测频率：MONTHLY/WEEKLY/DAILY */
    private String monitoringFrequency;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
