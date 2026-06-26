package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.HealthRecipe;
import com.chuanzhi.health.service.admin.HealthRecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/health-recipes")
@RequiredArgsConstructor
public class HealthRecipeController {

    private final HealthRecipeService service;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String category) {
        return Result.ok(service.list(page, size, keyword, category));
    }

    @GetMapping("/{id}")
    public Result<HealthRecipe> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @PostMapping
    public Result<HealthRecipe> create(@RequestBody HealthRecipe entity) {
        return Result.ok(service.create(entity));
    }

    @PutMapping("/{id}")
    public Result<HealthRecipe> update(@PathVariable Long id, @RequestBody HealthRecipe entity) {
        entity.setId(id);
        return Result.ok(service.update(entity));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok(null);
    }
}
