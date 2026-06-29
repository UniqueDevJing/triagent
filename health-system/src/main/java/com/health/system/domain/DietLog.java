package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("diet_log")
public class DietLog extends BaseEntity {
    private Long memberId;
    private LocalDate logDate;
    private String mealType;
    private String foodName;
    private BigDecimal quantity;
    private BigDecimal calories;
    private String remark;
}
