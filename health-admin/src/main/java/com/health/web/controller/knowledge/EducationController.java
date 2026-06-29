package com.health.web.controller.knowledge;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.EducationContent;
import com.health.system.mapper.EducationContentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/education")
public class EducationController extends BaseController {
    private final EducationContentMapper educationContentMapper;
    public EducationController(EducationContentMapper educationContentMapper) { this.educationContentMapper = educationContentMapper; }

    @GetMapping
    @Log(title = "宣教内容查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<EducationContent> p = educationContentMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<EducationContent>().orderByDesc(EducationContent::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(educationContentMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增宣教内容")
    public AjaxResult create(@RequestBody EducationContent content) { educationContentMapper.insert(content); return success(content); }

    @PutMapping
    @Log(title = "修改宣教内容")
    public AjaxResult update(@RequestBody EducationContent content) { educationContentMapper.updateById(content); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除宣教内容")
    public AjaxResult delete(@PathVariable List<Long> ids) { educationContentMapper.deleteBatchIds(ids); return success(); }
}
