package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("physical_exam_plans")
public class PhysicalExamPlan {
    @TableId(type = IdType.AUTO) private Long id;
    private Long memberId;
    private String planName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
