package com.health.web.assistant.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 混合知识检索（T-A 向量检索腿的编排入口）：关键词腿 + 向量腿 RRF 融合。
 *
 * <p>门控与降级策略（与 M4 可证伪原则一致）：
 * <ul>
 *   <li>{@code assistant.vector-rag.enabled=false}（默认）时本 Bean 不加载，
 *       系统行为与改造前完全一致（纯关键词检索）；</li>
 *   <li>向量腿嵌入失败/不可用时静默降级为纯关键词——RAG 永远不因增强腿挂掉而不可用；</li>
 *   <li>向量腿是否真的带来增量，须以 docs/agent/向量检索评测方案.md 的 mini 评测
 *       在部署环境跑出数字后再决定是否长期开启。</li>
 * </ul>
 */
@Component
@Primary
@ConditionalOnProperty(name = "assistant.vector-rag.enabled", havingValue = "true")
public class HybridKnowledgeRetriever implements KnowledgeRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridKnowledgeRetriever.class);

    private final KeywordKnowledgeRetriever keywordRetriever;
    private final InMemoryVectorKnowledgeRetriever vectorRetriever;

    public HybridKnowledgeRetriever(KeywordKnowledgeRetriever keywordRetriever,
                                    InMemoryVectorKnowledgeRetriever vectorRetriever) {
        this.keywordRetriever = keywordRetriever;
        this.vectorRetriever = vectorRetriever;
    }

    @Override
    public List<KnowledgeDoc> retrieve(String query, int topK) {
        List<KnowledgeDoc> keywordLeg = keywordRetriever.retrieve(query, topK);
        List<KnowledgeDoc> vectorLeg = List.of();
        try {
            vectorLeg = vectorRetriever.retrieve(query, topK);
        } catch (Exception e) {
            log.warn("[vector-rag] 向量腿失败，降级纯关键词: {}", e.getMessage());
        }
        List<KnowledgeDoc> fused = RrfFuser.fuse(keywordLeg, vectorLeg, topK);
        log.debug("[vector-rag] query={} keyword={} vector={} fused={}",
                query, keywordLeg.size(), vectorLeg.size(), fused.size());
        return fused;
    }
}
