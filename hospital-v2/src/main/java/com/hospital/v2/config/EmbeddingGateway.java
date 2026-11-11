package com.hospital.v2.config;

import com.hospital.v2.common.JsonUtils;
import com.hospital.v2.dao.Db;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一 Embedding 出口。
 * - 配置 LLM_API_KEY 且 mode=real：调用 text-embedding-v3（1024 维）。
 * - 否则：本地哈希词向量降级（维度一致，仅用于本地零依赖验证余弦检索）。
 * 每次调用写 ai_usage_log（use_case=embedding）。
 */
@Component
public class EmbeddingGateway {
    private final HospitalProps props;
    private final Db db;
    private EmbeddingModel remote;

    public EmbeddingGateway(HospitalProps props, Db db) {
        this.props = props;
        this.db = db;
        if (shouldUseReal()) {
            this.remote = OpenAiEmbeddingModel.builder()
                    .baseUrl(props.getLlm().getBaseUrl())
                    .apiKey(props.getLlm().getApiKey())
                    .modelName(props.getLlm().getEmbeddingModel())
                    .build();
        }
    }

    /** 仅看配置（构造期用，此时 remote 尚未初始化）。 */
    private boolean shouldUseReal() {
        String mode = props.getLlm().getMode();
        return "real".equalsIgnoreCase(mode)
                || ("auto".equalsIgnoreCase(mode) && props.getLlm().getApiKey() != null && !props.getLlm().getApiKey().isBlank());
    }

    private boolean useReal() {
        return shouldUseReal() && remote != null;
    }

    public float[] embed(String text) {
        long t0 = System.currentTimeMillis();
        try {
            if (useReal()) {
                var resp = remote.embed(text);
                float[] v = resp.content().vector();
                dev.langchain4j.model.output.TokenUsage tu = resp.tokenUsage();
                int tokens = tu == null || tu.totalTokenCount() == null ? 0 : tu.totalTokenCount();
                logUsage("embedding", props.getLlm().getEmbeddingModel(), tokens, 0,
                        (int) (System.currentTimeMillis() - t0), true, null);
                return v;
            }
            float[] v = localEmbed(text);
            logUsage("embedding", "local-hash-" + props.getLlm().getEmbeddingDim(), 0, 0, (int) (System.currentTimeMillis() - t0), true, null);
            return v;
        } catch (Exception e) {
            logUsage("embedding", props.getLlm().getEmbeddingModel(), 0, 0, (int) (System.currentTimeMillis() - t0), false, e.getMessage());
            return localEmbed(text);
        }
    }

    /** 本地确定性哈希向量：按字符 Unigram 哈希（中文无空格，整串做 token 会失配）→ 桶累加 → L2 归一化。 */
    private float[] localEmbed(String text) {
        int dim = props.getLlm().getEmbeddingDim();
        float[] v = new float[dim];
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isLetterOrDigit(c)) continue;
            int h = hash(String.valueOf(c), 0) % dim;
            if (h < 0) h += dim;
            v[h] += 1f;
        }
        double norm = 0;
        for (float x : v) norm += x * x;
        norm = Math.sqrt(norm) + 1e-9;
        for (int i = 0; i < dim; i++) v[i] = (float) (v[i] / norm);
        return v;
    }

    private int hash(String s, int i) {
        return (s.hashCode() * 31 + i * 2654435761L) > Integer.MAX_VALUE
                ? (int) ((s.hashCode() * 31L + i * 2654435761L) % Integer.MAX_VALUE)
                : (int) (s.hashCode() * 31L + i * 2654435761L);
    }

    private void logUsage(String useCase, String model, int pt, int ct, int ms, boolean ok, String err) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("use_case", useCase);
        m.put("model", model);
        m.put("prompt_tokens", pt);
        m.put("completion_tokens", ct);
        m.put("total_tokens", pt + ct);
        m.put("cost_ms", ms);
        m.put("success", ok ? 1 : 0);
        m.put("error_msg", err == null ? "" : (err.length() > 200 ? err.substring(0, 200) : err));
        db.insert("INSERT INTO ai_usage_log (use_case, model, prompt_tokens, completion_tokens, total_tokens, cost_ms, success, error_msg) "
                + "VALUES (:use_case,:model,:prompt_tokens,:completion_tokens,:total_tokens,:cost_ms,:success,:error_msg)", m);
    }

    /** 余弦相似度。 */
    public static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public static String toJson(float[] v) {
        return JsonUtils.toJson(v);
    }

    @SuppressWarnings("unchecked")
    public static float[] fromJson(String s) {
        if (s == null || s.isBlank()) return new float[0];
        java.util.List<Number> list = JsonUtils.fromJson(s, java.util.List.class);
        if (list == null) return new float[0];
        float[] v = new float[list.size()];
        for (int i = 0; i < list.size(); i++) v[i] = list.get(i).floatValue();
        return v;
    }
}
