package com.chuanzhi.health.service;

import com.chuanzhi.health.dto.AssessmentRequest;
import com.chuanzhi.health.entity.AssessmentRecord;
import com.chuanzhi.health.entity.AssessmentTemplate;
import com.chuanzhi.health.enums.RiskLevel;
import com.chuanzhi.health.mapper.AssessmentTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AssessmentServiceTest {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AssessmentTemplateMapper templateMapper;

    private Long templateId;

    @BeforeEach
    void setUp() {
        AssessmentTemplate template = new AssessmentTemplate();
        template.setTitle("测试评估量表");
        template.setDescription("用于单元测试");
        template.setCategory("综合评估");
        template.setQuestions("""
            [
                {"id":1,"text":"日常生活能力","options":[{"label":"自理","score":0},{"label":"部分依赖","score":5},{"label":"完全依赖","score":10}]},
                {"id":2,"text":"跌倒风险","options":[{"label":"无","score":0},{"label":"偶尔","score":5},{"label":"频繁","score":10}]}
            ]""");
        template.setScoringRules("""
            {
                "LOW":{"min":0,"desc":"低风险，保持现状"},
                "MEDIUM":{"min":5,"desc":"中等风险，建议关注"},
                "HIGH":{"min":10,"desc":"高风险，需立即干预"}
            }""");
        templateMapper.insert(template);
        templateId = template.getId();
    }

    @Test
    void shouldReturnLowRiskWhenScoreIsLow() {
        AssessmentRequest req = new AssessmentRequest();
        req.setUserId(1L);
        req.setTemplateId(templateId);
        Map<Long, Integer> answers = new HashMap<>();
        answers.put(1L, 0);
        answers.put(2L, 0);
        req.setAnswers(answers);

        AssessmentRecord record = assessmentService.submitAssessment(req);

        assertNotNull(record.getId());
        assertEquals(RiskLevel.LOW, record.getRiskLevel());
        assertEquals(0, record.getTotalScore().intValue());
        assertTrue(record.getReportText().contains("低风险"));
    }

    @Test
    void shouldReturnMediumRiskWhenScoreIsMedium() {
        AssessmentRequest req = new AssessmentRequest();
        req.setUserId(2L);
        req.setTemplateId(templateId);
        Map<Long, Integer> answers = new HashMap<>();
        answers.put(1L, 5);
        answers.put(2L, 2);
        req.setAnswers(answers);

        AssessmentRecord record = assessmentService.submitAssessment(req);

        assertEquals(RiskLevel.MEDIUM, record.getRiskLevel());
        assertEquals(7, record.getTotalScore().intValue());
    }

    @Test
    void shouldReturnHighRiskWhenScoreExceedsHighThreshold() {
        AssessmentRequest req = new AssessmentRequest();
        req.setUserId(3L);
        req.setTemplateId(templateId);
        Map<Long, Integer> answers = new HashMap<>();
        answers.put(1L, 10);
        answers.put(2L, 5);
        req.setAnswers(answers);

        AssessmentRecord record = assessmentService.submitAssessment(req);

        assertEquals(RiskLevel.HIGH, record.getRiskLevel());
        assertEquals(15, record.getTotalScore().intValue());
        assertTrue(record.getReportText().contains("高风险"));
    }

    @Test
    void shouldThrowWhenTemplateNotFound() {
        AssessmentRequest req = new AssessmentRequest();
        req.setUserId(1L);
        req.setTemplateId(99999L);
        req.setAnswers(Map.of(1L, 0));

        assertThrows(IllegalArgumentException.class, () -> assessmentService.submitAssessment(req));
    }

    @Test
    void shouldListAllTemplates() {
        var templates = assessmentService.listTemplates();
        assertFalse(templates.isEmpty());
        assertTrue(templates.stream().anyMatch(t -> t.getId().equals(templateId)));
    }
}
