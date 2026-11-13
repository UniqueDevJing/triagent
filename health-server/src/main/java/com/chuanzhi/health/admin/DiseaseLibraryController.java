package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.DiseaseLibrary;
import com.chuanzhi.health.service.admin.DiseaseLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/disease-library")
@RequiredArgsConstructor
public class DiseaseLibraryController {

    private final DiseaseLibraryService service;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String category) {
        return Result.ok(service.list(page, size, keyword, category));
    }

    @GetMapping("/{id}")
    public Result<DiseaseLibrary> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @PostMapping
    public Result<DiseaseLibrary> create(@RequestBody DiseaseLibrary entity) {
        return Result.ok(service.create(entity));
    }

    @PutMapping("/{id}")
    public Result<DiseaseLibrary> update(@PathVariable Long id, @RequestBody DiseaseLibrary entity) {
        entity.setId(id);
        return Result.ok(service.update(entity));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok(null);
    }
}
