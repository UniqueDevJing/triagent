package com.chuanzhi.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chuanzhi.health.dto.DashboardStats;
import com.chuanzhi.health.entity.*;
import com.chuanzhi.health.enums.PlanStatus;
import com.chuanzhi.health.mapper.*;
import com.chuanzhi.health.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final AssessmentRecordMapper assessmentRecordMapper;
    private final InterventionPlanMapper interventionPlanMapper;
    private final KnowledgeArticleMapper knowledgeArticleMapper;

    @Override
    @Cacheable(value = "dashboard:stats", unless = "#result == null")
    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();

        stats.setTotalUsers(userMapper.selectCount(null));
        stats.setTodayAssessments(
            assessmentRecordMapper.selectCount(
                new LambdaQueryWrapper<AssessmentRecord>()
                    .ge(AssessmentRecord::getCreatedAt, LocalDate.now().atStartOfDay())
            )
        );
        stats.setActiveInterventions(
            interventionPlanMapper.selectCount(
                new LambdaQueryWrapper<InterventionPlan>()
                    .eq(InterventionPlan::getStatus, PlanStatus.ACTIVE)
            )
        );
        stats.setKnowledgeArticles(knowledgeArticleMapper.selectCount(null));

        // 年龄分布
        List<User> users = userMapper.selectList(null);
        Map<String, Long> ageMap = new HashMap<>();
        for (User u : users) {
            String group;
            if (u.getAge() < 45) group = "45岁以下";
            else if (u.getAge() < 60) group = "45-60岁";
            else if (u.getAge() < 75) group = "60-75岁";
            else group = "75岁以上";
            ageMap.merge(group, 1L, Long::sum);
        }
        List<Map<String, Object>> ageDist = new ArrayList<>();
        ageMap.forEach((k, v) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", k);
            m.put("value", v);
            ageDist.add(m);
        });
        stats.setAgeDistribution(ageDist);

        // 评估趋势(近7天)
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = assessmentRecordMapper.selectCount(
                new LambdaQueryWrapper<AssessmentRecord>()
                    .ge(AssessmentRecord::getCreatedAt, date.atStartOfDay())
                    .lt(AssessmentRecord::getCreatedAt, date.plusDays(1).atStartOfDay())
            );
            Map<String, Object> m = new HashMap<>();
            m.put("date", date.toString());
            m.put("count", count);
            trend.add(m);
        }
        stats.setAssessmentTrend(trend);

        // 最近动态
        List<Map<String, Object>> activities = new ArrayList<>();
        List<AssessmentRecord> recentAssess = assessmentRecordMapper.selectList(
            new LambdaQueryWrapper<AssessmentRecord>()
                .orderByDesc(AssessmentRecord::getCreatedAt).last("LIMIT 5")
        );
        for (AssessmentRecord a : recentAssess) {
            Map<String, Object> m = new HashMap<>();
            m.put("type", "评估");
            m.put("desc", "用户" + a.getUserId() + " 完成健康评估，风险等级: " + a.getRiskLevel());
            m.put("time", a.getCreatedAt().toString());
            activities.add(m);
        }
        stats.setRecentActivities(activities);

        return stats;
    }
}
