package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.ChronicDiseaseMgmt;
import com.chuanzhi.health.service.admin.ChronicDiseaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/chronic-diseases")
@RequiredArgsConstructor
public class ChronicDiseaseController {

    private final ChronicDiseaseService chronicDiseaseService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) Long memberId) {
        return Result.ok(chronicDiseaseService.list(page, size, memberId));
    }

    @GetMapping("/{id}")
    public Result<ChronicDiseaseMgmt> get(@PathVariable Long id) {
        return Result.ok(chronicDiseaseService.getById(id));
    }

    @PostMapping
    public Result<ChronicDiseaseMgmt> create(@RequestBody ChronicDiseaseMgmt entity) {
        return Result.ok(chronicDiseaseService.create(entity));
    }

    @PutMapping("/{id}")
    public Result<ChronicDiseaseMgmt> update(@PathVariable Long id, @RequestBody ChronicDiseaseMgmt entity) {
        entity.setId(id);
        return Result.ok(chronicDiseaseService.update(entity));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        chronicDiseaseService.delete(id);
        return Result.ok(null);
    }
}
