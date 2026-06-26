package com.chuanzhi.health.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.AssessmentRecord;
import com.chuanzhi.health.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/assessment-records")
@RequiredArgsConstructor
public class AssessmentRecordController {

    private final AssessmentService assessmentService;

    @GetMapping
    public Result<PageResult<AssessmentRecord>> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String type) {
        IPage<AssessmentRecord> result = assessmentService.listRecords(page, size, memberId, type);
        return Result.ok(PageResult.of(result));
    }

    @GetMapping("/{id}")
    public Result<AssessmentRecord> getRecord(@PathVariable Long id) {
        return Result.ok(assessmentService.getRecordDetail(id));
    }

    @PostMapping
    public Result<AssessmentRecord> createRecord(@RequestBody AssessmentRecord record) {
        return Result.ok(assessmentService.createRecord(record));
    }
}
