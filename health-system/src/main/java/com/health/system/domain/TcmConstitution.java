package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tcm_constitution")
public class TcmConstitution extends BaseEntity {
    private Long memberId;
    private String constitutionType;
    private Integer score;
    private String description;
    private String healthAdvice;
}
