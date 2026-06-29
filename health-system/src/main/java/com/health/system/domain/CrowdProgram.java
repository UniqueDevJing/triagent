package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crowd_program")
public class CrowdProgram extends BaseEntity {
    private String programName;
    private String targetCrowd;
    private String programContent;
    private String frequency;
    private String status;
}
