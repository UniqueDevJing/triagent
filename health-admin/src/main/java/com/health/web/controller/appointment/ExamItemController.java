package com.health.web.controller.appointment;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.ExamItem;
import com.health.system.mapper.ExamItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointment/exam-item")
public class ExamItemController extends BaseController {
    private final ExamItemMapper examItemMapper;
    public ExamItemController(ExamItemMapper examItemMapper) { this.examItemMapper = examItemMapper; }

    @GetMapping
    @Log(title = "检查项查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<ExamItem> p = examItemMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<ExamItem>().orderByDesc(ExamItem::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(examItemMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增检查项")
    public AjaxResult create(@RequestBody ExamItem examItem) { examItemMapper.insert(examItem); return success(examItem); }

    @PutMapping
    @Log(title = "修改检查项")
    public AjaxResult update(@RequestBody ExamItem examItem) { examItemMapper.updateById(examItem); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除检查项")
    public AjaxResult delete(@PathVariable List<Long> ids) { examItemMapper.deleteBatchIds(ids); return success(); }
}
