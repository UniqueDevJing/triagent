package com.health.web.assistant.advisor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 图像生成安全边界（W4）。
 *
 * <p>文本护栏拦的是"越权开处方"，图像护栏拦的是**伪造与侵权**：
 * 假 X 光片、假病历、假证件、名人肖像——这些一旦生成，危害远大于多说一句话。
 *
 * <p>三道闸门，缺一不可：
 * <ol>
 *   <li>{@link #check(String)} 提示词审核：命中即拒，**不进队列、不占 GPU**；</li>
 *   <li>{@link #buildRequest(String)} 合规请求也必须带**水印文案 + 免责声明**；</li>
 *   <li>能力侧由 {@code ToolAccessPolicy} 限制"谁能调"（预约管家无权出图）。</li>
 * </ol>
 *
 * <p>规避对抗沿用文本护栏的做法：先剥离空格/符号再匹配，防止「X 光」「X*光」这类绕过。
 */
public final class ImageSafetyGuard {

    /** 命中即拒的规则（标签用于审计与前端提示，不回显命中细节给用户）。 */
    private static final List<Rule> RULES = List.of(
            new Rule("medical-imaging", Pattern.compile(
                    "(x\\s*光|ct|核磁|mri|b\\s*超|超声|心电图|脑电图|造影|病理切片)\\s*(片|照片|影像|图像|图|报告)?",
                    Pattern.CASE_INSENSITIVE)),
            new Rule("medical-document", Pattern.compile(
                    "(病历|诊断书|诊断证明|处方单|病危通知|出院小结|检验报告单)")),
            new Rule("id-document", Pattern.compile(
                    "(身份证|护照|驾驶证|行驶证|社保卡|银行卡|户口本)\\s*(照片|正面|反面|扫描|图片)?")),
            new Rule("real-person-likeness", Pattern.compile(
                    "(真人|明星|名人|网红|某人|某医生)\\s*的?\\s*(照片|肖像|脸|头像|证件照)")),
            new Rule("graphic-violence", Pattern.compile("(尸检|尸体|血淋淋|肢解|开膛|断肢)")),
            new Rule("sexual", Pattern.compile("(裸|色情|情色|性行为|暴露隐私部位)")));

    /** 合规配图必须携带的水印文案（前端/后端渲染时叠加）。 */
    public static final String WATERMARK = "AI 生成 · 非诊断依据";

    /** 合规配图必须携带的免责声明。 */
    public static final String DISCLAIMER =
            "本图为人工智能生成的科普示意图，不构成诊断或治疗依据；如有不适请到正规医疗机构就诊。";

    private static final Pattern EVASION = Pattern.compile("[\\s\\u3000*·_\\-|/\\\\]+");

    private ImageSafetyGuard() {
    }

    public record Rule(String label, Pattern pattern) {
    }

    /** 命中的规则标签（空集=通过）。 */
    public static Set<String> violations(String prompt) {
        Set<String> hits = new LinkedHashSet<>();
        if (prompt == null || prompt.isBlank()) {
            return hits;
        }
        String normalized = EVASION.matcher(prompt).replaceAll("");
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(normalized).find()) {
                hits.add(rule.label());
            }
        }
        return hits;
    }

    public static boolean isAllowed(String prompt) {
        return violations(prompt).isEmpty();
    }

    /**
     * 审核并构造生成请求。命中规则抛 {@link AgentBlockedException}，
     * 与文本护栏共用同一异常类型，便于上层统一兜底与审计。
     */
    public static ImagePrompt check(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new AgentBlockedException("image-blocked:empty-prompt");
        }
        Set<String> hits = violations(prompt);
        if (!hits.isEmpty()) {
            throw new AgentBlockedException("image-blocked:" + String.join(",", hits));
        }
        return new ImagePrompt(prompt.trim(), WATERMARK, DISCLAIMER);
    }

    /** 合规请求的最终形态：提示词 + 必须叠加的水印与免责声明。 */
    public record ImagePrompt(String prompt, String watermark, String disclaimer) {
        public ImagePrompt {
            if (watermark == null || watermark.isBlank()) {
                throw new IllegalArgumentException("配图必须带水印文案");
            }
            if (disclaimer == null || disclaimer.isBlank()) {
                throw new IllegalArgumentException("配图必须带免责声明");
            }
        }

        /** 落库/审计用的最小元数据（不含像素，避免日志体积膨胀）。 */
        public List<String> auditTags() {
            List<String> tags = new ArrayList<>();
            tags.add("ai-generated");
            tags.add("patient-education");
            return tags;
        }
    }
}