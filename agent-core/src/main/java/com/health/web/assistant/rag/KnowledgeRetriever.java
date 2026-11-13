package com.health.web.assistant.rag;

import java.util.List;

/**
 * 知识检索器抽象（Phase3）。
 * 当前实现为 MySQL 关键词/词频混合检索（KeywordKnowledgeRetriever）；
 * 后续可增加向量实现（VectorRetriever，如 MySQL9 VECTOR / pgvector）而不改动调用方。
 */
public interface KnowledgeRetriever {

    /**
     * 检索与 query 相关的知识文档。
     *
     * @param query 用户主诉/问题文本
     * @param topK  返回条数上限
     */
    List<KnowledgeDoc> retrieve(String query, int topK);
}
