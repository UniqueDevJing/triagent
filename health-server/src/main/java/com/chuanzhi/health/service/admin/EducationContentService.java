package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.EducationContent;
import com.chuanzhi.health.mapper.EducationContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EducationContentService {

    private final EducationContentMapper mapper;

    public PageResult<EducationContent> list(int page, int size, String keyword, String type) {
        Page<EducationContent> pg = new Page<>(page, size);
        LambdaQueryWrapper<EducationContent> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(EducationContent::getTitle, keyword);
        }
        if (type != null && !type.isEmpty()) {
            qw.eq(EducationContent::getType, type);
        }
        qw.orderByDesc(EducationContent::getCreatedAt);
        return PageResult.of(mapper.selectPage(pg, qw));
    }

    public EducationContent getById(Long id) {
        EducationContent entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("宣教内容不存在");
        return entity;
    }

    public EducationContent create(EducationContent entity) {
        mapper.insert(entity);
        return entity;
    }

    public EducationContent update(EducationContent entity) {
        mapper.updateById(entity);
        return mapper.selectById(entity.getId());
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
