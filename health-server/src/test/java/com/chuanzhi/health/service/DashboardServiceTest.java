package com.chuanzhi.health.service;

import com.chuanzhi.health.dto.DashboardStats;
import com.chuanzhi.health.entity.InterventionPlan;
import com.chuanzhi.health.entity.KnowledgeArticle;
import com.chuanzhi.health.entity.User;
import com.chuanzhi.health.mapper.InterventionPlanMapper;
import com.chuanzhi.health.mapper.KnowledgeArticleMapper;
import com.chuanzhi.health.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private InterventionPlanMapper planMapper;

    @Autowired
    private KnowledgeArticleMapper articleMapper;

    @BeforeEach
    void seedData() {
        String suffix = String.valueOf(System.nanoTime()).substring(9);

        User u1 = new User();
        u1.setUsername("dashuser1_" + suffix);
        u1.setPassword("pass");
        u1.setAge(50);
        u1.setName("测试用户1");
        u1.setRole("USER");
        userMapper.insert(u1);

        User u2 = new User();
        u2.setUsername("dashuser2_" + suffix);
        u2.setPassword("pass");
        u2.setAge(70);
        u2.setName("测试用户2");
        u2.setRole("USER");
        userMapper.insert(u2);

        InterventionPlan plan = new InterventionPlan();
        plan.setUserId(u1.getId());
        plan.setTitle("测试干预计划");
        plan.setGoal("降低血压");
        plan.setStatus(com.chuanzhi.health.enums.PlanStatus.ACTIVE);
        planMapper.insert(plan);

        KnowledgeArticle article = new KnowledgeArticle();
        article.setCategoryId(1L);
        article.setTitle("测试文章");
        article.setContent("测试内容");
        article.setAuthor("管理员");
        articleMapper.insert(article);
    }

    @Test
    void shouldReturnDashboardStats() {
        DashboardStats stats = dashboardService.getStats();

        assertNotNull(stats);
        assertTrue(stats.getTotalUsers() >= 2);
        assertTrue(stats.getActiveInterventions() >= 1);
        assertTrue(stats.getKnowledgeArticles() >= 1);
        assertNotNull(stats.getAgeDistribution());
        assertFalse(stats.getAgeDistribution().isEmpty());
        assertNotNull(stats.getAssessmentTrend());
        assertEquals(7, stats.getAssessmentTrend().size());
        assertNotNull(stats.getRecentActivities());
    }

    @Test
    void shouldReturnCorrectAgeDistribution() {
        DashboardStats stats = dashboardService.getStats();

        var ageDist = stats.getAgeDistribution();
        boolean hasMiddleAge = ageDist.stream().anyMatch(m -> "45-60岁".equals(m.get("name")));
        boolean hasElderly = ageDist.stream().anyMatch(m -> "60-75岁".equals(m.get("name")));
        assertTrue(hasMiddleAge || hasElderly);
    }
}
