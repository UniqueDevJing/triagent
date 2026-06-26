package com.chuanzhi.health.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.common.BusinessException;
import com.chuanzhi.health.common.PageResult;
import com.chuanzhi.health.entity.AssessmentIndicator;
import com.chuanzhi.health.mapper.AssessmentIndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentIndicatorService {

    private final AssessmentIndicatorMapper assessmentIndicatorMapper;

    public PageResult<AssessmentIndicator> list(int page, int size, String category) {
        Page<AssessmentIndicator> pg = new Page<>(page, size);
        LambdaQueryWrapper<AssessmentIndicator> qw = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            qw.eq(AssessmentIndicator::getCategory, category);
        }
        qw.orderByAsc(AssessmentIndicator::getId);
        return PageResult.of(assessmentIndicatorMapper.selectPage(pg, qw));
    }

    public AssessmentIndicator getById(Long id) {
        AssessmentIndicator indicator = assessmentIndicatorMapper.selectById(id);
        if (indicator == null) {
            throw new BusinessException("评估指标不存在");
        }
        return indicator;
    }

    public AssessmentIndicator create(AssessmentIndicator indicator) {
        assessmentIndicatorMapper.insert(indicator);
        return indicator;
    }

    public AssessmentIndicator update(AssessmentIndicator indicator) {
        AssessmentIndicator existing = assessmentIndicatorMapper.selectById(indicator.getId());
        if (existing == null) {
            throw new BusinessException("评估指标不存在");
        }
        assessmentIndicatorMapper.updateById(indicator);
        return assessmentIndicatorMapper.selectById(indicator.getId());
    }

    public void delete(Long id) {
        AssessmentIndicator existing = assessmentIndicatorMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("评估指标不存在");
        }
        assessmentIndicatorMapper.deleteById(id);
    }

    public List<AssessmentIndicator> getAll() {
        return assessmentIndicatorMapper.selectList(null);
    }
}
