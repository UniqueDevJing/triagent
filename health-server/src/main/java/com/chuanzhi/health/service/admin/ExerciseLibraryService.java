package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.ExerciseLibrary;
import com.chuanzhi.health.mapper.ExerciseLibraryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExerciseLibraryService {

    private final ExerciseLibraryMapper mapper;

    public PageResult<ExerciseLibrary> list(int page, int size, String keyword, String category) {
        Page<ExerciseLibrary> pg = new Page<>(page, size);
        LambdaQueryWrapper<ExerciseLibrary> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(ExerciseLibrary::getName, keyword);
        }
        if (category != null && !category.isEmpty()) {
            qw.eq(ExerciseLibrary::getCategory, category);
        }
        qw.orderByAsc(ExerciseLibrary::getCategory).orderByAsc(ExerciseLibrary::getName);
        return PageResult.of(mapper.selectPage(pg, qw));
    }

    public ExerciseLibrary getById(Long id) {
        ExerciseLibrary entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("运动项目不存在");
        return entity;
    }

    public ExerciseLibrary create(ExerciseLibrary entity) {
        mapper.insert(entity);
        return entity;
    }

    public ExerciseLibrary update(ExerciseLibrary entity) {
        mapper.updateById(entity);
        return mapper.selectById(entity.getId());
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
