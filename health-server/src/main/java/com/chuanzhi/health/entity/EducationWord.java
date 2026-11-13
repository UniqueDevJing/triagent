package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("education_words")
public class EducationWord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String term;
    private String definition;
    private String category;
    private String example;
    private LocalDateTime createdAt;
}
