package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("disease_library")
public class DiseaseLibrary extends BaseEntity {
    private String diseaseName;
    private String category;
    private String symptoms;
    private String causes;
    private String treatment;
    private String prevention;
    private String status;
}
