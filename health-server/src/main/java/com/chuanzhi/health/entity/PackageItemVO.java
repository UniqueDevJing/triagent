package com.chuanzhi.health.entity;

import lombok.Data;

@Data
public class PackageItemVO {
    private Long id;
    private Long packageId;
    private Long examItemId;
    private Integer sortOrder;
    private String examItemName;
}
