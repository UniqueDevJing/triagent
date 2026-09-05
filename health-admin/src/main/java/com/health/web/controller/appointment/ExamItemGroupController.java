package com.health.web.controller.appointment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.common.annotation.Log;
import com.health.common.core.AjaxResult;
import com.health.common.core.BaseController;
import com.health.system.domain.ExamItemGroup;
import com.health.system.mapper.ExamItemGroupMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 体检项目组/分类 CRUD（对应前端 ExamItemList/PackageList 的分类页签）。
 * 分页参数与前端约定保持一致：page + size（records/total 信封）。
 */
@RestController
@RequestMapping("/api/v1/appointment/exam-item-group")
public class ExamItemGroupController extends BaseController {

    private final ExamItemGroupMapper groupMapper;

    public ExamItemGroupController(ExamItemGroupMapper groupMapper) {
        this.groupMapper = groupMapper;
    }

    @GetMapping
    @Log(title = "项目组查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int size) {
        Page<ExamItemGroup> p = groupMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ExamItemGroup>().orderByAsc(ExamItemGroup::getSortOrder));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        return success(groupMapper.selectById(id));
    }

    @PostMapping
    @Log(title = "新增项目组")
    public AjaxResult create(@RequestBody ExamItemGroup group) {
        if (group.getName() == null || group.getName().isBlank()) {
            return error("名称不能为空");
        }
        groupMapper.insert(group);
        return success(group);
    }

    @PutMapping("/{id}")
    @Log(title = "修改项目组")
    public AjaxResult update(@PathVariable Long id, @RequestBody ExamItemGroup group) {
        group.setId(id);
        groupMapper.updateById(group);
        return success();
    }

    @DeleteMapping("/{ids}")
    @Log(title = "删除项目组")
    public AjaxResult delete(@PathVariable List<Long> ids) {
        groupMapper.deleteBatchIds(ids);
        return success();
    }
}
