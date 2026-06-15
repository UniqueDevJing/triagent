package com.chuanzhi.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.dto.AssessmentRequest;
import com.chuanzhi.health.entity.AssessmentRecord;
import com.chuanzhi.health.entity.AssessmentTemplate;
import com.chuanzhi.health.service.AssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping("/templates")
    public Result<List<AssessmentTemplate>> listTemplates() {
        return Result.ok(assessmentService.listTemplates());
    }

    @GetMapping("/templates/{id}")
    public Result<AssessmentTemplate> getTemplate(@PathVariable Long id) {
        return Result.ok(assessmentService.getTemplate(id));
    }

    @PostMapping("/submit")
    public Result<AssessmentRecord> submit(@Valid @RequestBody AssessmentRequest request) {
        return Result.ok(assessmentService.submitAssessment(request));
    }

    @GetMapping("/records")
    public Result<PageResult<AssessmentRecord>> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId) {
        IPage<AssessmentRecord> result = assessmentService.pageRecords(page, size, userId);
        return Result.ok(PageResult.of(result));
    }

    @GetMapping("/records/{id}")
    public Result<AssessmentRecord> getRecord(@PathVariable Long id) {
        return Result.ok(assessmentService.getRecordDetail(id));
    }
}
