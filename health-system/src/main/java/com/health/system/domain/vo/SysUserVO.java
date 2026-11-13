package com.health.system.domain.vo;

import com.health.system.domain.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserVO extends SysUser {
    private String role;
    private String roleName;
}
