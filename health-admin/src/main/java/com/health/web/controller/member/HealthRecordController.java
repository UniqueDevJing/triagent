package com.health.web.controller.member;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.HealthRecord;
import com.health.system.mapper.HealthRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/member/health-record")
public class HealthRecordController extends BaseController {

    private final HealthRecordMapper healthRecordMapper;

    public HealthRecordController(HealthRecordMapper healthRecordMapper) {
        this.healthRecordMapper = healthRecordMapper;
    }

    @GetMapping
    @Log(title = "健康档案查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) Long memberId,
                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<HealthRecord> qw = new LambdaQueryWrapper<HealthRecord>()
                .eq(memberId != null, HealthRecord::getMemberId, memberId)
                .orderByDesc(HealthRecord::getCreateTime);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(HealthRecord::getType, keyword).or().like(HealthRecord::getDoctorNotes, keyword));
        }
        Page<HealthRecord> p = healthRecordMapper.selectPage(new Page<>(page, pageSize), qw);
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        HealthRecord r = healthRecordMapper.selectById(id);
        return r != null ? success(r) : error("记录不存在");
    }

    @PostMapping
    @Log(title = "新增健康档案")
    public AjaxResult create(@RequestBody HealthRecord record) {
        healthRecordMapper.insert(record);
        return success(record);
    }

    @PutMapping("/{id}")
    @Log(title = "修改健康档案")
    public AjaxResult update(@PathVariable Long id, @RequestBody HealthRecord record) {
        record.setId(id);
        healthRecordMapper.updateById(record);
        return success();
    }

    @DeleteMapping("/{ids}")
    @Log(title = "删除健康档案")
    public AjaxResult delete(@PathVariable List<Long> ids) {
        healthRecordMapper.deleteBatchIds(ids);
        return success();
    }
}
