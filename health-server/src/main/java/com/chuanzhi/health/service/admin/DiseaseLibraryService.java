package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.DiseaseLibrary;
import com.chuanzhi.health.mapper.DiseaseLibraryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiseaseLibraryService {

    private final DiseaseLibraryMapper mapper;

    public PageResult<DiseaseLibrary> list(int page, int size, String keyword, String category) {
        Page<DiseaseLibrary> pg = new Page<>(page, size);
        LambdaQueryWrapper<DiseaseLibrary> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(DiseaseLibrary::getName, keyword);
        }
        if (category != null && !category.isEmpty()) {
            qw.eq(DiseaseLibrary::getCategory, category);
        }
        qw.orderByAsc(DiseaseLibrary::getName);
        return PageResult.of(mapper.selectPage(pg, qw));
    }

    public DiseaseLibrary getById(Long id) {
        DiseaseLibrary entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("疾病不存在");
        return entity;
    }

    public DiseaseLibrary create(DiseaseLibrary entity) {
        mapper.insert(entity);
        return entity;
    }

    public DiseaseLibrary update(DiseaseLibrary entity) {
        mapper.updateById(entity);
        return mapper.selectById(entity.getId());
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
