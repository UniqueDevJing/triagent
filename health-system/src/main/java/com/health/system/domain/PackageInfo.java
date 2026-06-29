package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("package_info")
public class PackageInfo extends BaseEntity {
    private String packageName;
    private String description;
    private BigDecimal price;
    private String suitableFor;
    private String status;
}
