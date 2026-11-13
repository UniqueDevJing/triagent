package com.health.web.controller.appointment;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.PackageInfo;
import com.health.system.domain.PackageItemDetail;
import com.health.system.mapper.PackageInfoMapper;
import com.health.system.mapper.PackageItemDetailMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointment/package")
public class PackageController extends BaseController {
    private final PackageInfoMapper packageInfoMapper;
    private final PackageItemDetailMapper packageItemDetailMapper;
    public PackageController(PackageInfoMapper packageInfoMapper, PackageItemDetailMapper packageItemDetailMapper) {
        this.packageInfoMapper = packageInfoMapper;
        this.packageItemDetailMapper = packageItemDetailMapper;
    }

    @GetMapping
    @Log(title = "套餐查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<PackageInfo> p = packageInfoMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<PackageInfo>().orderByDesc(PackageInfo::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(packageInfoMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增套餐")
    public AjaxResult create(@RequestBody PackageInfo packageInfo) { packageInfoMapper.insert(packageInfo); return success(packageInfo); }

    @PutMapping("/{id}")
    @Log(title = "修改套餐")
    public AjaxResult update(@RequestBody PackageInfo packageInfo) { packageInfoMapper.updateById(packageInfo); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除套餐")
    public AjaxResult delete(@PathVariable List<Long> ids) { packageInfoMapper.deleteBatchIds(ids); return success(); }

    @GetMapping("/{id}/items")
    public AjaxResult getItems(@PathVariable Long id) {
        return success(packageItemDetailMapper.selectList(
                new LambdaQueryWrapper<PackageItemDetail>().eq(PackageItemDetail::getPackageId, id)));
    }
}
