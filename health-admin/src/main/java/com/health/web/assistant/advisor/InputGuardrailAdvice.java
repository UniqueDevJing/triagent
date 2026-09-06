package com.health.web.assistant.advisor;

import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 输入安全护栏：空输入 / 越权 / 危险输入 early-reject。
 * 上限防御：单条消息 >2000 字符直接拒绝（防 LLM token 成本滥用与提示词爆炸）；
 * 会话 ID 校验格式（防恶意 key 填充会话内存表）。
 */
@Component
public class InputGuardrailAdvice implements AgentAdvice {

    /** 单条消息最大长度（字符） */
    static final int MAX_MESSAGE_LENGTH = 2000;
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_-]{6,64}");

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public AgentRequest before(AgentRequest request) {
        String msg = request.getMessage();
        if (msg == null || msg.trim().length() < 2) {
            throw new AgentBlockedException("请输入您的症状描述");
        }
        if (msg.length() > MAX_MESSAGE_LENGTH) {
            throw new AgentBlockedException("消息过长（上限 " + MAX_MESSAGE_LENGTH + " 字），请分段描述");
        }
        String sid = request.getSessionId();
        if (sid == null || !SESSION_ID_PATTERN.matcher(sid).matches()) {
            throw new AgentBlockedException("会话标识无效，请刷新页面后重试");
        }
        if (containsBanned(msg)) {
            throw new AgentBlockedException("该内容无法处理，请咨询专业医生");
        }
        return request;
    }

    @Override
    public void after(AgentRequest request, AgentResponse response) {
        // 输入护栏无需后置处理
    }

    private boolean containsBanned(String msg) {
        // 占位：实际可接入敏感词 / 越权指令检测
        return false;
    }
}
