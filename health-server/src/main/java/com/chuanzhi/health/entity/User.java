package com.chuanzhi.health.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 200, message = "密码长度6-200字符")
    private String password;

    @NotBlank(message = "角色不能为空")
    private String role;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名最长50字符")
    private String name;

    @NotNull(message = "性别不能为空")
    @Min(value = 0, message = "性别值无效")
    @Max(value = 2, message = "性别值无效")
    private Integer gender;

    @Min(value = 0, message = "年龄不能为负")
    @Max(value = 150, message = "年龄值无效")
    private Integer age;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 200, message = "地址最长200字符")
    private String address;

    private String emergencyContact;
    private String emergencyPhone;
    private String bloodType;
    private BigDecimal height;
    private BigDecimal weight;
    private String medicalHistory;
    private String allergies;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
