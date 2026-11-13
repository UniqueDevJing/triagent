package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("crowd_programs")
public class CrowdProgram {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 方案名称 */
    private String name;

    /** 目标人群条件（JSON对象） */
    private String targetGroup;

    /** 方案描述 */
    private String description;

    /** 方案内容（JSON对象） */
    private String content;

    /** 状态：1-启用 0-停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
