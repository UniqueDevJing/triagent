package com.chuanzhi.health.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanzhi.health.dto.AssessmentRequest;
import com.chuanzhi.health.entity.AssessmentRecord;
import com.chuanzhi.health.entity.AssessmentTemplate;
import com.chuanzhi.health.enums.RiskLevel;
import com.chuanzhi.health.mapper.AssessmentRecordMapper;
import com.chuanzhi.health.mapper.AssessmentTemplateMapper;
import com.chuanzhi.health.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentTemplateMapper templateMapper;
    private final AssessmentRecordMapper recordMapper;

    @Override
    public List<AssessmentTemplate> listTemplates() {
        return templateMapper.selectList(null);
    }

    @Override
    public AssessmentTemplate getTemplate(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    @Transactional
    public AssessmentRecord submitAssessment(AssessmentRequest request) {
        AssessmentTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new IllegalArgumentException("评估量表不存在");
        }

        // 计算总分
        JSONArray questions = JSONUtil.parseArray(template.getQuestions());
        int[] totalScore = {0};
        request.getAnswers().forEach((qId, score) -> totalScore[0] += score);

        // 判断风险等级
        JSONObject rules = JSONUtil.parseObj(template.getScoringRules());
        RiskLevel riskLevel = RiskLevel.LOW;
        if (totalScore[0] > rules.getJSONObject("HIGH").getInt("min")) {
            riskLevel = RiskLevel.HIGH;
        } else if (totalScore[0] > rules.getJSONObject("MEDIUM").getInt("min")) {
            riskLevel = RiskLevel.MEDIUM;
        }

        String riskDesc = rules.getJSONObject(riskLevel.name()).getStr("desc");

        AssessmentRecord record = new AssessmentRecord();
        record.setUserId(request.getUserId());
        record.setTemplateId(request.getTemplateId());
        record.setAnswers(JSONUtil.toJsonStr(request.getAnswers()));
        record.setTotalScore(BigDecimal.valueOf(totalScore[0]));
        record.setRiskLevel(riskLevel);
        record.setReportText("评估得分: " + totalScore[0] + "分，风险等级: " + riskLevel + " - " + riskDesc);
        recordMapper.insert(record);

        return record;
    }

    @Override
    public IPage<AssessmentRecord> pageRecords(int page, int size, Long userId) {
        LambdaQueryWrapper<AssessmentRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(AssessmentRecord::getUserId, userId);
        }
        wrapper.orderByDesc(AssessmentRecord::getCreatedAt);
        return recordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public AssessmentRecord getRecordDetail(Long id) {
        return recordMapper.selectById(id);
    }
}
