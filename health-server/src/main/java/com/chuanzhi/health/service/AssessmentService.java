package com.chuanzhi.health.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.chuanzhi.health.dto.AssessmentRequest;
import com.chuanzhi.health.entity.AssessmentRecord;
import com.chuanzhi.health.entity.AssessmentTemplate;

import java.util.List;

public interface AssessmentService {
    List<AssessmentTemplate> listTemplates();
    AssessmentTemplate getTemplate(Long id);
    AssessmentRecord submitAssessment(AssessmentRequest request);
    IPage<AssessmentRecord> pageRecords(int page, int size, Long userId);
    AssessmentRecord getRecordDetail(Long id);
}
