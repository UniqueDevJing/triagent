package com.health.web.assistant.advisor;

import com.health.web.assistant.model.AgentRequest;
import com.health.web.assistant.model.AgentResponse;
import org.springframework.stereotype.Component;

/**
 * 输入安全护栏：空输入 / 越权 / 危险输入 early-reject。
 * Phase1 仅做空校验 + 占位规则；Phase2 接入敏感词与越权检测。
 */
@Component
public class InputGuardrailAdvice implements AgentAdvice {

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
