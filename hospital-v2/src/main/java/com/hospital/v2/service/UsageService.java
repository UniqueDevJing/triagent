package com.hospital.v2.service;

import com.hospital.v2.dao.Db;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** AI 调用观测：按用途聚合次数 / 成功率 / 平均耗时 / Token。 */
@Service
public class UsageService {
    private final Db db;

    public UsageService(Db db) {
        this.db = db;
    }

    public List<Map<String, Object>> byCase() {
        return db.query("SELECT use_case, COUNT(*) total, "
                + "SUM(success) success, ROUND(AVG(cost_ms),1) avg_ms, SUM(total_tokens) tokens "
                + "FROM ai_usage_log GROUP BY use_case ORDER BY total DESC", Map.of());
    }

    public Map<String, Object> summary() {
        Map<String, Object> row = db.queryOne(
                "SELECT COUNT(*) total, SUM(success) success, ROUND(AVG(cost_ms),1) avg_ms, SUM(total_tokens) tokens "
                        + "FROM ai_usage_log", Map.of());
        return row == null ? Map.of() : row;
    }
}
