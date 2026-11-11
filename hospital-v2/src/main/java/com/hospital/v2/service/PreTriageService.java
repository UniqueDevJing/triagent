package com.hospital.v2.service;

import com.hospital.v2.config.LlmGateway;
import com.hospital.v2.dao.Db;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能预问诊：多轮 SSE 追问（简练、口语化），会话记忆落库；
 * 分诊后会话被锁定，不能再追问（与视频一致）。
 */
@Service
public class PreTriageService {
    private static final String PRE_SYS = "你是医院预问诊助手，用简短口语化中文追问患者症状，每次只问一个重点，不要罗列。";

    private final Db db;
    private final LlmGateway llm;

    public PreTriageService(Db db, LlmGateway llm) {
        this.db = db;
        this.llm = llm;
    }

    public long startSession(Long patientId) {
        Map<String, Object> m = Map.of("patient_id", patientId == null ? 0 : patientId);
        return db.insert("INSERT INTO pre_triage_session (patient_id, status) VALUES (:patient_id,'ACTIVE')", m);
    }

    public boolean isLocked(long sessionId) {
        Map<String, Object> row = db.queryOne("SELECT locked FROM pre_triage_session WHERE id=:id", Map.of("id", sessionId));
        return row != null && ((Number) row.get("locked")).intValue() == 1;
    }

    public void appendUser(long sessionId, String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("session_id", sessionId);
        m.put("role", "user");
        m.put("content", text);
        db.insert("INSERT INTO pre_triage_message (session_id, role, content) VALUES (:session_id,:role,:content)", m);
    }

    private void appendAssistant(long sessionId, String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("session_id", sessionId);
        m.put("role", "assistant");
        m.put("content", text);
        db.insert("INSERT INTO pre_triage_message (session_id, role, content) VALUES (:session_id,:role,:content)", m);
        db.update("UPDATE pre_triage_session SET update_time=CURRENT_TIMESTAMP WHERE id=:id", Map.of("id", sessionId));
    }

    /** 生成助手追问（真实模型或本地脚本）。 */
    public String generateReply(long sessionId, String userText) {
        if (isLocked(sessionId)) return "本次预问诊已结束并生成分诊建议，如需继续请咨询医生。";
        int turn = ((Number) db.queryOne(
                "SELECT COUNT(*) c FROM pre_triage_message WHERE session_id=:s AND role='user'",
                Map.of("s", sessionId)).get("c")).intValue();

        String reply;
        String real = llm.chatText(PRE_SYS, "患者说：" + userText);
        if (real != null && !real.isBlank()) {
            reply = real;
        } else {
            reply = scripted(turn + 1);
        }
        appendAssistant(sessionId, reply);
        return reply;
    }

    /** 分诊后锁定会话。 */
    public void lock(long sessionId, String summary) {
        db.update("UPDATE pre_triage_session SET status='LOCKED', locked=1, summary=:s WHERE id=:id",
                Map.of("s", summary == null ? "" : summary, "id", sessionId));
    }

    private String scripted(int t) {
        return switch (t) {
            case 1 -> "请问这个症状出现多久了？是持续性的还是一阵一阵的？";
            case 2 -> "有没有伴随其他不舒服，比如发热、咳痰、胸闷或者头晕？";
            case 3 -> "以前有没有类似的情况，或者正在长期吃哪些药？";
            default -> "了解得差不多了，正在为您发起智能分诊，请稍候…";
        };
    }

    public List<Map<String, Object>> history(long sessionId) {
        return db.query("SELECT role, content, create_time FROM pre_triage_message WHERE session_id=:s ORDER BY id",
                Map.of("s", sessionId));
    }
}
