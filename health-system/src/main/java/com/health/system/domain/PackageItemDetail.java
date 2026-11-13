package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("package_item_detail")
public class PackageItemDetail extends BaseEntity {
    private Long packageId;
    private Long itemId;
    private Integer sortOrder;
}
