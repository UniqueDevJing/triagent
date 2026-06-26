package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.EducationWord;
import com.chuanzhi.health.service.admin.EducationWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/education-words")
@RequiredArgsConstructor
public class EducationWordController {

    private final EducationWordService service;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(service.list(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<EducationWord> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @PostMapping
    public Result<EducationWord> create(@RequestBody EducationWord entity) {
        return Result.ok(service.create(entity));
    }

    @PutMapping("/{id}")
    public Result<EducationWord> update(@PathVariable Long id, @RequestBody EducationWord entity) {
        entity.setId(id);
        return Result.ok(service.update(entity));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok(null);
    }
}
