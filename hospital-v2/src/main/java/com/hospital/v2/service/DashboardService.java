package com.hospital.v2.service;

import com.hospital.v2.dao.Db;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 就诊运营看板：三组数字从业务表现场聚合，不建统计表（与视频一致）。 */
@Service
public class DashboardService {
    private final Db db;

    public DashboardService(Db db) {
        this.db = db;
    }

    public Map<String, Object> overview() {
        String today = LocalDate.now().toString();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todayRegistration", count("SELECT COUNT(*) c FROM registration WHERE work_date=:t", today));
        m.put("pendingTriage", count("SELECT COUNT(*) c FROM triage_suggestion WHERE status='CONFIRMED' AND registered=0", null));
        m.put("totalSuggestion", count("SELECT COUNT(*) c FROM triage_suggestion", null));
        m.put("redFlag", count("SELECT COUNT(*) c FROM triage_suggestion WHERE red_flag=1", null));
        m.put("doctorOnline", count("SELECT COUNT(*) c FROM doctor d WHERE EXISTS "
                + "(SELECT 1 FROM doctor_schedule s WHERE s.doctor_id=d.id AND s.status=1 AND s.remain_num>0)", null));
        m.put("hitRate", hitRate());
        return m;
    }

    /** 分诊命中率：被拿去挂号 / 总建议（按推荐科室现场聚合）。 */
    public double hitRate() {
        long total = count("SELECT COUNT(*) c FROM triage_suggestion", null);
        if (total == 0) return 0;
        long hit = count("SELECT COUNT(*) c FROM triage_suggestion WHERE registered=1", null);
        return Math.round(hit * 1000.0 / total) / 10.0;
    }

    public List<Map<String, Object>> byDepartment() {
        return db.query("SELECT dep.name department, COUNT(s.id) suggestion, "
                + "SUM(s.registered) registered, ROUND(SUM(s.registered)*100.0/COUNT(s.id),1) hit_rate "
                + "FROM triage_suggestion s JOIN department dep ON dep.id=s.department_id "
                + "GROUP BY dep.name ORDER BY suggestion DESC", Map.of());
    }

    public List<Map<String, Object>> recentRuns() {
        return db.query("SELECT r.id run_id, r.status, r.steps, r.cost_ms, r.red_flag, "
                + "dep.name department, doc.name doctor, r.create_time "
                + "FROM agent_run r LEFT JOIN department dep ON dep.id=r.final_department_id "
                + "LEFT JOIN doctor doc ON doc.id=r.final_doctor_id ORDER BY r.id DESC LIMIT 20", Map.of());
    }

    public List<Map<String, Object>> timeline(long runId) {
        return db.query("SELECT step_index, thought, tool_code, input_json, output_json, cost_ms "
                + "FROM agent_step WHERE run_id=:r ORDER BY step_index", Map.of("r", runId));
    }

    public List<Map<String, Object>> toolLogs(long runId) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("r", runId);
        return db.query("SELECT id, tool_code, source, status, cost_ms, create_time FROM mcp_tool_call_log "
                + "WHERE run_id=:r ORDER BY id", p);
    }

    private long count(String sql, String date) {
        Map<String, Object> p = date == null ? Map.of() : Map.of("t", date);
        Map<String, Object> row = db.queryOne(sql, p);
        return row == null ? 0 : ((Number) row.get("c")).longValue();
    }
}
