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
        // 参数校验：模型输出的 keyword 可能为空串——空关键词会拼成 LIKE '%%' 全表扫
        if (keyword == null || keyword.isBlank()) {
            events.result("searchDiseases", "未提供症状关键词，跳过检索");
            return List.of();
        }
        String kw = keyword.trim();
        if (kw.length() > 50) {
            kw = kw.substring(0, 50);   // 防超长串拖垮 LIKE
        }
        events.call("searchDiseases", Map.of("keyword", kw));
        List<DiseaseLibrary> result = triageTools.searchDiseases(kw);
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
        if (keyword == null || keyword.isBlank()) {
            events.result("searchMembers", "未提供姓名或手机号，跳过检索");
            return List.of();
        }
        String kw = keyword.trim();
        if (kw.length() > 30) {
            kw = kw.substring(0, 30);
        }
        events.call("searchMembers", Map.of("keyword", kw));
        List<Member> result = triageTools.searchMembers(kw);
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
        // memberId 必须为正数：null/非正数直接给工具层结果说明，而不是把 NPE 抛成 500
        if (memberId == null || memberId <= 0) {
            events.result("getMemberAssessments", "会员ID无效（需先用 searchMembers 匹配得到），跳过查询");
            return List.of();
        }
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
        // 日期格式前置校验：模型可能输出 "明天"/"10月3号" 等，直接 parse 会抛 500
        if (appointmentDate != null && !appointmentDate.isBlank()
                && !appointmentDate.trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            String msg = "就诊日期格式应为 yyyy-MM-dd（收到: " + appointmentDate.trim() + "），请让用户确认具体日期";
            events.result("createPreOrder", msg);
            throw new IllegalArgumentException(msg);
        }
        AgentPreOrder po = preOrderService.create(userId, memberId, department, appointmentDate, symptomSummary);
        String summary = "已生成预订单 #" + po.getId() + " [" + po.getStatus() + "] "
                + po.getDepartment() + " " + po.getAppointmentDate();
        // 结构化下发：前端据此渲染「待确认预约」卡片并调两步确认的第二步（此前只有纯文本，
        // 前端解析不出订单号，confirmPreOrder 接口一直是死代码）
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("summary", summary);
        payload.put("orderId", po.getId());
        payload.put("status", String.valueOf(po.getStatus()));
        payload.put("department", po.getDepartment());
        payload.put("date", String.valueOf(po.getAppointmentDate()));
        events.result("createPreOrder", payload);
        return po;
    }
}
