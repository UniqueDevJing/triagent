package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("disease_library")
public class DiseaseLibrary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String description;
    private String symptoms;
    private String causes;
    private String treatment;
    private String prevention;
    private String riskFactors;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
