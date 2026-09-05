package com.health.web.assistant.rag;

/**
 * 知识检索结果文档（Phase3 RAG）。
 * 统一抽象：无论关键词检索还是未来的向量检索，都归一为该结构供注入/引用。
 */
public class KnowledgeDoc {

    /** 来源类型，如 疾病库 / 科普文章 */
    private final String source;
    /** 来源记录 ID */
    private final Long refId;
    /** 标题 */
    private final String title;
    /** 摘要片段 */
    private final String snippet;
    /** 相关度得分 */
    private final double score;

    public KnowledgeDoc(String source, Long refId, String title, String snippet, double score) {
        this.source = source;
        this.refId = refId;
        this.title = title;
        this.snippet = snippet;
        this.score = score;
    }

    public String getSource() {
        return source;
    }

    public Long getRefId() {
        return refId;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public double getScore() {
        return score;
    }
}
