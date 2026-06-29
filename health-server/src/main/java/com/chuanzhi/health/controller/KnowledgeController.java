package com.chuanzhi.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.common.Result;
import com.chuanzhi.health.entity.KnowledgeArticle;
import com.chuanzhi.health.entity.KnowledgeCategory;
import com.chuanzhi.health.service.KnowledgeService;
import com.chuanzhi.health.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "知识库", description = "健康知识分类管理与文章发布")
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final SseService sseService;

    @Operation(summary = "获取知识分类列表")
    @GetMapping("/categories")
    public Result<List<KnowledgeCategory>> listCategories() {
        return Result.ok(knowledgeService.listCategories());
    }

    @Operation(summary = "分页查询文章列表")
    @GetMapping("/articles")
    public Result<PageResult<KnowledgeArticle>> listArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        IPage<KnowledgeArticle> result = knowledgeService.pageArticles(page, size, categoryId, keyword);
        return Result.ok(PageResult.of(result));
    }

    @Operation(summary = "获取文章详情")
    @GetMapping("/articles/{id}")
    public Result<KnowledgeArticle> getArticle(@PathVariable Long id) {
        return Result.ok(knowledgeService.getArticle(id));
    }

    @Operation(summary = "创建文章")
    @PostMapping("/articles")
    public Result<KnowledgeArticle> createArticle(@Valid @RequestBody KnowledgeArticle article) {
        KnowledgeArticle created = knowledgeService.createArticle(article);
        sseService.broadcast("knowledge", "article_created", created);
        return Result.ok(created);
    }

    @Operation(summary = "更新文章")
    @PutMapping("/articles/{id}")
    public Result<KnowledgeArticle> updateArticle(@PathVariable Long id, @Valid @RequestBody KnowledgeArticle article) {
        article.setId(id);
        KnowledgeArticle updated = knowledgeService.updateArticle(article);
        sseService.broadcast("knowledge", "article_updated", updated);
        return Result.ok(updated);
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/articles/{id}")
    public Result<?> deleteArticle(@PathVariable Long id) {
        knowledgeService.deleteArticle(id);
        sseService.broadcast("knowledge", "article_deleted", Map.of("id", id));
        return Result.ok();
    }
}
