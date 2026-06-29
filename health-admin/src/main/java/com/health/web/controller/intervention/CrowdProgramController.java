package com.health.web.controller.intervention;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.CrowdProgram;
import com.health.system.mapper.CrowdProgramMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/intervention/crowd")
public class CrowdProgramController extends BaseController {
    private final CrowdProgramMapper crowdProgramMapper;
    public CrowdProgramController(CrowdProgramMapper crowdProgramMapper) { this.crowdProgramMapper = crowdProgramMapper; }

    @GetMapping
    @Log(title = "人群方案查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<CrowdProgram> p = crowdProgramMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<CrowdProgram>().orderByDesc(CrowdProgram::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(crowdProgramMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增人群方案")
    public AjaxResult create(@RequestBody CrowdProgram program) { crowdProgramMapper.insert(program); return success(program); }

    @PutMapping
    @Log(title = "修改人群方案")
    public AjaxResult update(@RequestBody CrowdProgram program) { crowdProgramMapper.updateById(program); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除人群方案")
    public AjaxResult delete(@PathVariable List<Long> ids) { crowdProgramMapper.deleteBatchIds(ids); return success(); }
}
