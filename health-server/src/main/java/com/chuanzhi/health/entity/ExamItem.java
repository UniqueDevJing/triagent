package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam_items")
public class ExamItem {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private String description;
    private String referenceRange;
    private String unit;
    private Long categoryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableField(exist = false) private String categoryName;
}
