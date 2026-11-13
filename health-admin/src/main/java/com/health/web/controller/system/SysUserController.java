package com.health.web.controller.system;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.SysRole;
import com.health.system.domain.SysUser;
import com.health.system.domain.SysUserRole;
import com.health.system.domain.vo.SysUserVO;
import com.health.system.mapper.SysRoleMapper;
import com.health.system.mapper.SysUserMapper;
import com.health.system.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/system/user")
public class SysUserController extends BaseController {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public SysUserController(SysUserMapper userMapper,
                             SysUserRoleMapper userRoleMapper,
                             SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @GetMapping
    @Log(title = "用户查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getCreateTime));
        List<SysUserVO> vos = toVOList(p.getRecords());
        return toPage(p.getTotal(), vos);
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return error("用户不存在");
        return success(toVO(user));
    }

    @PostMapping
    @Log(title = "新增用户")
    public AjaxResult create(@RequestBody SysUser user) {
        userMapper.insert(user);
        return success(user);
    }

    @PutMapping("/{id}")
    @Log(title = "修改用户")
    public AjaxResult update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userMapper.updateById(user);
        return success();
    }

    @DeleteMapping("/{ids}")
    @Log(title = "删除用户")
    public AjaxResult delete(@PathVariable List<Long> ids) {
        userMapper.deleteBatchIds(ids);
        return success();
    }

    private List<SysUserVO> toVOList(List<SysUser> users) {
        if (users.isEmpty()) return List.of();

        List<Long> userIds = users.stream().map(SysUser::getId).toList();

        // 查询所有用户-角色关联
        List<SysUserRole> allUserRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
        Map<Long, Long> userRoleMap = allUserRoles.stream()
                .collect(Collectors.toMap(SysUserRole::getUserId, SysUserRole::getRoleId, (a, b) -> a));

        // 查询所有角色
        List<Long> roleIds = allUserRoles.stream().map(SysUserRole::getRoleId).distinct().toList();
        Map<Long, SysRole> roleMap = roleIds.isEmpty() ? Map.of() :
                roleMapper.selectBatchIds(roleIds).stream()
                        .collect(Collectors.toMap(SysRole::getId, Function.identity()));

        return users.stream().map(u -> {
            SysUserVO vo = new SysUserVO();
            vo.setId(u.getId());
            vo.setUserName(u.getUserName());
            vo.setNickName(u.getNickName());
            vo.setEmail(u.getEmail());
            vo.setPhoneNumber(u.getPhoneNumber());
            vo.setSex(u.getSex());
            vo.setAvatar(u.getAvatar());
            vo.setStatus(u.getStatus());
            vo.setDeptId(u.getDeptId());
            vo.setLoginIp(u.getLoginIp());
            vo.setLoginDate(u.getLoginDate());
            vo.setRemark(u.getRemark());
            vo.setCreateTime(u.getCreateTime());
            vo.setUpdateTime(u.getUpdateTime());

            Long roleId = userRoleMap.get(u.getId());
            if (roleId != null) {
                SysRole role = roleMap.get(roleId);
                if (role != null) {
                    vo.setRole(role.getRoleKey());
                    vo.setRoleName(role.getRoleName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private SysUserVO toVO(SysUser u) {
        return toVOList(List.of(u)).get(0);
    }
}
