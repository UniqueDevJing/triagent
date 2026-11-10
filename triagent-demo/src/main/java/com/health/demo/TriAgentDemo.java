package com.health.demo;

import com.health.web.assistant.agent.AgentType;
import com.health.web.assistant.agent.ConversationRouter;
import com.health.web.assistant.agent.SupervisorSafetyPlanner;
import com.health.web.assistant.agent.SymptomStateMachine;
import com.health.web.assistant.memory.InMemorySessionFactStore;
import com.health.web.assistant.tool.BudgetGuard;
import com.health.web.assistant.tool.ToolAccessGuard;
import com.health.web.assistant.tool.ToolAccessPolicy;
import com.health.web.assistant.tool.ToolAuditLog;

import java.lang.reflect.Field;
import java.util.List;

/**
 * T4：TriAgent 最小独立 demo（只依赖 agent-core，无需完整健康平台 / 数据库 / LLM Key）。
 *
 * <p>五条演示，全部为确定性决策（0 次 LLM 调用），输出可审计的决策链：
 * <ol>
 *   <li>红旗短路：当前胸痛 → 确定性 EMERGENCY（医疗安全优先）；</li>
 *   <li>多轮澄清：部位 → 时长 → 伴随症状，字段缺失不进入模型；</li>
 *   <li>权限拦截：SCHEDULER 越权调用 searchDiseases → 拒绝 + 审计留痕；</li>
 *   <li>预算熔断：会话 token 超限 → 0 LLM 确定性降级；</li>
 *   <li>协作拦截：本会话已有 EMERGENCY 事实 → 预约落单前被拦截。</li>
 * </ol>
 *
 * <p>运行方式（任选其一，十分钟内可完成本地启动）：
 * <pre>
 *   mvn -pl triagent-demo -am test -Dtest=TriAgentDemoRunTest    # 一条命令跑全部演示
 *   mvn -pl triagent-demo -am exec:java -Dexec.mainClass=com.health.demo.TriAgentDemo
 * </pre>
 * Windows 控制台若中文乱码，追加 -Dfile.encoding=UTF-8。
 */
public class TriAgentDemo {

    private static final String LINE = "--------------------------------------------------------------";

    public static void main(String[] args) {
        System.out.println("=== TriAgent 最小独立 demo（agent-core 决策链，0 LLM 调用）===");
        demo1RedFlagShortCircuit();
        demo2MultiTurnClarify();
        demo3PermissionDenied();
        demo4BudgetCircuitBreak();
        demo5EmergencyFactBlocksScheduler();
        System.out.println("=== 全部演示结束 ===");
    }

    /** 演示 1：当前胸痛并请求预约 → 必须先走急症分诊（0 次 LLM 调用） */
    static void demo1RedFlagShortCircuit() {
        System.out.println(LINE);
        System.out.println("[演示1] 红旗短路：用户说「突然胸痛伴呼吸困难，帮我预约心内科」");
        SymptomStateMachine sm = new SymptomStateMachine();
        SymptomStateMachine.Decision d = sm.decide("demo1", "突然胸痛伴呼吸困难，帮我预约心内科");
        System.out.println("  决策链: 红旗词表命中 " + d.redFlagWords()
                + " -> " + d.type() + "（0 次 LLM 调用，禁止创建预订单）");
        System.out.println("  建议科室: " + sm.buildEmergencyTriage(d.redFlagWords()).getDepartments());
    }

    /** 演示 2：多轮澄清——部位/时长/伴随症状字段缺失时逐项追问，不进模型 */
    static void demo2MultiTurnClarify() {
        System.out.println(LINE);
        System.out.println("[演示2] 多轮澄清：头痛 -> 3天 -> 没有");
        SymptomStateMachine sm = new SymptomStateMachine();
        String[] turns = {"我头痛", "3天了", "没有"};
        StringBuilder chain = new StringBuilder();
        for (String turn : turns) {
            SymptomStateMachine.Decision d = sm.decide("demo2", turn);
            chain.append(turn).append(" -> ").append(d.type())
                    .append(d.missingFields().isEmpty() ? "" : "(缺:" + d.missingFields() + ")")
                    .append("  ");
        }
        System.out.println("  决策链: " + chain);
        System.out.println("  三字段齐备后才把主诉交给模型做常规分诊");
    }

