package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recipe_library")
public class RecipeLibrary extends BaseEntity {
    private String recipeName;
    private String mealType;
    private String suitableFor;
    private BigDecimal totalCalories;
    private String ingredients;
    private String steps;
    private String nutritionInfo;
    private String status;
}
