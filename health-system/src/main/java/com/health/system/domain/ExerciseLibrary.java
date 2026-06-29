package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exercise_library")
public class ExerciseLibrary extends BaseEntity {
    private String exerciseName;
    private String exerciseType;
    private String difficulty;
    private Integer duration;
    private BigDecimal caloriesBurn;
    private String description;
    private String videoUrl;
    private String status;
}
