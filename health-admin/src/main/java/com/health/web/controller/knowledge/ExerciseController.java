package com.health.web.controller.knowledge;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.ExerciseLibrary;
import com.health.system.mapper.ExerciseLibraryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/exercise")
public class ExerciseController extends BaseController {
    private final ExerciseLibraryMapper exerciseLibraryMapper;
    public ExerciseController(ExerciseLibraryMapper exerciseLibraryMapper) { this.exerciseLibraryMapper = exerciseLibraryMapper; }

    @GetMapping
    @Log(title = "运动库查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<ExerciseLibrary> p = exerciseLibraryMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<ExerciseLibrary>().orderByDesc(ExerciseLibrary::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(exerciseLibraryMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增运动")
    public AjaxResult create(@RequestBody ExerciseLibrary exercise) { exerciseLibraryMapper.insert(exercise); return success(exercise); }

    @PutMapping("/{id}")
    @Log(title = "修改运动")
    public AjaxResult update(@RequestBody ExerciseLibrary exercise) { exerciseLibraryMapper.updateById(exercise); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除运动")
    public AjaxResult delete(@PathVariable List<Long> ids) { exerciseLibraryMapper.deleteBatchIds(ids); return success(); }
}
