package com.chuanzhi.health.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStats {
    private long totalUsers;
    private long todayAssessments;
    private long activeInterventions;
    private long knowledgeArticles;
    private List<Map<String, Object>> ageDistribution;
    private List<Map<String, Object>> assessmentTrend;
    private List<Map<String, Object>> recentActivities;
}
