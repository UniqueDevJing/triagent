package com.chuanzhi.health.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.entity.KnowledgeArticle;
import com.chuanzhi.health.entity.KnowledgeCategory;

import java.util.List;

public interface KnowledgeService {
    List<KnowledgeCategory> listCategories();
    IPage<KnowledgeArticle> pageArticles(int page, int size, Long categoryId, String keyword);
    KnowledgeArticle getArticle(Long id);
    KnowledgeArticle createArticle(KnowledgeArticle article);
    KnowledgeArticle updateArticle(KnowledgeArticle article);
    void deleteArticle(Long id);
}
