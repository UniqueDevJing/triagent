package com.hospital.v2.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hospital.v2.common.JsonUtils;
import com.hospital.v2.config.HospitalProps;
import com.hospital.v2.dao.Db;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库驱动 MCP 工具中心。
 * - 运行时按 mcp_tool 表 enabled=1 现查现建工具池（关了模型下一次真的调不到）。
 * - 工具调用统一走 execute()，成功/失败都写 mcp_tool_call_log。
 */
@Service
public class ToolCenter {
    private final Db db;
    private final KnowledgeService knowledge;
    private final HospitalProps props;

    public ToolCenter(Db db, KnowledgeService knowledge, HospitalProps props) {
        this.db = db;
        this.knowledge = knowledge;
        this.props = props;
    }

    /** 读取启用的工具，构建给模型的 ToolSpecification 列表。 */
    public List<ToolSpecification> buildSpecs() {
        List<Map<String, Object>> tools = db.query(
                "SELECT tool_code, name, description FROM mcp_tool WHERE enabled=1 ORDER BY sort", Map.of());
        List<ToolSpecification> specs = new ArrayList<>();
        for (Map<String, Object> t : tools) {
            specs.add(ToolSpecification.builder()
                    .name(String.valueOf(t.get("tool_code")))
                    .description(String.valueOf(t.get("description")))
                    .parameters(JsonObjectSchema.builder().build())
                    .build());
        }
        return specs;
    }

    /** 执行工具：解析入参 → 分发 → 记日志 → 返回结果文本。 */
    public String execute(String toolCode, String argsJson, Long runId, Long stepId, String source) {
        long t0 = System.currentTimeMillis();
        Map<String, Object> args = JsonUtils.fromJson(argsJson == null ? "{}" : argsJson, new TypeReference<>() {});
        if (args == null) args = new LinkedHashMap<>();
        try {
            String result = switch (toolCode) {
                case "department_query" -> departmentQuery(str(args, "keyword"));
                case "doctor_query" -> doctorQuery(num(args, "department_id"), str(args, "title"));
                case "schedule_query" -> scheduleQuery(num(args, "doctor_id"), str(args, "work_date"), str(args, "period"));
                case "patient_profile_query" -> patientQuery(num(args, "patient_id"));
                case "symptom_tag_search" -> symptomSearch(str(args, "keyword"));
                case "knowledge_retrieve" -> knowledgeRetrieve(str(args, "query"), num(args, "top_k"));
                default -> throw new IllegalArgumentException("未知工具: " + toolCode);
            };
            writeLog(toolCode, argsJson, result, "SUCCESS", (int) (System.currentTimeMillis() - t0), runId, stepId, source);
            return result;
        } catch (Exception e) {
            String err = "{\"error\":\"" + e.getMessage() + "\"}";
            writeLog(toolCode, argsJson, err, "FAILED", (int) (System.currentTimeMillis() - t0), runId, stepId, source);
            return err;
        }
    }