    /** 演示 3：SCHEDULER 尝试调用未授权医疗工具 → 拒绝并留下审计记录 */
    static void demo3PermissionDenied() {
        System.out.println(LINE);
        System.out.println("[演示3] 权限拦截：SCHEDULER 越权调用 searchDiseases");
        ToolAccessPolicy policy = new ToolAccessPolicy();
        ToolAuditLog audit = new ToolAuditLog();
        ToolAccessGuard guard = new ToolAccessGuard(policy, audit);
        try {
            guard.check(AgentType.SCHEDULER, "searchDiseases", "demo3", 1001L);
            System.out.println("  决策链: （异常）调用竟然放行 —— 不应出现");
        } catch (Exception e) {
            System.out.println("  决策链: ToolAccessPolicy 无该能力 -> 拒绝 -> " + e.getClass().getSimpleName());
        }
        System.out.println("  审计留痕: " + audit.recent());
        System.out.println("  审计拒绝率: " + audit.denialRate());
    }

    /** 演示 4：会话 token 预算耗尽 → 跳过 LLM 的确定性降级 */
    static void demo4BudgetCircuitBreak() {
        System.out.println(LINE);
        System.out.println("[演示4] 预算熔断：会话 token 超上限 -> DEGRADED（0 token 消耗）");
        BudgetGuard guard = new BudgetGuard();
        setField(guard, "enabled", true);
        setField(guard, "maxToolCalls", 20);
        setField(guard, "maxTokens", 100);
        String noisy = "x".repeat(400);
        guard.recordTokens("demo4", noisy);
        guard.recordTokens("demo4", noisy);
        boolean exceeded = guard.tokenExceeded("demo4");
        System.out.println("  决策链: recordTokens x2 (各约 200 token, 上限 100) -> tokenExceeded=" + exceeded);
        System.out.println("  降级原因: " + guard.degradeReason("demo4"));
        System.out.println("  熔断会话数: " + guard.circuitBreakCount());
    }

    /** 演示 5：本会话已有 EMERGENCY 分诊事实 → 预约落单前被协作拦截 */
    static void demo5EmergencyFactBlocksScheduler() {
        System.out.println(LINE);
        System.out.println("[演示5] 协作拦截：先分诊出 EMERGENCY，再请求预约");
        InMemorySessionFactStore factStore = new InMemorySessionFactStore();
        SymptomStateMachine sm = new SymptomStateMachine();
        ConversationRouter router = new ConversationRouter();
        SupervisorSafetyPlanner planner = new SupervisorSafetyPlanner(sm, router, factStore);

        // 第一步：分诊出急症并落库会话事实（AgentRegistry 在 health-admin，demo 内联同口径关键词路由）
        var d = sm.decide("demo5", "胸闷,感觉喘不上气");
        factStore.recordEmergency("demo5", "EMERGENCY",
                sm.buildEmergencyTriage(d.redFlagWords()).getDepartments(), "演示免责声明");
        System.out.println("  第一步: TRIAGE（关键词路由同口径）-> " + d.type() + " -> 会话事实已落库");

        // 第二步：用户接着请求预约 -> 被拦截
        String msg = "帮我预约明天上午的门诊";
        boolean hitScheduler = msg.contains("预约") || msg.contains("挂号");
        var ef = planner.gateScheduler("demo5", msg);
        System.out.println("  第二步: 字面路由 " + (hitScheduler ? "SCHEDULER" : "TRIAGE")
                + " -> gateScheduler 返回 " + (ef == null ? "放行" : "EMERGENCY 事实") + " -> SAFETY_BLOCK");
        System.out.println("  放行条件: 用户澄清「看过急诊/已排除」且当轮无红旗词");
    }

    /** 反射设置 @Value 字段（demo 无 Spring 容器） */
    static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("demo 反射设置失败: " + name, e);
        }
    }

    private TriAgentDemo() {
    }
}
