package com.health.web.controller.knowledge;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.DiseaseLibrary;
import com.health.system.mapper.DiseaseLibraryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/disease")
public class DiseaseController extends BaseController {
    private final DiseaseLibraryMapper diseaseLibraryMapper;
    public DiseaseController(DiseaseLibraryMapper diseaseLibraryMapper) { this.diseaseLibraryMapper = diseaseLibraryMapper; }

    @GetMapping
    @Log(title = "疾病库查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<DiseaseLibrary> p = diseaseLibraryMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<DiseaseLibrary>().orderByDesc(DiseaseLibrary::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(diseaseLibraryMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增疾病")
    public AjaxResult create(@RequestBody DiseaseLibrary disease) { diseaseLibraryMapper.insert(disease); return success(disease); }

    @PutMapping("/{id}")
    @Log(title = "修改疾病")
    public AjaxResult update(@RequestBody DiseaseLibrary disease) { diseaseLibraryMapper.updateById(disease); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除疾病")
    public AjaxResult delete(@PathVariable List<Long> ids) { diseaseLibraryMapper.deleteBatchIds(ids); return success(); }
}
