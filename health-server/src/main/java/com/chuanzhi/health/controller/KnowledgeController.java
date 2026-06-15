package com.chuanzhi.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.KnowledgeArticle;
import com.chuanzhi.health.entity.KnowledgeCategory;
import com.chuanzhi.health.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/categories")
    public Result<List<KnowledgeCategory>> listCategories() {
        return Result.ok(knowledgeService.listCategories());
    }

    @GetMapping("/articles")
    public Result<PageResult<KnowledgeArticle>> listArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        IPage<KnowledgeArticle> result = knowledgeService.pageArticles(page, size, categoryId, keyword);
        return Result.ok(PageResult.of(result));
    }

    @GetMapping("/articles/{id}")
    public Result<KnowledgeArticle> getArticle(@PathVariable Long id) {
        return Result.ok(knowledgeService.getArticle(id));
    }

    @PostMapping("/articles")
    public Result<KnowledgeArticle> createArticle(@Valid @RequestBody KnowledgeArticle article) {
        return Result.ok(knowledgeService.createArticle(article));
    }

    @PutMapping("/articles/{id}")
    public Result<KnowledgeArticle> updateArticle(@PathVariable Long id, @Valid @RequestBody KnowledgeArticle article) {
        article.setId(id);
        return Result.ok(knowledgeService.updateArticle(article));
    }

    @DeleteMapping("/articles/{id}")
    public Result<?> deleteArticle(@PathVariable Long id) {
        knowledgeService.deleteArticle(id);
        return Result.ok();
    }
}
