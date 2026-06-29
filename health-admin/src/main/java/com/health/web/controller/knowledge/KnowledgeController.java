package com.health.web.controller.knowledge;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.KnowledgeArticle;
import com.health.system.mapper.KnowledgeArticleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge/article")
public class KnowledgeController extends BaseController {
    private final KnowledgeArticleMapper knowledgeArticleMapper;
    public KnowledgeController(KnowledgeArticleMapper knowledgeArticleMapper) { this.knowledgeArticleMapper = knowledgeArticleMapper; }

    @GetMapping
    @Log(title = "知识文章查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<KnowledgeArticle> p = knowledgeArticleMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<KnowledgeArticle>().orderByDesc(KnowledgeArticle::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(knowledgeArticleMapper.selectById(id)); }

    @PostMapping
    @Log(title = "新增知识文章")
    public AjaxResult create(@RequestBody KnowledgeArticle article) { knowledgeArticleMapper.insert(article); return success(article); }

    @PutMapping
    @Log(title = "修改知识文章")
    public AjaxResult update(@RequestBody KnowledgeArticle article) { knowledgeArticleMapper.updateById(article); return success(); }

    @DeleteMapping("/{ids}")
    @Log(title = "删除知识文章")
    public AjaxResult delete(@PathVariable List<Long> ids) { knowledgeArticleMapper.deleteBatchIds(ids); return success(); }
}
