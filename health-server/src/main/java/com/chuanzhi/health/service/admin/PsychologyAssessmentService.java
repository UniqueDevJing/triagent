package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.PsychologyAssessment;
import com.chuanzhi.health.mapper.PsychologyAssessmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PsychologyAssessmentService {

    private final PsychologyAssessmentMapper psychologyAssessmentMapper;

    public PageResult<PsychologyAssessment> list(int page, int size) {
        Page<PsychologyAssessment> pg = new Page<>(page, size);
        LambdaQueryWrapper<PsychologyAssessment> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(PsychologyAssessment::getId);
        return PageResult.of(psychologyAssessmentMapper.selectPage(pg, qw));
    }

    public PsychologyAssessment getById(Long id) {
        PsychologyAssessment assessment = psychologyAssessmentMapper.selectById(id);
        if (assessment == null) {
            throw new BusinessException("心理评估不存在");
        }
        return assessment;
    }

    public PsychologyAssessment create(PsychologyAssessment assessment) {
        psychologyAssessmentMapper.insert(assessment);
        return assessment;
    }

    public PsychologyAssessment update(PsychologyAssessment assessment) {
        PsychologyAssessment existing = psychologyAssessmentMapper.selectById(assessment.getId());
        if (existing == null) {
            throw new BusinessException("心理评估不存在");
        }
        psychologyAssessmentMapper.updateById(assessment);
        return psychologyAssessmentMapper.selectById(assessment.getId());
    }

    public void delete(Long id) {
        PsychologyAssessment existing = psychologyAssessmentMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("心理评估不存在");
        }
        psychologyAssessmentMapper.deleteById(id);
    }
}
