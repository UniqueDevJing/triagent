package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_records")
public class HealthRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @NotBlank(message = "记录类型不能为空")
    private String type;

    private String metrics;
    private String reportUrl;
    private String doctorNotes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
