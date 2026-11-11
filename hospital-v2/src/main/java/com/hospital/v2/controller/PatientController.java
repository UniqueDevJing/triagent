package com.hospital.v2.controller;

import com.hospital.v2.common.Result;
import com.hospital.v2.domain.TriageResult;
import com.hospital.v2.service.MedicalRecordService;
import com.hospital.v2.service.PreTriageService;
import com.hospital.v2.service.ReActTriageAgent;
import com.hospital.v2.service.RegistrationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** 患者端：智能预问诊（SSE 流式）、发起分诊、预约挂号、病历查看。 */
@RestController
@RequestMapping("/api/patient")
public class PatientController {
    private final PreTriageService preTriage;
    private final ReActTriageAgent agent;
    private final RegistrationService registration;
    private final MedicalRecordService records;
    private final com.hospital.v2.dao.Db db;

    public PatientController(PreTriageService preTriage, ReActTriageAgent agent,
                             RegistrationService registration, MedicalRecordService records,
                             com.hospital.v2.dao.Db db) {
        this.preTriage = preTriage;
        this.agent = agent;
        this.registration = registration;
        this.records = records;
        this.db = db;
    }

    @PostMapping("/session")
    public Result<?> session(@RequestParam(required = false) Long patientId) {
        return Result.ok(Map.of("sessionId", preTriage.startSession(patientId)));
    }

    /** 预问诊多轮 SSE 真流式。 */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam long sessionId, @RequestParam(required = false) String msg) {
        SseEmitter em = new SseEmitter(60000L);
        CompletableFuture.runAsync(() -> {
            try {
                if (msg != null && !msg.isBlank()) preTriage.appendUser(sessionId, msg);
                String reply = preTriage.generateReply(sessionId, msg == null ? "" : msg);
                for (String chunk : chunk(reply)) {
                    em.send(SseEmitter.event().data(chunk));
                    Thread.sleep(35);
                }
                em.send(SseEmitter.event().data("[DONE]"));
                em.complete();
            } catch (Exception e) {
                em.completeWithError(e);
            }
        });
        return em;
    }

    /** 发起 ReAct 分诊并锁定会话。 */
    @PostMapping("/triage")
    public Result<?> triage(@RequestParam long sessionId,
                            @RequestParam(required = false) Long patientId,
                            @RequestParam String complaint) {
        TriageResult r = agent.triage(sessionId, patientId, complaint);
        preTriage.lock(sessionId, "分诊完成，建议科室(id=" + r.suggestion().get("department_id") + ")");
        return Result.ok(r.suggestion());
    }

    @PostMapping("/register")
    public Result<?> register(@RequestParam long suggestionId, @RequestParam Long patientId) {
        return Result.ok(registration.register(suggestionId, patientId));
    }

    @GetMapping("/registrations")
    public Result<?> registrations(@RequestParam Long patientId) {
        return Result.ok(registration.myRegistrations(patientId));
    }

    /** 新建病历草稿（七段结构化，任意段落可为空，可继续编辑）。 */
    @PostMapping("/record")
    public Result<?> record(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("recordId", records.saveDraft(body)));
    }

    /** 提交病历：草稿 → SUBMITTED 锁定。 */
    @PostMapping("/record/submit")
    public Result<?> sign(@RequestParam long recordId) {
        records.submit(recordId);
        return Result.ok();
    }

    @GetMapping("/records")
    public Result<?> records(@RequestParam Long patientId) {
        return Result.ok(records.myRecords(patientId));
    }

    // ---- 个人档案（查看/维护性别、年龄、慢病史、过敏史） ----
    @GetMapping("/profile")
    public Result<?> profile(@RequestParam Long patientId) {
        return Result.ok(db.queryOne("SELECT * FROM patient_profile WHERE id=:id", Map.of("id", patientId)));
    }

    @PostMapping("/profile/save")
    public Result<?> profileSave(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        if (id == null) return Result.fail(400, "缺少患者 id");
        db.update("UPDATE patient_profile SET name=:name, gender=:gender, age=:age, "
                + "allergic_history=:allergic_history, past_history=:past_history WHERE id=:id", body);
        return Result.ok();
    }

    private List<String> chunk(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        int step = 8;
        for (int i = 0; i < text.length(); i += step) {
            out.add(text.substring(i, Math.min(i + step, text.length())));
        }
        return out;
    }
}
