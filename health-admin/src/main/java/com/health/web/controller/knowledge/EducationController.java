package com.health.web.controller.knowledge;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.EducationContent;
import com.health.system.domain.EducationWord;
import com.health.system.mapper.EducationContentMapper;
import com.health.system.mapper.EducationWordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/education")
public class EducationController extends BaseController {
    private final EducationContentMapper educationContentMapper;
    private final EducationWordMapper educationWordMapper;

    public EducationController(EducationContentMapper educationContentMapper,
                               EducationWordMapper educationWordMapper) {
        this.educationContentMapper = educationContentMapper;
        this.educationWordMapper = educationWordMapper;
    }

    // ========== 宣教内容 ==========
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

    @PutMapping("/{id}")
    @Log(title = "修改宣教内容")
    public AjaxResult update(@RequestBody EducationContent content) { educationContentMapper.updateById(content); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除宣教内容")
    public AjaxResult delete(@PathVariable List<Long> ids) { educationContentMapper.deleteBatchIds(ids); return success(); }

    // ========== 宣教词 ==========
    @GetMapping("/word")
    @Log(title = "宣教词查询")
    public AjaxResult listWord(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int pageSize,
                               @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<EducationWord> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(EducationWord::getTerm, keyword).or().like(EducationWord::getDefinition, keyword));
        }
        qw.orderByDesc(EducationWord::getCreateTime);
        Page<EducationWord> p = educationWordMapper.selectPage(new Page<>(page, pageSize), qw);
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/word/{id}")
    public AjaxResult getWordById(@PathVariable Long id) { return success(educationWordMapper.selectById(id)); }

    @PostMapping("/word")
    @Log(title = "新增宣教词")
    public AjaxResult createWord(@RequestBody EducationWord word) { educationWordMapper.insert(word); return success(word); }

    @PutMapping("/word/{id}")
    @Log(title = "修改宣教词")
    public AjaxResult updateWord(@RequestBody EducationWord word) { educationWordMapper.updateById(word); return success(); }

    @DeleteMapping("/word/{ids}")
    @Log(title = "删除宣教词")
    public AjaxResult deleteWord(@PathVariable List<Long> ids) { educationWordMapper.deleteBatchIds(ids); return success(); }
}
