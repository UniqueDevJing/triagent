package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.EducationContent;
import com.chuanzhi.health.service.admin.EducationContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/education-contents")
@RequiredArgsConstructor
public class EducationContentController {

    private final EducationContentService service;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String type) {
        return Result.ok(service.list(page, size, keyword, type));
    }

    @GetMapping("/{id}")
    public Result<EducationContent> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @PostMapping
    public Result<EducationContent> create(@RequestBody EducationContent entity) {
        return Result.ok(service.create(entity));
    }

    @PutMapping("/{id}")
    public Result<EducationContent> update(@PathVariable Long id, @RequestBody EducationContent entity) {
        entity.setId(id);
        return Result.ok(service.update(entity));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok(null);
    }
}
