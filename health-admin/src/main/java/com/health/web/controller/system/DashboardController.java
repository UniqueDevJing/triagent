package com.health.web.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.common.core.AjaxResult;
import com.health.system.domain.AssessmentRecord;
import com.health.system.domain.InterventionPlan;
import com.health.system.domain.KnowledgeArticle;
import com.health.system.mapper.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;
    private final AssessmentRecordMapper assessmentMapper;
    private final InterventionPlanMapper interventionMapper;
    private final KnowledgeArticleMapper knowledgeMapper;

    public DashboardController(JdbcTemplate jdbcTemplate,
                               AssessmentRecordMapper assessmentMapper,
                               InterventionPlanMapper interventionMapper,
                               KnowledgeArticleMapper knowledgeMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.assessmentMapper = assessmentMapper;
        this.interventionMapper = interventionMapper;
        this.knowledgeMapper = knowledgeMapper;
    }

    @GetMapping("/stats")
    public AjaxResult stats() {
        Map<String, Object> data = new HashMap<>();

        Long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Long.class);
        data.put("totalUsers", totalUsers != null ? totalUsers : 0);

        Long todayAssessments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_record WHERE assess_date = CURDATE()", Long.class);
        data.put("todayAssessments", todayAssessments != null ? todayAssessments : 0);

        Long activeInterventions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM intervention_plan WHERE status = 'ACTIVE'", Long.class);
        data.put("activeInterventions", activeInterventions != null ? activeInterventions : 0);

        Long knowledgeArticles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_article WHERE status = 'PUBLISHED'", Long.class);
        data.put("knowledgeArticles", knowledgeArticles != null ? knowledgeArticles : 0);

        // 7日趋势：空数据时返回最近7天全0值
        List<Map<String, Object>> trend = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(assess_date, '%m-%d') AS date, COUNT(*) AS count " +
                "FROM assessment_record WHERE assess_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                "GROUP BY assess_date ORDER BY assess_date");
        if (trend.isEmpty()) {
            trend = buildDefaultWeekTrend();
        }
        data.put("assessmentTrend", trend);

        // 年龄分布：空数据时返回默认结构（全0值）
        List<Map<String, Object>> ageDistribution = jdbcTemplate.queryForList(
                "SELECT name, COUNT(*) AS value FROM (" +
                "SELECT CASE WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) < 18 THEN '未成年' " +
                "WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) BETWEEN 18 AND 35 THEN '18-35岁' " +
                "WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) BETWEEN 36 AND 50 THEN '36-50岁' " +
                "WHEN TIMESTAMPDIFF(YEAR, birthday, CURDATE()) BETWEEN 51 AND 65 THEN '51-65岁' " +
                "ELSE '65岁以上' END AS name FROM member WHERE birthday IS NOT NULL) t " +
                "GROUP BY name ORDER BY name");
        if (ageDistribution.isEmpty()) {
            ageDistribution = buildDefaultAgeDistribution();
        }
        data.put("ageDistribution", ageDistribution);

        // 最近动态：空数据时返回引导提示
        data.put("recentActivities", buildRecentActivities());

        return AjaxResult.success(data);
    }

    private List<Map<String, Object>> buildDefaultWeekTrend() {
        List<Map<String, Object>> list = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", LocalDate.now().minusDays(i).format(fmt));
            item.put("count", 0);
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> buildDefaultAgeDistribution() {
        return List.of(
                Map.of("name", "未成年", "value", 0),
                Map.of("name", "18-35岁", "value", 0),
                Map.of("name", "36-50岁", "value", 0),
                Map.of("name", "51-65岁", "value", 0),
                Map.of("name", "65岁以上", "value", 0)
        );
    }

    private List<Map<String, Object>> buildRecentActivities() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<AssessmentRecord> recentAssessments = assessmentMapper.selectList(
                new LambdaQueryWrapper<AssessmentRecord>()
                        .ge(AssessmentRecord::getCreateTime, sevenDaysAgo)
                        .orderByDesc(AssessmentRecord::getCreateTime)
                        .last("LIMIT 5"));

        List<InterventionPlan> recentPlans = interventionMapper.selectList(
                new LambdaQueryWrapper<InterventionPlan>()
                        .ge(InterventionPlan::getCreateTime, sevenDaysAgo)
                        .orderByDesc(InterventionPlan::getCreateTime)
                        .last("LIMIT 5"));

        List<KnowledgeArticle> recentArticles = knowledgeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                        .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                        .ge(KnowledgeArticle::getCreateTime, sevenDaysAgo)
                        .orderByDesc(KnowledgeArticle::getCreateTime)
                        .last("LIMIT 5"));

        List<Map<String, Object>> activities = new ArrayList<>();
        for (AssessmentRecord r : recentAssessments) {
            activities.add(Map.of(
                    "type", "评估",
                    "desc", "会员 #" + r.getMemberId() + " 完成健康评估，得分 " + r.getTotalScore(),
                    "time", r.getCreateTime().toString().replace("T", " ").substring(0, 16)
            ));
        }
        for (InterventionPlan p : recentPlans) {
            activities.add(Map.of(
                    "type", "干预",
                    "desc", "为会员 #" + p.getMemberId() + " 创建干预方案：" + p.getPlanName(),
                    "time", p.getCreateTime().toString().replace("T", " ").substring(0, 16)
            ));
        }
        for (KnowledgeArticle a : recentArticles) {
            activities.add(Map.of(
                    "type", "知识",
                    "desc", "发布科普文章：" + a.getTitle(),
                    "time", a.getCreateTime().toString().replace("T", " ").substring(0, 16)
            ));
        }
        activities.sort((a, b) -> ((String) b.get("time")).compareTo((String) a.get("time")));
        List<Map<String, Object>> result = activities.stream().limit(10)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        if (result.isEmpty()) {
            result.add(Map.of(
                    "type", "系统",
                    "desc", "系统已成功初始化，等待首次数据录入。请添加会员并完成体检评估。",
                    "time", LocalDate.now().toString()
            ));
        }
        return result;
    }
}