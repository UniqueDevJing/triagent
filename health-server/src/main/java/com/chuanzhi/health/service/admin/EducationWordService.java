package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.EducationWord;
import com.chuanzhi.health.mapper.EducationWordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EducationWordService {

    private final EducationWordMapper mapper;

    public PageResult<EducationWord> list(int page, int size, String keyword) {
        Page<EducationWord> pg = new Page<>(page, size);
        LambdaQueryWrapper<EducationWord> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(EducationWord::getTerm, keyword).or().like(EducationWord::getDefinition, keyword);
        }
        qw.orderByAsc(EducationWord::getTerm);
        return PageResult.of(mapper.selectPage(pg, qw));
    }

    public EducationWord getById(Long id) {
        EducationWord entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("宣教词不存在");
        return entity;
    }

    public EducationWord create(EducationWord entity) {
        mapper.insert(entity);
        return entity;
    }

    public EducationWord update(EducationWord entity) {
        mapper.updateById(entity);
        return mapper.selectById(entity.getId());
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
