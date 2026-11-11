package com.hospital.v2.controller;

import com.hospital.v2.common.Result;
import com.hospital.v2.domain.TriageResult;
import com.hospital.v2.service.MedicalRecordService;
import com.hospital.v2.service.PreTriageService;
import com.hospital.v2.service.ReActTriageAgent;
import com.hospital.v2.service.RegistrationService;
import com.hospital.v2.security.AuthPrincipal;
import com.hospital.v2.security.CurrentUserService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final CurrentUserService currentUser;

    public PatientController(PreTriageService preTriage, ReActTriageAgent agent,
                             RegistrationService registration, MedicalRecordService records,
                             com.hospital.v2.dao.Db db, CurrentUserService currentUser) {
        this.preTriage = preTriage;
        this.agent = agent;
        this.registration = registration;
        this.records = records;
        this.db = db;
        this.currentUser = currentUser;
    }

    @PostMapping("/session")
    public Result<?> session(@RequestParam(required = false) Long patientId,
                             @AuthenticationPrincipal AuthPrincipal principal) {
        long ownPatientId = currentUser.patientId(principal, patientId);
        return Result.ok(Map.of("sessionId", preTriage.startSession(ownPatientId)));
    }

    /** 预问诊多轮 SSE 真流式。 */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam long sessionId, @RequestParam(required = false) String msg,
                           @AuthenticationPrincipal AuthPrincipal principal) {
        currentUser.assertPatientOwnsSession(principal, sessionId);
        SseEmitter em = new SseEmitter(60000L);
        CompletableFuture.runAsync(() -> {
            try {
                if (msg != null && !msg.isBlank()) preTriage.appendUser(sessionId, msg);
                boolean realStreaming = preTriage.streamReply(sessionId, msg == null ? "" : msg,
                        token -> send(em, token),
                        full -> { send(em, "[DONE]"); em.complete(); },
                        em::completeWithError);
                if (realStreaming) return;
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

    private void send(SseEmitter emitter, String data) {
        try { emitter.send(SseEmitter.event().data(data)); }
        catch (java.io.IOException e) { emitter.completeWithError(e); }
    }

    /** 发起 ReAct 分诊并锁定会话。 */
    @PostMapping("/triage")
    public Result<?> triage(@RequestParam long sessionId,
                            @RequestParam(required = false) Long patientId,
                            @RequestParam String complaint,
                            @AuthenticationPrincipal AuthPrincipal principal) {
        long ownPatientId = currentUser.patientId(principal, patientId);
        currentUser.assertPatientOwnsSession(principal, sessionId);
        TriageResult r = agent.triage(sessionId, ownPatientId, complaint);
        preTriage.lock(sessionId, "分诊完成，建议科室(id=" + r.suggestion().get("department_id") + ")");
        return Result.ok(r.suggestion());
    }

    @PostMapping("/register")
    public Result<?> register(@RequestParam long suggestionId, @RequestParam(required = false) Long patientId,
                              @AuthenticationPrincipal AuthPrincipal principal) {
        currentUser.assertPatientOwnsSuggestion(principal, suggestionId);
        return Result.ok(registration.register(suggestionId, currentUser.patientId(principal, patientId)));
    }

    @GetMapping("/registrations")
    public Result<?> registrations(@RequestParam(required = false) Long patientId,
                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return Result.ok(registration.myRegistrations(currentUser.patientId(principal, patientId)));
    }

    @GetMapping("/records")
    public Result<?> records(@RequestParam(required = false) Long patientId,
                             @AuthenticationPrincipal AuthPrincipal principal) {
        return Result.ok(records.myRecords(currentUser.patientId(principal, patientId)));
    }

    @GetMapping("/record/detail")
    public Result<?> recordDetail(@RequestParam long recordId,
                                  @AuthenticationPrincipal AuthPrincipal principal) {
        currentUser.assertPatientOwnsRecord(principal, recordId);
        return Result.ok(records.detail(recordId));
    }

    // ---- 个人档案（查看/维护性别、年龄、慢病史、过敏史） ----
    @GetMapping("/profile")
    public Result<?> profile(@RequestParam(required = false) Long patientId,
                             @AuthenticationPrincipal AuthPrincipal principal) {
        long ownPatientId = currentUser.patientId(principal, patientId);
        return Result.ok(db.queryOne("SELECT * FROM patient_profile WHERE id=:id", Map.of("id", ownPatientId)));
    }

    @PostMapping("/profile/save")
    public Result<?> profileSave(@RequestBody Map<String, Object> body,
                                 @AuthenticationPrincipal AuthPrincipal principal) {
        Long requested = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        long id = currentUser.patientId(principal, requested);
        Map<String, Object> safe = new LinkedHashMap<>(body);
        safe.put("id", id);
        db.update("UPDATE patient_profile SET name=:name, gender=:gender, age=:age, "
                + "allergic_history=:allergic_history, past_history=:past_history WHERE id=:id", safe);
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
