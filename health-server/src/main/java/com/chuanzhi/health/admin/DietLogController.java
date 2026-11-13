package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.DietLog;
import com.chuanzhi.health.service.admin.DietLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/diet-logs")
@RequiredArgsConstructor
public class DietLogController {

    private final DietLogService dietLogService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) Long memberId,
                          @RequestParam(required = false) LocalDate recordedDate) {
        return Result.ok(dietLogService.list(page, size, memberId, recordedDate));
    }

    @GetMapping("/{id}")
    public Result<DietLog> get(@PathVariable Long id) {
        return Result.ok(dietLogService.getById(id));
    }

    @PostMapping
    public Result<DietLog> create(@RequestBody DietLog entity) {
        return Result.ok(dietLogService.create(entity));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        dietLogService.delete(id);
        return Result.ok(null);
    }
}
