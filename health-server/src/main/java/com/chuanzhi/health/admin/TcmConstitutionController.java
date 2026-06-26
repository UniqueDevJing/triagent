package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.TcmConstitution;
import com.chuanzhi.health.service.admin.TcmConstitutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tcm-constitutions")
@RequiredArgsConstructor
public class TcmConstitutionController {

    private final TcmConstitutionService tcmConstitutionService;

    @GetMapping
    public Result<PageResult<TcmConstitution>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(tcmConstitutionService.list(page, size));
    }

    @GetMapping("/{id}")
    public Result<TcmConstitution> get(@PathVariable Long id) {
        return Result.ok(tcmConstitutionService.getById(id));
    }

    @PostMapping
    public Result<TcmConstitution> create(@RequestBody TcmConstitution constitution) {
        return Result.ok(tcmConstitutionService.create(constitution));
    }

    @PutMapping("/{id}")
    public Result<TcmConstitution> update(@PathVariable Long id, @RequestBody TcmConstitution constitution) {
        constitution.setId(id);
        return Result.ok(tcmConstitutionService.update(constitution));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        tcmConstitutionService.delete(id);
        return Result.ok(null);
    }
}
