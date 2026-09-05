package com.health.web.assistant.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.domain.AgentPreOrder;
import com.health.system.domain.AssessmentRecord;
import com.health.system.domain.DiseaseLibrary;
import com.health.system.domain.Member;
import com.health.system.mapper.AssessmentRecordMapper;
import com.health.web.assistant.service.PreOrderService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 统一工具集（每请求一个实例）。
 * 复用现有 mapper/服务，包一层 @Tool + 事件发布：模型可调，前端可看（透明推理）。
 * 各 Agent 通过 AgentDefinition.allowedTools 白名单只暴露本角色需要的工具。
 */
public class AgentToolkit {

    private final Long userId;
    private final ToolEventPublisher events;
    private final TriageTools triageTools;
    private final AssessmentRecordMapper assessmentRecordMapper;
    private final PreOrderService preOrderService;

    public AgentToolkit(Long userId,
                        ToolEventPublisher events,
                        TriageTools triageTools,
                        AssessmentRecordMapper assessmentRecordMapper,
                        PreOrderService preOrderService) {
        this.userId = userId;
        this.events = events;
        this.triageTools = triageTools;
        this.assessmentRecordMapper = assessmentRecordMapper;
        this.preOrderService = preOrderService;
    }

    @Tool(name = "searchDiseases", description = "按症状关键词搜索疾病库（病名/症状/病因），返回候选疾病供分诊参考")
    public List<DiseaseLibrary> searchDiseases(
            @ToolParam(description = "症状关键词，如 头痛、胸痛、腹痛") String keyword) {
        events.call("searchDiseases", Map.of("keyword", keyword));
        List<DiseaseLibrary> result = triageTools.searchDiseases(keyword);
        String summary = result.isEmpty() ? "无匹配疾病"
                : "命中 " + result.size() + " 条: " + result.stream()
                        .map(DiseaseLibrary::getDiseaseName)
                        .collect(Collectors.joining("、"));
        events.result("searchDiseases", summary);
        return result;
    }

    @Tool(name = "searchMembers", description = "按姓名或手机号模糊搜索会员，用于把用户身份对到会员档案")
    public List<Member> searchMembers(
            @ToolParam(description = "姓名或手机号关键词") String keyword) {
        events.call("searchMembers", Map.of("keyword", keyword));
        List<Member> result = triageTools.searchMembers(keyword);
        String summary = result.isEmpty() ? "无匹配会员"
                : "命中 " + result.size() + " 条: " + result.stream()
                        .map(m -> "#" + m.getId() + " " + m.getName() + (m.getPhone() == null ? "" : " " + m.getPhone()))
                        .collect(Collectors.joining("; "));
        events.result("searchMembers", summary);
        return result;
    }

    @Tool(name = "getMemberAssessments", description = "查询会员历史健康评估/体检记录（含风险等级、结论、建议，按日期倒序），用于报告解读")
    public List<AssessmentRecord> getMemberAssessments(
            @ToolParam(description = "会员ID") Long memberId) {
        events.call("getMemberAssessments", Map.of("memberId", memberId));
        List<AssessmentRecord> result = assessmentRecordMapper.selectList(
                new LambdaQueryWrapper<AssessmentRecord>()
                        .eq(AssessmentRecord::getMemberId, memberId)
                        .orderByDesc(AssessmentRecord::getAssessDate));
        String summary = result.isEmpty() ? "该会员暂无评估记录"
                : "共 " + result.size() + " 条（最近评估日期: " + result.get(0).getAssessDate() + "）";
        events.result("getMemberAssessments", summary);
        return result;
    }

    @Tool(name = "createPreOrder", description = "为会员创建「科室+日期」预占单（PENDING，两步确认第一步）；同一会员+科室+日期重复调用返回同一单（幂等）")
    public AgentPreOrder createPreOrder(
            @ToolParam(description = "拟就诊科室，如 心内科、呼吸内科、神经内科") String department,
            @ToolParam(description = "期望就诊日期 yyyy-MM-dd；省略则默认次日", required = false) String appointmentDate,
            @ToolParam(description = "主诉/症状摘要（供人工核对），如 持续头痛3天伴恶心") String symptomSummary,
            @ToolParam(description = "会员ID，须先用 searchMembers 匹配得到；可选", required = false) Long memberId) {
        events.call("createPreOrder", Map.of(
                "department", department,
                "date", appointmentDate == null || appointmentDate.isBlank() ? "(默认次日)" : appointmentDate,
                "memberId", memberId == null ? "(未指定)" : String.valueOf(memberId)));
        AgentPreOrder po = preOrderService.create(userId, memberId, department, appointmentDate, symptomSummary);
        String summary = "已生成预订单 #" + po.getId() + " [" + po.getStatus() + "] "
                + po.getDepartment() + " " + po.getAppointmentDate();
        events.result("createPreOrder", summary);
        return po;
    }
}
