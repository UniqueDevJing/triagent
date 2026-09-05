package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private String type;
    private BigDecimal totalScore;
    private String riskLevel;
    private String conclusion;
    private String suggestion;
    private Long assessorId;
    private LocalDate assessDate;
    /** 关联查询字段，非数据库列 */
    @TableField(exist = false)
    private String memberName;
}
