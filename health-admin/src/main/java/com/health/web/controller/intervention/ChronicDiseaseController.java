package com.health.web.controller.intervention;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.ChronicDisease;
import com.health.system.mapper.ChronicDiseaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/intervention/chronic")
public class ChronicDiseaseController extends BaseController {
    private final ChronicDiseaseMapper chronicDiseaseMapper;
    public ChronicDiseaseController(ChronicDiseaseMapper chronicDiseaseMapper) { this.chronicDiseaseMapper = chronicDiseaseMapper; }

    @GetMapping
    @Log(title = "慢性病查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<ChronicDisease> p = chronicDiseaseMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<ChronicDisease>().orderByDesc(ChronicDisease::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(chronicDiseaseMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增慢性病")
    public AjaxResult create(@RequestBody ChronicDisease disease) { chronicDiseaseMapper.insert(disease); return success(disease); }

    @PutMapping
    @Log(title = "修改慢性病")
    public AjaxResult update(@RequestBody ChronicDisease disease) { chronicDiseaseMapper.updateById(disease); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除慢性病")
    public AjaxResult delete(@PathVariable List<Long> ids) { chronicDiseaseMapper.deleteBatchIds(ids); return success(); }
}
