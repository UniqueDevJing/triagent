package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("health_recipes")
public class HealthRecipe {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String ingredients;
    private String steps;
    private Integer calories;
    private String nutritionInfo;
    private String suitableFor;
    private Integer cookingTime;
    private String difficulty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
