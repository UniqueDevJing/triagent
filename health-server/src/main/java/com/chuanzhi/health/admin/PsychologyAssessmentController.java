package com.chuanzhi.health.admin;

import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.PsychologyAssessment;
import com.chuanzhi.health.service.admin.PsychologyAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/psychology-assessments")
@RequiredArgsConstructor
public class PsychologyAssessmentController {

    private final PsychologyAssessmentService psychologyAssessmentService;

    @GetMapping
    public Result<PageResult<PsychologyAssessment>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(psychologyAssessmentService.list(page, size));
    }

    @GetMapping("/{id}")
    public Result<PsychologyAssessment> get(@PathVariable Long id) {
        return Result.ok(psychologyAssessmentService.getById(id));
    }

    @PostMapping
    public Result<PsychologyAssessment> create(@RequestBody PsychologyAssessment assessment) {
        return Result.ok(psychologyAssessmentService.create(assessment));
    }

    @PutMapping("/{id}")
    public Result<PsychologyAssessment> update(@PathVariable Long id, @RequestBody PsychologyAssessment assessment) {
        assessment.setId(id);
        return Result.ok(psychologyAssessmentService.update(assessment));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        psychologyAssessmentService.delete(id);
        return Result.ok(null);
    }
}
