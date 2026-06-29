package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_item")
public class ExamItem extends BaseEntity {
    private String itemName;
    private String itemCode;
    private String unit;
    private BigDecimal price;
    private String referenceRange;
    private Long categoryId;
    private String remark;
}
