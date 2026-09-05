package com.health.web.assistant.agent;

import com.health.web.assistant.model.TriageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 分诊对话状态机（Phase3）：在多轮澄清中逐步补齐「部位→时长→伴随症状」字段，
 * 并在命中红旗警示征象时短路输出确定性 EMERGENCY 分诊（不依赖 LLM，可离线演示）。
 * 会话状态按 sessionId 聚合，避免对已提供字段重复追问。
 */
@Component
public class SymptomStateMachine {

    private static final Logger log = LoggerFactory.getLogger(SymptomStateMachine.class);

    /** 决策类型 */
    public enum DecisionType { PROCEED, CLARIFY, EMERGENCY }

    /** 决策结果（record） */
    public record Decision(DecisionType type, String question, List<String> missingFields,
                           List<String> redFlagWords) {

        public static Decision proceed() {
            return new Decision(DecisionType.PROCEED, null, List.of(), List.of());
        }

        public static Decision clarify(String question, List<String> missing) {
            return new Decision(DecisionType.CLARIFY, question, missing, List.of());
        }

        public static Decision emergency(List<String> redWords) {
            return new Decision(DecisionType.EMERGENCY, null, List.of(), redWords);
        }
    }

    /** 会话内已采集字段 */
    private static final class SessionState {
        boolean bodyKnown;
        boolean durationKnown;
        boolean accompanyKnown;
    }

    // 红旗警示征象 → 建议科室（多条命中时取并集）
    private static final Map<String, List<String>> RED_FLAGS = new LinkedHashMap<>();

    static {
        RED_FLAGS.put("胸痛", List.of("急诊科", "心内科"));
        RED_FLAGS.put("胸闷", List.of("急诊科", "心内科"));
        RED_FLAGS.put("压榨", List.of("急诊科", "心内科"));
        RED_FLAGS.put("呼吸困难", List.of("急诊科", "呼吸内科"));
        RED_FLAGS.put("憋气", List.of("急诊科", "呼吸内科"));
        RED_FLAGS.put("窒息", List.of("急诊科", "呼吸内科"));
        RED_FLAGS.put("口角歪斜", List.of("急诊科", "神经内科"));
        RED_FLAGS.put("一侧肢体无力", List.of("急诊科", "神经内科"));
        RED_FLAGS.put("言语不清", List.of("急诊科", "神经内科"));
        RED_FLAGS.put("剧烈头痛", List.of("急诊科", "神经内科"));
        RED_FLAGS.put("喷射性呕吐", List.of("急诊科", "神经内科"));
        RED_FLAGS.put("意识模糊", List.of("急诊科", "神经内科"));
        RED_FLAGS.put("昏迷", List.of("急诊科", "神经内科"));
        RED_FLAGS.put("抽搐", List.of("急诊科", "神经内科"));
        RED_FLAGS.put("惊厥", List.of("急诊科", "神经内科"));
        RED_FLAGS.put("晕厥", List.of("急诊科", "神经内科"));
        RED_FLAGS.put("大量出血", List.of("急诊科", "普外科"));
    }

    private static final List<String> BODY_PARTS = List.of(            "头痛", "头晕", "胸", "腹", "胃", "背", "腰", "颈", "肩", "膝", "关节",
            "腿", "手", "脚", "眼", "耳", "鼻", "咽喉", "喉咙", "口腔", "皮肤", "心前区");
    /** 症状本身已可推断大致部位的词，视为提供了部位线索 */
    private static final List<String> INFERRED_BODY = List.of(
            "咳嗽", "咳痰", "鼻塞", "流涕", "咽痛", "腹泻", "便秘", "皮疹", "瘙痒", "尿痛", "尿频");
    private static final List<String> ACCOMPANY = List.of(
            "发热", "发烧", "恶心", "呕吐", "乏力", "疲劳", "盗汗", "消瘦", "食欲",
            "腹泻", "便秘", "麻木", "疼痛加剧", "心悸", "心慌", "咳嗽", "咳痰", "皮疹");
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(\\d+\\s*(个\\s*)?(小时|天|周|月|年))|(几(天|周|月|年)|多年|长期|反复|最近|这两个月|半年|一个多月|多月)");

