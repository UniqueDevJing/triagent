package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String userName;
    private String nickName;
    private String password;
    private String email;
    private String phoneNumber;
    private String sex;
    private String avatar;
    private String status;
    private Long deptId;
    private String loginIp;
    private java.time.LocalDateTime loginDate;
    private String remark;
}
