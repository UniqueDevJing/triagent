package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.CrowdProgram;
import com.chuanzhi.health.service.admin.CrowdProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/crowd-programs")
@RequiredArgsConstructor
public class CrowdProgramController {

    private final CrowdProgramService crowdProgramService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String name) {
        return Result.ok(crowdProgramService.list(page, size, name));
    }

    @GetMapping("/{id}")
    public Result<CrowdProgram> get(@PathVariable Long id) {
        return Result.ok(crowdProgramService.getById(id));
    }

    @PostMapping
    public Result<CrowdProgram> create(@RequestBody CrowdProgram entity) {
        return Result.ok(crowdProgramService.create(entity));
    }

    @PutMapping("/{id}")
    public Result<CrowdProgram> update(@PathVariable Long id, @RequestBody CrowdProgram entity) {
        entity.setId(id);
        return Result.ok(crowdProgramService.update(entity));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        crowdProgramService.delete(id);
        return Result.ok(null);
    }
}
