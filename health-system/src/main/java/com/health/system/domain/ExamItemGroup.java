package com.health.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.health.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测项目组（分类）。列 group_name 映射为 name 以匹配前端/通用语义。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_item_group")
public class ExamItemGroup extends BaseEntity {

    /** 组/分类名称 */
    @TableField("group_name")
    private String name;

    /** 描述 */
    private String description;

    /** 排序 */
    private Integer sortOrder;
}