    private static final int MAX_SESSIONS = 2000;

    private final Map<String, SessionState> sessions = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SessionState> eldest) {
            return size() > MAX_SESSIONS;
        }
    };

    /** 解析一条消息并更新会话状态，返回本轮的决策 */
    /** 文本中命中哪些红旗征象（无副作用，供 Supervisor 做组合请求的应急分诊优先决策） */
    public List<String> redFlagsIn(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return RED_FLAGS.keySet().stream().filter(text::contains).collect(java.util.stream.Collectors.toList());
    }

    public Decision decide(String sessionId, String message) {
        if (message == null) {
            message = "";
        }
        String text = message.trim();
        SessionState st = sessions.computeIfAbsent(sessionId, k -> new SessionState());

        // 1) 红旗优先：命中 → 强制急诊
        List<String> redHit = new ArrayList<>();
        for (String flag : RED_FLAGS.keySet()) {
            if (text.contains(flag)) {
                redHit.add(flag);
            }
        }
        if (!redHit.isEmpty()) {
            log.info("[state] 会话={} 命中红旗征象 {} → EMERGENCY", sessionId, redHit);
            return Decision.emergency(redHit);
        }

        // 2) 采集常规字段（会话内聚合）
        boolean body = BODY_PARTS.stream().anyMatch(text::contains)
                || INFERRED_BODY.stream().anyMatch(text::contains);
        boolean duration = DURATION_PATTERN.matcher(text).find();
        boolean accompany = ACCOMPANY.stream().anyMatch(text::contains);
        st.bodyKnown = st.bodyKnown || body;
        st.durationKnown = st.durationKnown || duration;
        st.accompanyKnown = st.accompanyKnown || accompany;

        // 3) 按 部位→时长→伴随症状 依次追问
        if (!st.bodyKnown) {
            return Decision.clarify("为了更好地判断，请先告诉我不适主要在哪个部位？（如头痛、胸痛、腹痛、关节痛等）",
                    List.of("bodyPart"));
        }
        if (!st.durationKnown) {
            return Decision.clarify("这个症状持续或反复多久了？（如 3 天、两周、半年、长期反复）",
                    List.of("duration"));
        }
        if (!st.accompanyKnown) {
            return Decision.clarify("有没有伴随症状或相关病史？（如发热、恶心、乏力、高血压、糖尿病等，可回答“无”）",
                    List.of("accompany"));
        }
        log.info("[state] 会话={} 字段已齐 → 交给模型分诊", sessionId);
        return Decision.proceed();
    }

    /** 红旗命中的确定性应急分诊结果 */
    public TriageResult buildEmergencyTriage(List<String> redWords) {
        Set<String> depts = new LinkedHashSet<>();
        for (String w : redWords) {
            List<String> d = RED_FLAGS.get(w);
            if (d != null) {
                depts.addAll(d);
            }
        }
        if (depts.isEmpty()) {
            depts.add("急诊科");
        }
        TriageResult r = new TriageResult();
        r.setUrgency("EMERGENCY");
        r.setDepartments(new ArrayList<>(depts));
        r.setHospitalLevel("就近医院急诊 / 呼叫 120");
        r.setConfidence(0.9);
        r.setFollowUp(List.of("立即停止活动，由他人陪同或拨打 120 前往急诊", "疑似卒中时勿等待、勿自行驾车", "带上既往病历与正在服用的药物清单"));
        r.setDisclaimer("本结论由系统红旗警示规则自动触发，仅供紧急处置参考，不构成医疗诊断。");
        return r;
    }

    /** 红旗应答正文（确定性） */
    public String emergencyAnswerText(List<String> redWords) {
        return "⚠️ 您描述的症状（" + String.join("、", redWords)
                + "）属于需要立即处理的警示征象。请马上停止手头活动，由他人陪同或拨打 120 前往就近医院急诊就诊"
                + "；如疑似卒中，请勿等待、勿自行驾车。系统已检索到相关知识供陪同人员参考，不构成诊断。";
    }
}
