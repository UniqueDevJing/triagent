package com.health.web.assistant.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RRF（Reciprocal Rank Fusion）融合（T-A）：把关键词腿与向量腿的两份排序结果
 * 融合成一份，score = Σ 1/(k + rank)，k=60 为论文标准值。
 *
 * <p>选 RRF 而不是分数加权的原因：两条腿的分数量纲不同（关键词是命中权重，
 * 向量是余弦相似度），直接加权要做归一化校准；RRF 只看排名，免校准、可解释。
 */
public final class RrfFuser {

    private RrfFuser() {
    }

    /** 融合常数（论文标准 60）：越大排名差异越平缓 */
    public static final int DEFAULT_K = 60;

    /**
     * @param keywordLeg 关键词腿排序结果（可空）
     * @param vectorLeg  向量腿排序结果（可空）
     * @param topK       返回条数上限
     */
    public static List<KnowledgeDoc> fuse(List<KnowledgeDoc> keywordLeg, List<KnowledgeDoc> vectorLeg, int topK) {
        List<KnowledgeDoc> a = keywordLeg == null ? List.of() : keywordLeg;
        List<KnowledgeDoc> b = vectorLeg == null ? List.of() : vectorLeg;
        Map<String, Double> scores = new HashMap<>();
        Map<String, KnowledgeDoc> docs = new HashMap<>();
        accumulate(a, scores, docs);
        accumulate(b, scores, docs);
        List<KnowledgeDoc> fused = new ArrayList<>(docs.values());
        fused.sort(Comparator.comparingDouble((KnowledgeDoc d) ->
                scores.get(key(d))).reversed());
        return fused.size() <= topK ? fused : new ArrayList<>(fused.subList(0, topK));
    }

    private static void accumulate(List<KnowledgeDoc> leg, Map<String, Double> scores, Map<String, KnowledgeDoc> docs) {
        for (int i = 0; i < leg.size(); i++) {
            KnowledgeDoc d = leg.get(i);
            String k = key(d);
            scores.merge(k, 1.0 / (DEFAULT_K + i + 1), Double::sum);
            // 首见文档优先（关键词腿先算，保住它的 snippet 措辞）
            docs.putIfAbsent(k, d);
        }
    }

    private static String key(KnowledgeDoc d) {
        return d.getSource() + "#" + d.getRefId();
    }
}
