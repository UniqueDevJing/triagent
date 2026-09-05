package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("education_word")
public class EducationWord extends BaseEntity {
    private String term;
    private String definition;
    private String category;
    private String example;
}
