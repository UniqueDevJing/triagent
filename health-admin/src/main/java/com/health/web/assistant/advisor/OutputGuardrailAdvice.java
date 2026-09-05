package com.health.web.assistant.advisor;

import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;
import org.springframework.stereotype.Component;

/**
 * 输出护栏：强制补充免责声明，拦截缺失免责的危险输出。
 * Phase1 在聚合文本后处理；Phase2 改为缓冲流并在输出前注入。
 */
@Component
public class OutputGuardrailAdvice implements AgentAdvice {

    private static final String DISCLAIMER =
            "本建议不能替代执业医师诊断，如有紧急情况请立即就医或拨打120。";

    @Override
    public int getOrder() {
        return 80;
    }

    @Override
    public AgentRequest before(AgentRequest request) {
        return request;
    }

    @Override
    public void after(AgentRequest request, AgentResponse response) {
        if (response.getFullText() == null) {
            return;
        }
        // CLARIFY/BLOCKED 等非医疗建议轮次不追加免责文本（避免追问句尾出现免责声明）
        boolean skipText = "CLARIFY".equals(response.getTurnType())
                || "BLOCKED".equals(response.getTurnType());
        boolean hasDisclaimer = response.getFullText().contains("不能替代")
                || response.getFullText().contains("执业医师");
        if (!skipText && !hasDisclaimer) {
            response.setFullText(response.getFullText() + "\n\n" + DISCLAIMER);
        }
        if (response.getTriage() != null) {
            String d = response.getTriage().getDisclaimer();
            if (d == null || d.isBlank()) {
                response.getTriage().setDisclaimer(DISCLAIMER);
            }
        }
    }
}
