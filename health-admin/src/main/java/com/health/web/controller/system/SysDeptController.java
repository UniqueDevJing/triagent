package com.health.web.controller.system;

import com.health.common.core.AjaxResult;
import com.health.common.core.BaseController;
import com.health.system.domain.SysDept;
import com.health.system.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/department")
public class SysDeptController extends BaseController {

    private final SysDeptMapper deptMapper;

    public SysDeptController(SysDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    @GetMapping
    public AjaxResult list() {
        List<SysDept> list = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getOrderNum));
        return success(list);
    }

    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) {
        return success(deptMapper.selectById(id));
    }

    @PostMapping
    public AjaxResult create(@RequestBody SysDept dept) {
        deptMapper.insert(dept);
        return success(dept);
    }

    @PutMapping("/{id}")
    public AjaxResult update(@RequestBody SysDept dept) {
        deptMapper.updateById(dept);
        return success();
    }

    @DeleteMapping("/{ids}")
    public AjaxResult delete(@PathVariable List<Long> ids) {
        deptMapper.deleteBatchIds(ids);
        return success();
    }
}
