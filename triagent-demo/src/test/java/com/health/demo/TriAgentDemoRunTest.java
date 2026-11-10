package com.health.demo;

import org.junit.jupiter.api.Test;

/**
 * T4 验收入口：一条命令跑通 5 条演示。
 * mvn -pl triagent-demo -am test -Dtest=TriAgentDemoRunTest
 * 断言全部为确定性结论（医疗安全类不通过 = 直接失败）。
 */
class TriAgentDemoRunTest {

    @Test
    void runAllFiveDemos() {
        TriAgentDemo.main(new String[0]);
    }

    @Test
    void demo1EmergencyIsDeterministic() {
        com.health.web.assistant.agent.SymptomStateMachine sm =
                new com.health.web.assistant.agent.SymptomStateMachine();
        var d = sm.decide("t1", "突然胸痛伴呼吸困难");
        org.junit.jupiter.api.Assertions.assertEquals(
                com.health.web.assistant.agent.SymptomStateMachine.DecisionType.EMERGENCY, d.type());
    }

    @Test
    void demo3SchedulerCannotCallDiseaseSearch() {
        com.health.web.assistant.tool.ToolAccessGuard guard =
                new com.health.web.assistant.tool.ToolAccessGuard(
                        new com.health.web.assistant.tool.ToolAccessPolicy(),
                        new com.health.web.assistant.tool.ToolAuditLog());
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                guard.check(com.health.web.assistant.agent.AgentType.SCHEDULER,
                        "searchDiseases", "t3", 1L));
    }

    @Test
    void demo5EmergencyFactBlocksScheduler() {
        com.health.web.assistant.memory.InMemorySessionFactStore store =
                new com.health.web.assistant.memory.InMemorySessionFactStore();
        store.recordEmergency("t5", "EMERGENCY", java.util.List.of("急诊科"), "d");
        var planner = new com.health.web.assistant.agent.SupervisorSafetyPlanner(
                new com.health.web.assistant.agent.SymptomStateMachine(),
                new com.health.web.assistant.agent.ConversationRouter(),
                store);
        org.junit.jupiter.api.Assertions.assertNotNull(
                planner.gateScheduler("t5", "帮我预约明天上午的门诊"));
    }
}
