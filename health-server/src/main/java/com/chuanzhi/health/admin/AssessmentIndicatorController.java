package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.AssessmentIndicator;
import com.chuanzhi.health.service.admin.AssessmentIndicatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/assessment-indicators")
@RequiredArgsConstructor
public class AssessmentIndicatorController {

    private final AssessmentIndicatorService assessmentIndicatorService;

    @GetMapping
    public Result<PageResult<AssessmentIndicator>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        return Result.ok(assessmentIndicatorService.list(page, size, category));
    }

    @GetMapping("/all")
    public Result<List<AssessmentIndicator>> getAll() {
        return Result.ok(assessmentIndicatorService.getAll());
    }

    @GetMapping("/{id}")
    public Result<AssessmentIndicator> get(@PathVariable Long id) {
        return Result.ok(assessmentIndicatorService.getById(id));
    }

    @PostMapping
    public Result<AssessmentIndicator> create(@RequestBody AssessmentIndicator indicator) {
        return Result.ok(assessmentIndicatorService.create(indicator));
    }

    @PutMapping("/{id}")
    public Result<AssessmentIndicator> update(@PathVariable Long id, @RequestBody AssessmentIndicator indicator) {
        indicator.setId(id);
        return Result.ok(assessmentIndicatorService.update(indicator));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        assessmentIndicatorService.delete(id);
        return Result.ok(null);
    }
}
