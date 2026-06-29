package com.health.web.controller.knowledge;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.RecipeLibrary;
import com.health.system.mapper.RecipeLibraryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/recipe")
public class RecipeController extends BaseController {
    private final RecipeLibraryMapper recipeLibraryMapper;
    public RecipeController(RecipeLibraryMapper recipeLibraryMapper) { this.recipeLibraryMapper = recipeLibraryMapper; }

    @GetMapping
    @Log(title = "食谱库查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<RecipeLibrary> p = recipeLibraryMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<RecipeLibrary>().orderByDesc(RecipeLibrary::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(recipeLibraryMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增食谱")
    public AjaxResult create(@RequestBody RecipeLibrary recipe) { recipeLibraryMapper.insert(recipe); return success(recipe); }

    @PutMapping
    @Log(title = "修改食谱")
    public AjaxResult update(@RequestBody RecipeLibrary recipe) { recipeLibraryMapper.updateById(recipe); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除食谱")
    public AjaxResult delete(@PathVariable List<Long> ids) { recipeLibraryMapper.deleteBatchIds(ids); return success(); }
}
