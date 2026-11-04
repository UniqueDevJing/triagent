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

    /** 处方类诱导（输入护栏）：用户主动要求开药 / 开处方 */
    private static final Pattern PRESCRIPTION_INPUT = Pattern.compile(
            "(开(个|一|了)?(处)?方|开(点|些|个)?药|配(点|些)?药|抓(点|些|个)?药|拿(点|些)?药|自己买药|自行买药)");

    /**
     * 处方类输出（输出护栏）：模型直接给出用药剂量 / 开具处方。
     * 要求含具体剂量或直接出具，避免把「请咨询医生开具处方 / 遵医嘱服用」这类安全引导误伤。
     */
    private static final Pattern PRESCRIPTION_OUTPUT = Pattern.compile(
            "(建议)?(您|你)?(每日|每天|每次|口服|服用|饭后|餐前).{0,8}?\\d+\\s*(次|片|粒|毫克|mg|ML|ml|袋|丸|支)"
            + "|(剂量|用量|顿服|日服).{0,4}(为|:|约)?\\s*\\d"
            + "|(我|本助手|系统|为您|给你|为你)(帮|为)?(开|配|抓).{0,3}(处)?方"
            + "|可以.{0,10}?\\d+\\s*(片|粒|毫克|mg|袋|丸|支)"
            + "|自行(服药|用药)",
            Pattern.CASE_INSENSITIVE);

    private static final String PRESCRIPTION_BLOCK_MSG =
            "⚠️ 出于用药安全，本系统不提供处方或具体用药剂量建议。请前往正规医院由执业医师"
            + "面诊后开具处方；如出现胸痛、呼吸困难、意识模糊等警示征象，请立即拨打 120 或前往急诊。";

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public AgentRequest before(AgentRequest request) {
        String msg = request.getMessage();
        // 分诊澄清提示承诺“可回答‘无’”，单字否定应答须放行（否则状态机永远收不到该字段）
        if (msg == null || (msg.trim().length() < 2 && !"无".equals(msg.trim()))) {
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
        String full = response.getFullText();
        if (full != null && outputHasPrescription(full)) {
            response.setBlocked(true);
            response.setBlockReason("检测到处方/用药剂量建议，已按安全策略拦截");
            response.setFullText(PRESCRIPTION_BLOCK_MSG);
        }
    }

    /** 模型输出是否含处方 / 具体用药剂量建议（供输出护栏与单测复用） */
    public boolean outputHasPrescription(String text) {
        if (text == null) {
            return false;
        }
        return PRESCRIPTION_OUTPUT.matcher(text).find();
    }

    boolean containsBanned(String msg) {
        // 输入护栏：拦截用户主动要求开药 / 开处方（红队维度④：诱导越权开处方）
        if (msg == null) {
            return false;
        }
        return PRESCRIPTION_INPUT.matcher(msg).find();
    }
}