    private String departmentQuery(String kw) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("kw", "%" + (kw == null ? "" : kw) + "%");
        List<Map<String, Object>> rows = db.query(
                "SELECT id, name, code, description FROM department WHERE name LIKE :kw OR code LIKE :kw", p);
        if (!rows.isEmpty()) {
            return JsonUtils.toJson(Map.of("count", rows.size(), "matched_by", "department_name", "departments", rows));
        }
        // 容错：按科室名查不到时，联动「症状→科室」映射表（模型常用症状词当关键词）
        List<Map<String, Object>> mapped = db.query(
                "SELECT d.id, d.name, d.code, d.description, m.symptom_keyword, m.weight "
                        + "FROM symptom_department_map m JOIN department d ON d.id=m.department_id "
                        + "WHERE m.symptom_keyword LIKE :kw ORDER BY m.weight DESC", p);
        if (!mapped.isEmpty()) {
            return JsonUtils.toJson(Map.of("count", mapped.size(), "matched_by", "symptom_map", "departments", mapped));
        }
        return JsonUtils.toJson(Map.of("count", 0, "matched_by", "none", "departments", List.of(),
                "hint", "未匹配到科室，请换关键词或先调 symptom_tag_search"));
    }

    private String doctorQuery(Long deptId, String title) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("dept", deptId);
        p.put("title", title);
        String sql = "SELECT id, name, department_id, title, specialty, good_at FROM doctor WHERE 1=1";
        if (deptId != null) sql += " AND department_id=:dept";
        if (title != null && !title.isBlank()) sql += " AND title=:title";
        List<Map<String, Object>> rows = db.query(sql, p);
        return JsonUtils.toJson(Map.of("count", rows.size(), "doctors", rows));
    }

    private String scheduleQuery(Long doctorId, String date, String period) {
        // period 归一化：模型常给 morning/afternoon/上午/下午，库里是 AM/PM
        String normPeriod = normalizePeriod(period);
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("doc", doctorId);
        p.put("date", date);
        p.put("period", normPeriod);
        String sql = "SELECT id, doctor_id, department_id, work_date, period, total_num, remain_num, status "
                + "FROM doctor_schedule WHERE doctor_id=:doc AND work_date=:date AND status=1";
        if (normPeriod != null && !normPeriod.isBlank()) sql += " AND period=:period";
        List<Map<String, Object>> rows = db.query(sql, p);
        if (!rows.isEmpty()) {
            return JsonUtils.toJson(Map.of("count", rows.size(), "schedules", rows));
        }
        // 容错：指定日期查不到时，自动放宽到未来 7 天该医生的所有排班
        List<Map<String, Object>> wide = db.query(
                "SELECT id, doctor_id, department_id, work_date, period, total_num, remain_num, status "
                        + "FROM doctor_schedule WHERE doctor_id=:doc AND status=1 AND remain_num>0 "
                        + "AND work_date>=CURDATE() ORDER BY work_date, period LIMIT 10",
                Map.of("doc", doctorId));
        return JsonUtils.toJson(Map.of(
                "count", wide.size(), "schedules", wide,
                "note", "指定日期 " + date + (normPeriod == null || normPeriod.isBlank() ? "" : " " + normPeriod)
                        + " 无排班，已自动返回该医生未来 7 天可用号源"));
    }

    /** morning/AM/上午 → AM；afternoon/PM/下午 → PM；其余原样返回（null 安全）。 */
    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) return "";
        String s = period.trim().toUpperCase();
        if (s.equals("MORNING") || s.contains("上午") || s.equals("AM")) return "AM";
        if (s.equals("AFTERNOON") || s.contains("下午") || s.equals("PM")) return "PM";
        return period.trim();
    }

    private String patientQuery(Long patientId) {
        Map<String, Object> p = Map.of("pid", patientId);
        Map<String, Object> row = db.queryOne(
                "SELECT id, name, gender, age, allergic_history, past_history FROM patient_profile WHERE id=:pid", p);
        return JsonUtils.toJson(row == null ? Map.of("found", false) : row);
    }

    private String symptomSearch(String kw) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("kw", "%" + (kw == null ? "" : kw) + "%");
        List<Map<String, Object>> tags = db.query(
                "SELECT id, name, department_id, description FROM symptom_tag WHERE name LIKE :kw OR description LIKE :kw", p);
        List<Map<String, Object>> maps = db.query(
                "SELECT symptom_keyword, department_id, weight FROM symptom_department_map WHERE symptom_keyword LIKE :kw", p);
        return JsonUtils.toJson(Map.of("tags", tags, "deptHints", maps));
    }

    private String knowledgeRetrieve(String query, Long topK) {
        List<Map<String, Object>> hits = knowledge.retrieve(query, null, topK == null ? props.getRag().getTopK() : topK.intValue());
        List<Map<String, Object>> slim = new ArrayList<>();
        for (Map<String, Object> h : hits) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("content", h.get("content"));
            m.put("score", h.get("score"));
            slim.add(m);
        }
        return JsonUtils.toJson(Map.of("count", slim.size(), "segments", slim));
    }

    private void writeLog(String code, String input, String output, String status, int ms, Long runId, Long stepId, String source) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("run_id", runId);
        m.put("step_id", stepId);
        m.put("tool_code", code);
        m.put("source", source == null ? "agent" : source);
        m.put("input_json", input);
        m.put("output_json", output.length() > 4000 ? output.substring(0, 4000) : output);
        m.put("status", status);
        m.put("cost_ms", ms);
        db.insert("INSERT INTO mcp_tool_call_log (run_id, step_id, tool_code, source, input_json, output_json, status, cost_ms) "
                + "VALUES (:run_id,:step_id,:tool_code,:source,:input_json,:output_json,:status,:cost_ms)", m);
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    private Long num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }
}
