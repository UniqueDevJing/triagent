package com.health.web.controller.intervention;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.DietLog;
import com.health.system.mapper.DietLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/intervention/diet")
public class DietLogController extends BaseController {
    private final DietLogMapper dietLogMapper;
    public DietLogController(DietLogMapper dietLogMapper) { this.dietLogMapper = dietLogMapper; }

    @GetMapping
    @Log(title = "饮食记录查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<DietLog> p = dietLogMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<DietLog>().orderByDesc(DietLog::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(dietLogMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增饮食记录")
    public AjaxResult create(@RequestBody DietLog dietLog) { dietLogMapper.insert(dietLog); return success(dietLog); }

    @PutMapping
    @Log(title = "修改饮食记录")
    public AjaxResult update(@RequestBody DietLog dietLog) { dietLogMapper.updateById(dietLog); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除饮食记录")
    public AjaxResult delete(@PathVariable List<Long> ids) { dietLogMapper.deleteBatchIds(ids); return success(); }
}
