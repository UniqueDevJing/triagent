# TriAgent 最小独立 demo（T4）

只依赖 `agent-core`（状态机 / 权限 / 预算 / 路由 / 会话事实库），**不需要**完整健康平台、数据库、Redis 或 LLM API Key——五条演示全部是确定性决策（0 次 LLM 调用）。

## 十分钟本地启动

```bash
# 一条命令跑全部 5 条演示（推荐）
mvn -pl triagent-demo -am test -Dtest=TriAgentDemoRunTest

# 或者直接运行 main
mvn -pl triagent-demo -am compile
java -Dfile.encoding=UTF-8 -cp "triagent-demo/target/classes;agent-core/target/classes;$HOME/.m2/repository/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar" com.health.demo.TriAgentDemo
```

Windows 控制台中文乱码时加 `-Dfile.encoding=UTF-8`。

## 五条演示场景

| # | 场景 | 对应验收场景 | 决策链要点 |
|---|---|---|---|
| 1 | 红旗短路 | 当前胸痛并请求预约 → 先走急症分诊 | 红旗词表命中 → EMERGENCY，0 次 LLM，禁止创建预订单 |
| 2 | 多轮澄清 | 部位→时长→伴随症状 | 字段缺失逐项追问，三字段齐备才进模型 |
| 3 | 权限拦截 | SCHEDULER 调未授权工具 | ToolAccessPolicy 拒绝 + ToolAuditLog 留痕（拒绝率可算） |
| 4 | 预算熔断 | 会话 token 超限 | 0 LLM 确定性降级 DEGRADED，降级原因可解释 |
| 5 | 协作拦截 | 会话有 EMERGENCY 事实再预约 | gateScheduler 返回事实 → SAFETY_BLOCK；澄清「看过急诊」且当轮无红旗才放行 |

## 与完整平台的关系

本模块只演示 agent-core 的确定性决策链。完整编排（LLM 流式、RAG、Advisor 管道、SSE）在 `health-admin` 的 `AssistantOrchestrator`——demo 里的每个决策组件与线上是同一份代码（ConversationRouter / SymptomStateMachine / ToolAccessGuard / BudgetGuard / SupervisorSafetyPlanner）。
