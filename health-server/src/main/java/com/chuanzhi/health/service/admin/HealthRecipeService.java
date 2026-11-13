package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.HealthRecipe;
import com.chuanzhi.health.mapper.HealthRecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthRecipeService {

    private final HealthRecipeMapper mapper;

    public PageResult<HealthRecipe> list(int page, int size, String keyword, String category) {
        Page<HealthRecipe> pg = new Page<>(page, size);
        LambdaQueryWrapper<HealthRecipe> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(HealthRecipe::getName, keyword);
        }
        if (category != null && !category.isEmpty()) {
            qw.eq(HealthRecipe::getCategory, category);
        }
        qw.orderByDesc(HealthRecipe::getCreatedAt);
        return PageResult.of(mapper.selectPage(pg, qw));
    }

    public HealthRecipe getById(Long id) {
        HealthRecipe entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("食谱不存在");
        return entity;
    }

    public HealthRecipe create(HealthRecipe entity) {
        mapper.insert(entity);
        return entity;
    }

    public HealthRecipe update(HealthRecipe entity) {
        mapper.updateById(entity);
        return mapper.selectById(entity.getId());
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
