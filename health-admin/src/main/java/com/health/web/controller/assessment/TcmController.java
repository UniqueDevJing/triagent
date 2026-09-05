package com.health.web.controller.assessment;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.TcmConstitution;
import com.health.system.mapper.TcmConstitutionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessment/tcm")
public class TcmController extends BaseController {
    private final TcmConstitutionMapper tcmConstitutionMapper;
    public TcmController(TcmConstitutionMapper tcmConstitutionMapper) { this.tcmConstitutionMapper = tcmConstitutionMapper; }

    @GetMapping
    @Log(title = "中医体质查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<TcmConstitution> p = tcmConstitutionMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<TcmConstitution>().orderByDesc(TcmConstitution::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(tcmConstitutionMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增中医体质")
    public AjaxResult create(@RequestBody TcmConstitution tcm) { tcmConstitutionMapper.insert(tcm); return success(tcm); }

    @PutMapping("/{id}")
    @Log(title = "修改中医体质")
    public AjaxResult update(@RequestBody TcmConstitution tcm) { tcmConstitutionMapper.updateById(tcm); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除中医体质")
    public AjaxResult delete(@PathVariable List<Long> ids) { tcmConstitutionMapper.deleteBatchIds(ids); return success(); }
}
