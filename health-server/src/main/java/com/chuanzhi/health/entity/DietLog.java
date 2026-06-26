package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.chuanzhi.health.enums.MealType;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("diet_logs")
public class DietLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联会员ID */
    private Long memberId;

    /** 餐次 */
    private MealType mealType;

    /** 食物列表（JSON数组） */
    private String foodItems;

    /** 总热量（千卡） */
    private Integer calories;

    /** 记录日期 */
    private LocalDate recordedDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
