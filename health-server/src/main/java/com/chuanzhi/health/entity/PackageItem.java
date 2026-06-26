package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("package_items")
public class PackageItem {
    @TableId(type = IdType.AUTO) private Long id;
    private Long packageId;
    private Long examItemId;
    private Integer sortOrder;
}
