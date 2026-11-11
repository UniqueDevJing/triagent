package com.hospital.v2.controller;

import com.hospital.v2.common.Result;
import com.hospital.v2.service.MedicalRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 医生端：门诊接诊（查看挂到自己名下的患者）+ 病历七段结构化书写（草稿/提交）。 */
@RestController
@RequestMapping("/api/doctor")
public class DoctorController {
    private final MedicalRecordService records;

    public DoctorController(MedicalRecordService records) {
        this.records = records;
    }

    /** 我的接诊列表：挂号 + 患者档案 + 病历状态。 */
    @GetMapping("/registrations")
    public Result<?> registrations(@RequestParam long doctorId) {
        return Result.ok(records.doctorRegistrations(doctorId));
    }

    /** 保存病历草稿（七段结构化，可反复编辑）。 */
    @PostMapping("/record/save")
    public Result<?> saveDraft(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("recordId", records.saveDraft(body)));
    }

    /** 提交病历：草稿 → SUBMITTED 锁定；同时把挂号置为 DONE（完成就诊）。 */
    @PostMapping("/record/submit")
    public Result<?> submit(@RequestParam long recordId, @RequestParam long registrationId) {
        records.submit(recordId);
        return Result.ok();
    }

    /** 完成就诊（不写病历直接结诊）。 */
    @PostMapping("/registration/done")
    public Result<?> done(@RequestParam long registrationId) {
        records.finishVisit(registrationId);
        return Result.ok();
    }

    /** 病历详情。 */
    @GetMapping("/record")
    public Result<?> detail(@RequestParam long recordId) {
        return Result.ok(records.detail(recordId));
    }
}
