package com.chuanzhi.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.dto.AssessmentRequest;
import com.chuanzhi.health.entity.AssessmentRecord;
import com.chuanzhi.health.entity.AssessmentTemplate;
import com.chuanzhi.health.service.AssessmentService;
import com.chuanzhi.health.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "健康评估", description = "评估量表管理与评估提交评分")
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final SseService sseService;

    @Operation(summary = "获取评估量表列表")
    @GetMapping("/templates")
    public Result<List<AssessmentTemplate>> listTemplates() {
        return Result.ok(assessmentService.listTemplates());
    }

    @Operation(summary = "获取评估量表详情")
    @GetMapping("/templates/{id}")
    public Result<AssessmentTemplate> getTemplate(@PathVariable Long id) {
        return Result.ok(assessmentService.getTemplate(id));
    }

    @Operation(summary = "提交评估答卷")
    @PostMapping("/submit")
    public Result<AssessmentRecord> submit(@Valid @RequestBody AssessmentRequest request) {
        AssessmentRecord record = assessmentService.submitAssessment(request);
        sseService.broadcast("assessments", "assessment_submitted", record);
        sseService.broadcast("dashboard", "assessment_submitted", record);
        return Result.ok(record);
    }

    @Operation(summary = "分页查询评估记录")
    @GetMapping("/records")
    public Result<PageResult<AssessmentRecord>> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId) {
        IPage<AssessmentRecord> result = assessmentService.pageRecords(page, size, userId);
        return Result.ok(PageResult.of(result));
    }

    @Operation(summary = "获取评估记录详情")
    @GetMapping("/records/{id}")
    public Result<AssessmentRecord> getRecord(@PathVariable Long id) {
        return Result.ok(assessmentService.getRecordDetail(id));
    }
}
