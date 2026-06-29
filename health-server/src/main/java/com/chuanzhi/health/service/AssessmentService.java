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

    // 管理员端：分页查询评估记录
    IPage<AssessmentRecord> listRecords(int page, int size, Long memberId, String type);

    // 管理员端：创建评估记录
    AssessmentRecord createRecord(AssessmentRecord record);
}
