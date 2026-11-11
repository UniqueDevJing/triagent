package com.hospital.v2.controller;

import com.hospital.v2.common.Result;
import com.hospital.v2.service.MedicalRecordService;
import com.hospital.v2.security.AuthPrincipal;
import com.hospital.v2.security.CurrentUserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 医生端：门诊接诊（查看挂到自己名下的患者）+ 病历七段结构化书写（草稿/提交）。 */
@RestController
@RequestMapping("/api/doctor")
public class DoctorController {
    private final MedicalRecordService records;
    private final CurrentUserService currentUser;

    public DoctorController(MedicalRecordService records, CurrentUserService currentUser) {
        this.records = records;
        this.currentUser = currentUser;
    }

    /** 我的接诊列表：挂号 + 患者档案 + 病历状态。 */
    @GetMapping("/registrations")
    public Result<?> registrations(@RequestParam(required = false) Long doctorId,
                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return Result.ok(records.doctorRegistrations(currentUser.doctorId(principal, doctorId)));
    }

    /** 保存病历草稿（七段结构化，可反复编辑）。 */
    @PostMapping("/record/save")
    public Result<?> saveDraft(@RequestBody Map<String, Object> body,
                               @AuthenticationPrincipal AuthPrincipal principal) {
        Map<String, Object> safe = new java.util.LinkedHashMap<>(body);
        safe.put("doctor_id", currentUser.doctorId(principal,
                body.get("doctor_id") instanceof Number n ? n.longValue() : null));
        return Result.ok(Map.of("recordId", records.saveDraft(safe)));
    }

    /** 提交病历：草稿 → SUBMITTED 锁定；同时把挂号置为 DONE（完成就诊）。 */
    @PostMapping("/record/submit")
    public Result<?> submit(@RequestParam long recordId, @RequestParam long registrationId,
                            @AuthenticationPrincipal AuthPrincipal principal) {
        currentUser.assertDoctorOwnsRecord(principal, recordId);
        currentUser.assertDoctorOwnsRegistration(principal, registrationId);
        records.submit(recordId);
        return Result.ok();
    }

    /** 完成就诊（不写病历直接结诊）。 */
    @PostMapping("/registration/done")
    public Result<?> done(@RequestParam long registrationId, @AuthenticationPrincipal AuthPrincipal principal) {
        currentUser.assertDoctorOwnsRegistration(principal, registrationId);
        records.finishVisit(registrationId);
        return Result.ok();
    }

    /** 病历详情。 */
    @GetMapping("/record")
    public Result<?> detail(@RequestParam long recordId, @AuthenticationPrincipal AuthPrincipal principal) {
        currentUser.assertDoctorOwnsRecord(principal, recordId);
        return Result.ok(records.detail(recordId));
    }
}
