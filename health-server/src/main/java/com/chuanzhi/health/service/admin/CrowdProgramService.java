package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.CrowdProgram;
import com.chuanzhi.health.mapper.CrowdProgramMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CrowdProgramService {

    private final CrowdProgramMapper crowdProgramMapper;

    public PageResult<CrowdProgram> list(int page, int size, String name) {
        Page<CrowdProgram> pg = new Page<>(page, size);
        LambdaQueryWrapper<CrowdProgram> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            qw.like(CrowdProgram::getName, name);
        }
        qw.orderByDesc(CrowdProgram::getCreatedAt);
        return PageResult.of(crowdProgramMapper.selectPage(pg, qw));
    }

    public CrowdProgram getById(Long id) {
        CrowdProgram entity = crowdProgramMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("群体方案不存在");
        }
        return entity;
    }

    public CrowdProgram create(CrowdProgram entity) {
        crowdProgramMapper.insert(entity);
        return entity;
    }

    public CrowdProgram update(CrowdProgram entity) {
        crowdProgramMapper.updateById(entity);
        return crowdProgramMapper.selectById(entity.getId());
    }

    public void delete(Long id) {
        crowdProgramMapper.deleteById(id);
    }
}
