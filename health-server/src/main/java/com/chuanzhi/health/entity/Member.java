package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("members")
public class Member {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private Integer gender;
    private Integer age;
    private String idCard;
    private String phone;
    private String emergencyContact;
    private String emergencyPhone;
    private String bloodType;
    private BigDecimal height;
    private BigDecimal weight;
    private String medicalHistory;
    private String allergies;
    private String memberLevel;
    private Integer status;
    @TableLogic private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
