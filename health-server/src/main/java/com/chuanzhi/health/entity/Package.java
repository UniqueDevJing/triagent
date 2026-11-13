package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("packages")
public class Package {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String icon;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
    @TableField(exist = false) private List<PackageItem> items;
}
