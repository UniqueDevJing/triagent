package com.chuanzhi.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.entity.KnowledgeArticle;
import com.chuanzhi.health.entity.KnowledgeCategory;
import com.chuanzhi.health.mapper.KnowledgeArticleMapper;
import com.chuanzhi.health.mapper.KnowledgeCategoryMapper;
import com.chuanzhi.health.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeArticleMapper articleMapper;

    @Override
    @Cacheable(value = "knowledge:categories", unless = "#result.isEmpty()")
    public List<KnowledgeCategory> listCategories() {
        return categoryMapper.selectList(
            new LambdaQueryWrapper<KnowledgeCategory>().orderByAsc(KnowledgeCategory::getSortOrder)
        );
    }

    @Override
    @Cacheable(value = "knowledge:articles", key = "#page + ':' + #size + ':' + #categoryId + ':' + #keyword", unless = "#result.records.isEmpty()")
    public IPage<KnowledgeArticle> pageArticles(int page, int size, Long categoryId, String keyword) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(KnowledgeArticle::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(KnowledgeArticle::getTitle, keyword)
                            .or().like(KnowledgeArticle::getSummary, keyword));
        }
        wrapper.orderByDesc(KnowledgeArticle::getCreatedAt);
        return articleMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public KnowledgeArticle getArticle(Long id) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article != null) {
            article.setViewCount(article.getViewCount() + 1);
            articleMapper.updateById(article);
        }
        return article;
    }

    @Override
    @Transactional
    @CacheEvict(value = { "knowledge:articles", "dashboard:stats" }, allEntries = true)
    public KnowledgeArticle createArticle(KnowledgeArticle article) {
        article.setViewCount(0);
        articleMapper.insert(article);
        return article;
    }

    @Override
    @CacheEvict(value = { "knowledge:articles", "dashboard:stats" }, allEntries = true)
    public KnowledgeArticle updateArticle(KnowledgeArticle article) {
        articleMapper.updateById(article);
        return articleMapper.selectById(article.getId());
    }

    @Override
    @CacheEvict(value = { "knowledge:articles", "dashboard:stats" }, allEntries = true)
    public void deleteArticle(Long id) {
        articleMapper.deleteById(id);
    }
}
