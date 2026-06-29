package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_item_group")
public class ExamItemGroup extends BaseEntity {
    private String groupName;
    private String description;
    private Integer sortOrder;
}
