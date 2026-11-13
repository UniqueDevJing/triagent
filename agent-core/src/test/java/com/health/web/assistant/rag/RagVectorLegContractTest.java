package com.health.web.assistant.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-A 向量检索腿的评测证明（杂项补测）。
 *
 * <p>这个提交本身写着"默认关闭，待评测证明"。要证明的核心不是"能编译"，而是：
 * <ol>
 *   <li><b>RRF 的数学性质</b>：两条腿都命中的文档必须排在只有单腿命中的前面（融合的全部意义）；</li>
 *   <li><b>去重</b>：同一文档出现在两条腿里只能出现一次，且保留关键词腿的措辞；</li>
 *   <li><b>退化安全</b>：任一条腿为空/为 null 时不能抛异常，结果等于另一条腿；</li>
 *   <li><b>余弦相似度的边界</b>：零向量、长度不一致不能返回 NaN 或抛异常。</li>
 * </ol>
 */
class RagVectorLegContractTest {

    private static KnowledgeDoc doc(String source, long refId, String title, double score) {
        return new KnowledgeDoc(source, refId, title, title + "-snippet", score);
    }

    // ───────────── RRF 融合 ─────────────

    @Test
    @DisplayName("RRF：两腿都命中的文档必须排在只有单腿命中的前面")
    void documentHitByBothLegsRanksFirst() {
        KnowledgeDoc both = doc("kb", 1, "两腿都命中", 0.9);
        KnowledgeDoc keywordOnly = doc("kb", 2, "只有关键词", 0.8);
        KnowledgeDoc vectorOnly = doc("kb", 3, "只有向量", 0.7);

        List<KnowledgeDoc> fused = RrfFuser.fuse(
                List.of(keywordOnly, both),
                List.of(vectorOnly, both),
                10);

        assertThat(fused).hasSize(3);
        assertThat(fused.get(0).getRefId()).as("RRF 的核心性质").isEqualTo(1L);
    }

    @Test
    @DisplayName("RRF：同一文档出现在两腿只出现一次，且保留关键词腿的措辞")
    void dedupesAndKeepsKeywordWording() {
        KnowledgeDoc keywordVersion = new KnowledgeDoc("kb", 9L, "标题", "关键词腿措辞", 0.5);
        KnowledgeDoc vectorVersion = new KnowledgeDoc("kb", 9L, "标题", "向量腿措辞", 0.9);

        List<KnowledgeDoc> fused = RrfFuser.fuse(List.of(keywordVersion), List.of(vectorVersion), 10);

        assertThat(fused).hasSize(1);
        assertThat(fused.get(0).getSnippet()).isEqualTo("关键词腿措辞");
    }

    @Test
    @DisplayName("RRF：单腿/空腿/null 退化时结果等于另一条腿，不抛异常")
    void degradesWithSingleLeg() {
        List<KnowledgeDoc> leg = List.of(doc("kb", 1, "A", 0.9), doc("kb", 2, "B", 0.5));

        assertThat(RrfFuser.fuse(leg, null, 10)).extracting(KnowledgeDoc::getRefId).containsExactly(1L, 2L);
        assertThat(RrfFuser.fuse(null, leg, 10)).extracting(KnowledgeDoc::getRefId).containsExactly(1L, 2L);
        assertThat(RrfFuser.fuse(null, null, 10)).isEmpty();
        assertThat(RrfFuser.fuse(List.of(), List.of(), 10)).isEmpty();
    }

    @Test
    @DisplayName("RRF：topK 生效，且不修改入参顺序")
    void respectsTopKWithoutMutatingInputs() {
        List<KnowledgeDoc> kw = List.of(doc("kb", 1, "A", 0.9), doc("kb", 2, "B", 0.8), doc("kb", 3, "C", 0.7));
        List<KnowledgeDoc> kwCopy = List.copyOf(kw);

        List<KnowledgeDoc> fused = RrfFuser.fuse(kw, List.of(), 2);

        assertThat(fused).hasSize(2);
        assertThat(kw).as("融合不能改动入参").isEqualTo(kwCopy);
    }

    @Test
    @DisplayName("RRF：排名靠前比分数高更重要（只看名次，不看原始分数）")
    void rankMattersNotRawScore() {
        KnowledgeDoc highScoreLowRank = doc("kb", 1, "高分但排第二", 0.99);
        KnowledgeDoc lowScoreHighRank = doc("kb", 2, "低分但排第一", 0.01);

        List<KnowledgeDoc> fused = RrfFuser.fuse(
                List.of(lowScoreHighRank, highScoreLowRank), List.of(), 10);

        assertThat(fused.get(0).getRefId()).isEqualTo(2L);
    }

    // ───────────── 余弦 ─────────────

    @Test
    @DisplayName("余弦：自身为 1，正交为 0，反向为 -1")
    void cosineBasics() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};

        assertThat(Cosine.cosine(a, a)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(Cosine.cosine(a, b)).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(Cosine.cosine(a, new float[] {-1f, 0f}))
                .isCloseTo(-1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    @DisplayName("余弦：零向量与长度不一致返回 0，不产生 NaN")
    void cosineDegenerateCases() {
        assertThat(Cosine.cosine(new float[] {0f, 0f}, new float[] {1f, 1f})).isZero();
        assertThat(Cosine.cosine(new float[] {1f, 2f}, new float[] {1f, 2f, 3f})).isZero();
        assertThat(Cosine.cosine(null, new float[] {1f})).isZero();
        assertThat(Double.isNaN(Cosine.cosine(new float[] {0f}, new float[] {0f}))).isFalse();
    }
}