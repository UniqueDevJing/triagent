package com.health.web.assistant.trace;

import java.util.regex.Pattern;

/**
 * T6 敏感信息脱敏规则（Trace/审计/日志统一使用）。
 * 规则：
 * <ul>
 *   <li>手机号（1[3-9] 开头 11 位）：保留前 3 后 4；</li>
 *   <li>身份证号（15/18 位）：保留前 4 后 2；</li>
 *   <li>邮箱：保留首字符与域名；</li>
 *   <li>JWT / 长密钥串（eyJ 开头或 32 位以上连续字母数字）：整体打码；</li>
 *   <li>字段名含 key/token/secret/password/authorization：值整体打码（由 redactField 处理）。</li>
 * </ul>
 * 脱敏不可逆，且对已脱敏文本幂等（重复处理不二次破坏）。
 */
public final class SensitiveRedactor {

    private static final Pattern PHONE = Pattern.compile("(?<![0-9])(1[3-9][0-9])[0-9]{4}([0-9]{4})(?![0-9])");
    private static final Pattern ID_CARD = Pattern.compile("(?<![0-9Xx])([0-9Xx]{18}|[0-9]{15})(?![0-9Xx])");
    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+)");
    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{5,}");
    private static final Pattern LONG_SECRET = Pattern.compile("(?<![A-Za-z0-9])[A-Za-z0-9]{32,}(?![A-Za-z0-9])");
    private static final Pattern SENSITIVE_FIELD = Pattern.compile(
            "(?i).*(api[_-]?key|token|secret|password|passwd|authorization|credential).*");

    private SensitiveRedactor() {
    }

    /** 字段级脱敏：字段名命中敏感关键词 → 值整体打码；否则做文本规则脱敏 */
    public static String redactField(String fieldName, String value) {
        if (value == null) {
            return null;
        }
        if (fieldName != null && SENSITIVE_FIELD.matcher(fieldName).matches()) {
            return "***REDACTED***";
        }
        return redact(value);
    }

    /** 文本规则脱敏（幂等：对已打码文本重复处理结果不变） */
    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = text;
        out = JWT.matcher(out).replaceAll("***JWT***");
        out = PHONE.matcher(out).replaceAll("$1****$2");
        // 身份证：整段匹配后保留前4后2（正则替换无法按组计算长度，用 Matcher 回调）
        java.util.regex.Matcher idm = ID_CARD.matcher(out);
        StringBuilder idSb = new StringBuilder();
        while (idm.find()) {
            String id = idm.group();
            String masked = id.substring(0, 4) + "**********" + id.substring(id.length() - 2);
            idm.appendReplacement(idSb, java.util.regex.Matcher.quoteReplacement(masked));
        }
        idm.appendTail(idSb);
        out = idSb.toString();
        out = EMAIL.matcher(out).replaceAll("$1***$2");
        out = LONG_SECRET.matcher(out).replaceAll("***SECRET***");
        return out;
    }
}
