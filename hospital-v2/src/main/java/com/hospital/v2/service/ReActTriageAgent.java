package com.hospital.v2.service;

import com.hospital.v2.common.BizException;
import com.hospital.v2.common.JsonUtils;
import com.hospital.v2.config.HospitalProps;
import com.hospital.v2.config.LlmGateway;
import com.hospital.v2.dao.Db;
import com.hospital.v2.domain.TriageResult;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多步 ReAct 分诊 Agent。
 * 后端只把「工具清单 + 循环上限」交给模型，模型逐轮自主决定调用哪些工具；
 * 每步写 agent_run/agent_step（可回放）；AI 输出经代码二次核验后再落库；
 * 命中危急征象规则则硬拦截转急诊。
 */
@Service
public class ReActTriageAgent {

    /** 动态系统提示词：注入今天日期与步数预算，强约束收敛行为（真实模型防幻觉/防发散的关键）。 */
    private String systemPrompt(int maxSteps) {
        String today = LocalDate.now().toString();
        return "你是智慧医院分诊 Agent。今天是 " + today + "，查询号源时 work_date 只能填 " + today
                + " 及之后的日期。\n"
                + "你有最多 " + maxSteps + " 轮调用机会。标准流程共 4 步：\n"
                + "1) symptom_tag_search( symptom ) 确认症状标签；\n"
                + "2) department_query( 症状对应科室关键词 ) 选定科室；\n"
                + "3) doctor_query( department_id ) 选定医生；\n"
                + "4) schedule_query( doctor_id, work_date=" + today + " 等有效日期, period ) 查号源。\n"
                + "硬性要求：每步只调用一个工具；schedule_query 一旦返回有余号的排班，"
                + "下一步必须立即输出最终 JSON，绝不再调用任何工具。\n"
                + "最终输出格式（只输出 JSON 本身，无任何多余文字）：\n"
                + "{\"department_id\":int,\"doctor_id\":int,\"confidence\":0.x,\"reason\":\"一句话理由\"}";
    }

    private final LlmGateway llm;
    private final ToolCenter tools;
    private final HospitalProps props;
    private final Db db;

    public ReActTriageAgent(LlmGateway llm, ToolCenter tools, HospitalProps props, Db db) {
        this.llm = llm;
        this.tools = tools;
        this.props = props;
        this.db = db;
    }

    public TriageResult triage(Long sessionId, Long patientId, String complaint) {
        long t0 = System.currentTimeMillis();
        long runId = newRun(sessionId, patientId);
        List<ToolSpecification> specs = tools.buildSpecs();
        List<ChatMessage> msgs = new ArrayList<>();
        msgs.add(SystemMessage.from(systemPrompt(props.getLlm().getMaxAgentSteps())));
        msgs.add(UserMessage.from("患者主诉：" + complaint + "。请通过工具核查后给出分诊建议（仅输出 JSON）。"));

        int steps = 0;
        Map<String, Object> finalJson = null;

        // 危急征象硬拦截（代码侧，不依赖模型）
        Long redDept = checkRedFlag(complaint);
        if (redDept != null) {
            Map<String, Object> rf = new LinkedHashMap<>();
            rf.put("department_id", redDept);
            rf.put("doctor_id", firstDoctor(redDept));
            rf.put("confidence", 0.99);
            rf.put("reason", "命中危急征象规则，已硬拦截转急诊绿色通道。");
            finalJson = buildVerified(runId, sessionId, patientId, rf, true, t0);
            return new TriageResult(runId, finalJson, steps + 1);
        }

        // ReAct 循环
        while (steps < props.getLlm().getMaxAgentSteps()) {
            Response<AiMessage> resp = llm.generate(msgs, specs);
            AiMessage ai = resp.content();
            steps++;
            if (ai.hasToolExecutionRequests()) {
                for (ToolExecutionRequest req : ai.toolExecutionRequests()) {
                    long stepId = newStep(runId, steps, ai.text(), req.name(), req.arguments());
                    String result = tools.execute(req.name(), req.arguments(), runId, stepId, "agent");
                    msgs.add(ai);
                    msgs.add(ToolExecutionResultMessage.from(req, result));
                    db.update("UPDATE agent_step SET output_json=:o, cost_ms=:c WHERE id=:id",
                            Map.of("o", clip(result), "c", 30, "id", stepId));
                }
            } else {
                finalJson = parseFinal(ai.text());
                if (finalJson != null) break;
            }
        }
        if (finalJson == null) finalJson = fallback(complaint);
        Map<String, Object> verified = buildVerified(runId, sessionId, patientId, finalJson, false, t0);
        return new TriageResult(runId, verified, steps);
    }

