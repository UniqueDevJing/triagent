package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tcm_constitutions")
public class TcmConstitution {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String features;

    private String advice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
