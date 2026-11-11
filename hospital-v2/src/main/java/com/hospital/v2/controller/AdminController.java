package com.hospital.v2.controller;

import com.hospital.v2.common.Result;
import com.hospital.v2.dao.Db;
import com.hospital.v2.service.DashboardService;
import com.hospital.v2.service.KnowledgeService;
import com.hospital.v2.service.ToolCenter;
import com.hospital.v2.service.UsageService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理端 REST：工具注册表、知识库、看板、Agent 时间线、AI 观测、基础数据。 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ToolCenter tools;
    private final KnowledgeService knowledge;
    private final DashboardService dashboard;
    private final UsageService usage;
    private final Db db;

    public AdminController(ToolCenter tools, KnowledgeService knowledge,
                           DashboardService dashboard, UsageService usage, Db db) {
        this.tools = tools;
        this.knowledge = knowledge;
        this.dashboard = dashboard;
        this.usage = usage;
        this.db = db;
    }

    // ---- 工具中心 ----
    @GetMapping("/tools")
    public Result<List<Map<String, Object>>> tools() {
        return Result.ok(db.query("SELECT * FROM mcp_tool ORDER BY sort", Map.of()));
    }

    @PostMapping("/tools/toggle")
    public Result<?> toggle(@RequestParam long id) {
        db.update("UPDATE mcp_tool SET enabled = 1 - enabled WHERE id=:id", Map.of("id", id));
        return Result.ok();
    }

    @PostMapping("/tools/test")
    public Result<?> testTool(@RequestParam String code, @RequestBody(required = false) Map<String, Object> args) {
        String argJson;
        try {
            argJson = args == null ? "{}" : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(args);
        } catch (Exception e) {
            argJson = "{}";
        }
        String result = tools.execute(code, argJson, null, null, "manual");
        return Result.ok(Map.of("result", result));
    }

    // ---- 知识库 RAG ----
    @GetMapping("/knowledge/bases")
    public Result<?> bases() { return Result.ok(knowledge.listBases()); }

    @GetMapping("/knowledge/docs")
    public Result<?> docs(@RequestParam long baseId) { return Result.ok(knowledge.listDocs(baseId)); }

    @PostMapping("/knowledge/add")
    public Result<?> addDoc(@RequestParam long baseId, @RequestParam String title, @RequestParam String content) {
        return Result.ok(Map.of("docId", knowledge.addDoc(baseId, title, content)));
    }

    @GetMapping("/knowledge/retrieve")
    public Result<?> retrieve(@RequestParam String q, @RequestParam(required = false) Long baseId,
                              @RequestParam(defaultValue = "4") int topK) {
        return Result.ok(knowledge.retrieve(q, baseId, topK));
    }

    // ---- 看板与观测 ----
    @GetMapping("/dashboard/overview")
    public Result<?> overview() { return Result.ok(dashboard.overview()); }

    @GetMapping("/dashboard/by-department")
    public Result<?> byDept() { return Result.ok(dashboard.byDepartment()); }

    @GetMapping("/dashboard/runs")
    public Result<?> runs() { return Result.ok(dashboard.recentRuns()); }

    @GetMapping("/dashboard/timeline")
    public Result<?> timeline(@RequestParam long runId) { return Result.ok(dashboard.timeline(runId)); }

    @GetMapping("/dashboard/tool-logs")
    public Result<?> toolLogs(@RequestParam long runId) { return Result.ok(dashboard.toolLogs(runId)); }

    @GetMapping("/usage")
    public Result<?> usage() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("summary", usage.summary());
        m.put("byCase", usage.byCase());
        return Result.ok(m);
    }

    // ---- 基础数据 ----
    @GetMapping("/departments")
    public Result<?> departments() { return Result.ok(db.query("SELECT * FROM department ORDER BY id", Map.of())); }

    @GetMapping("/doctors")
    public Result<?> doctors(@RequestParam(required = false) Long departmentId) {
        if (departmentId == null) return Result.ok(db.query("SELECT * FROM doctor ORDER BY id", Map.of()));
        return Result.ok(db.query("SELECT * FROM doctor WHERE department_id=:d ORDER BY id", Map.of("d", departmentId)));
    }

    @GetMapping("/schedules")
    public Result<?> schedules(@RequestParam(required = false) Long doctorId, @RequestParam(required = false) String date) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("doc", doctorId);
        p.put("date", date);
        String sql = "SELECT * FROM doctor_schedule WHERE 1=1";
        if (doctorId != null) sql += " AND doctor_id=:doc";
        if (date != null) sql += " AND work_date=:date";
        sql += " ORDER BY work_date, period";
        return Result.ok(db.query(sql, p));
    }

    @GetMapping("/symptom-tags")
    public Result<?> symptomTags() { return Result.ok(db.query("SELECT * FROM symptom_tag ORDER BY id", Map.of())); }

    @GetMapping("/red-flags")
    public Result<?> redFlags() { return Result.ok(db.query("SELECT * FROM red_flag_rule ORDER BY level", Map.of())); }

    @GetMapping("/ai-models")
    public Result<?> aiModels() { return Result.ok(db.query("SELECT * FROM ai_model_config ORDER BY sort", Map.of())); }
}