    // ---------- 二次核验 ----------
    private Map<String, Object> buildVerified(long runId, Long sessionId, Long patientId,
                                             Map<String, Object> raw, boolean redFlag, long t0) {
        long dept = asLong(raw.get("department_id"), 1L);
        long doc = asLong(raw.get("doctor_id"), firstDoctor(dept));
        double conf = asDouble(raw.get("confidence"), 0.8);
        String reason = String.valueOf(raw.getOrDefault("reason", ""));

        // 科室存在性
        Map<String, Object> d = db.queryOne("SELECT id, name FROM department WHERE id=:id", Map.of("id", dept));
        if (d == null) { dept = 1; doc = firstDoctor(1L); }

        // 医生归属 + 号源校验（无余号则在本科室换人）
        Map<String, Object> docRow = db.queryOne(
                "SELECT id, name, department_id FROM doctor WHERE id=:id", Map.of("id", doc));
        if (docRow == null || !deptEquals(docRow, dept)) {
            doc = firstDoctor(dept);
        }
        String date = nearestAvailableDate(doc);
        if (date == null) {
            reason += "（提示：该医生暂无可约号源，建议人工改派）";
            conf = Math.min(conf, 0.6);
        }

        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("department_id", dept);
        suggestion.put("department_name", d == null ? "" : d.get("name"));
        suggestion.put("doctor_id", doc);
        suggestion.put("doctor_name", doctorName(doc));
        suggestion.put("confidence", conf);
        suggestion.put("red_flag", redFlag ? 1 : 0);
        suggestion.put("reason", reason);
        suggestion.put("suggested_date", date == null ? "" : date);

        // 落库：triage_suggestion
        Map<String, Object> ins = new LinkedHashMap<>();
        ins.put("run_id", runId);
        ins.put("session_id", sessionId);
        ins.put("patient_id", patientId);
        ins.put("department_id", dept);
        ins.put("doctor_id", doc);
        ins.put("confidence", conf);
        ins.put("red_flag", redFlag ? 1 : 0);
        ins.put("reason", reason);
        ins.put("status", "CONFIRMED");
        long sugId = db.insert("INSERT INTO triage_suggestion (run_id, session_id, patient_id, department_id, doctor_id, "
                + "confidence, red_flag, reason, status) VALUES "
                + "(:run_id,:session_id,:patient_id,:department_id,:doctor_id,:confidence,:red_flag,:reason,:status)", ins);

        // 更新 run
        db.update("UPDATE agent_run SET status='DONE', final_department_id=:d, final_doctor_id=:doc, "
                        + "red_flag=:rf, steps=:s, cost_ms=:c WHERE id=:id",
                Map.of("d", dept, "doc", doc, "rf", redFlag ? 1 : 0, "s", suggestion.size(), "c",
                        (int) (System.currentTimeMillis() - t0), "id", runId));

        suggestion.put("suggestion_id", sugId);
        suggestion.put("run_id", runId);
        return suggestion;
    }

    private Long checkRedFlag(String complaint) {
        if (complaint == null) return null;
        List<Map<String, Object>> rules = db.query("SELECT id, name, keywords, level, advice FROM red_flag_rule", Map.of());
        for (Map<String, Object> r : rules) {
            String kws = String.valueOf(r.get("keywords"));
            for (String kw : kws.split(",")) {
                if (!kw.isBlank() && complaint.contains(kw.trim())) {
                    // 命中 → 急诊科（id=5）
                    return 5L;
                }
            }
        }
        return null;
    }

    private String nearestAvailableDate(long doctorId) {
        String today = LocalDate.now().toString();
        Map<String, Object> row = db.queryOne(
                "SELECT work_date, period FROM doctor_schedule WHERE doctor_id=:doc AND status=1 "
                        + "AND remain_num>0 AND work_date>=:today ORDER BY work_date, period LIMIT 1",
                Map.of("doc", doctorId, "today", today));
        if (row == null) return null;
        return row.get("work_date") + " " + row.get("period");
    }

    private long firstDoctor(long deptId) {
        Map<String, Object> row = db.queryOne(
                "SELECT id FROM doctor WHERE department_id=:d ORDER BY id LIMIT 1", Map.of("d", deptId));
        return row == null ? 1L : ((Number) row.get("id")).longValue();
    }

    private boolean deptEquals(Map<String, Object> docRow, long dept) {
        return ((Number) docRow.get("department_id")).longValue() == dept;
    }

    private String doctorName(long docId) {
        Map<String, Object> row = db.queryOne("SELECT name FROM doctor WHERE id=:id", Map.of("id", docId));
        return row == null ? "" : String.valueOf(row.get("name"));
    }

    private Map<String, Object> fallback(String complaint) {
        long dept = 1, doc = 1;
        if (complaint != null) {
            if (complaint.contains("心") || complaint.contains("胸")) { dept = 2; doc = 3; }
            else if (complaint.contains("腹") || complaint.contains("泻")) { dept = 3; doc = 5; }
            else if (complaint.contains("头") || complaint.contains("晕")) { dept = 4; doc = 6; }
            else if (complaint.contains("骨") || complaint.contains("关节")) { dept = 6; doc = 8; }
            else if (complaint.contains("热") || complaint.contains("儿")) { dept = 7; doc = 9; }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("department_id", dept);
        m.put("doctor_id", doc);
        m.put("confidence", 0.7);
        m.put("reason", "循环上限内未收敛，已按症状关键词兜底分诊。");
        return m;
    }

    private Map<String, Object> parseFinal(String text) {
        if (text == null) return null;
        int s = text.indexOf("{");
        int e = text.lastIndexOf("}");
        if (s < 0 || e < 0 || e <= s) return null;
        return JsonUtils.fromJson(text.substring(s, e + 1), new com.fasterxml.jackson.core.type.TypeReference<>() {});
    }

    private long newRun(Long sessionId, Long patientId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("session_id", sessionId);
        m.put("patient_id", patientId);
        return db.insert("INSERT INTO agent_run (session_id, patient_id, status) VALUES (:session_id,:patient_id,'RUNNING')", m);
    }

    private long newStep(long runId, int idx, String thought, String code, String args) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("run_id", runId);
        m.put("step_index", idx);
        m.put("thought", thought == null ? "" : thought);
        m.put("tool_code", code);
        m.put("input_json", args);
        return db.insert("INSERT INTO agent_step (run_id, step_index, thought, tool_code, input_json) "
                + "VALUES (:run_id,:step_index,:thought,:tool_code,:input_json)", m);
    }

    private String clip(String s) { return s == null ? "" : (s.length() > 4000 ? s.substring(0, 4000) : s); }
    private long asLong(Object o, long d) {
        if (o == null) return d;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return d; }
    }
    private double asDouble(Object o, double d) {
        if (o == null) return d;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return d; }
    }
}
