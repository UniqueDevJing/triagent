# agent-core — 可复用 Agent 内核

> triagent 的「AI 内核」与 RuoYi 业务脚手架的边界在此划清。
> 本模块**不依赖** RuoYi / MyBatis / Redis / Sa-Token / Reactor / Spring-AI，
> 只依赖 `spring-context`（容器注解）+ `slf4j-api`。

## 它装的是什么（按优化清单 T1 的四块）

| 块 | 类 | 说明 |
|---|---|---|
| **状态机** | `agent/SymptomStateMachine` | 确定性分诊状态机：红旗征象短路 EMERGENCY、矛盾症状强制澄清、否定回答识别、急诊 override 防降级。**纯规则，不调 LLM** |
| **工具权限** | `tool/ToolAccessPolicy`、`tool/ToolCapability`、`tool/ToolAccessGuard`、`tool/ToolAuditLog` | 声明式「角色→能力→工具」；方法入口真实越权拦截（抛 `AgentBlockedException`）+ 审计落盘 + 拒绝率指标（协同评审 P0-1/2） |
| **事实总线** | `memory/FactBus`、`memory/InMemoryFactBus`、`memory/SessionFactStore`、`memory/InMemorySessionFactStore` | 通用发布-订阅事实总线；`InMemorySessionFactStore` 内部复用总线承载急诊事实（会话级 KV） |
| **事件流 / DTO** | `model/SseEvent`、`model/SseEventType`、`model/TriageResult`、`model/AgentResponse`、`model/AgentRequest`、`model/ChatRequest` | 编排内部与前端之间的事件/请求/响应数据结构（纯 POJO） |
| 支撑类型 | `agent/AgentType`、`agent/AgentDefinition`、`advisor/AgentBlockedException`、`rag/KnowledgeDoc`、`rag/KnowledgeRetriever` | 领域枚举、异常、检索接口 |

> 注：`SseEvent` 是纯数据结构，**不含** `SseEmitter`；真正的 HTTP 流式序列化在 `health-admin` 的 `AssistantController`。

## 不在本模块（留在 health-admin 做接入）

- `AssistantOrchestrator`（Spring-AI `ChatClient` + Reactor `Flux` + MyBatis 重耦合）
- `AgentToolkit` / `TriageTools`（`@Tool` 注解 + RuoYi 域对象 + MyBatis）
- `advisor/*`（输入/输出护栏、审计建议，挂在 Spring-AI Advisor 链上）
- `controller/*`、`config/*`、`*Redis*Store`（Redis 适配 / Sa-Token / Web）
- `KnowledgeRetriever` 的 **实现** `KeywordKnowledgeRetriever`（MyBatis），接口在 core

## 主工程如何接入

`health-admin` 通过 Maven 依赖引入本模块，Spring 的 `@ComponentScan`
（`com.health.web.**`）会自动扫描本 jar 中的 `@Component` + `@Value` 类并注册为 Bean，
**无需任何额外装配代码**：

```xml
<dependency>
    <groupId>com.health</groupId>
    <artifactId>agent-core</artifactId>
</dependency>
```

业务侧只需：
1. 用 core 提供的 `ToolAccessPolicy` 配置角色权限；
2. 在 `@Tool` 方法入口调用 `ToolAccessGuard.check(role, tool, sessionId, userId)`（已由 `AgentToolkit` 完成）；
3. 用 `SessionFactStore`（Redis 实现在 health-admin）做跨 Agent 事实协作；
4. 把 `SseEvent` / `TriageResult` 交给 `AssistantController` 做 SSE 序列化。

## 模块指标（T1 验收）

- 主源码：**20 文件 / 1,312 行**；测试：**4 文件 / 253 行**
- 第三方依赖：**2 个**（`spring-context`、`slf4j-api`），无 RuoYi 体系依赖
- 主工程 `health-admin` 引用本模块的文件数：**20**
- 单测：`SymptomStateMachineTest`（状态机）、`ToolAccessPolicyTest`（权限矩阵）、
  `ToolAccessGuardTest`（真实越权拦截）、`FactBusTest`（事实总线），`mvn test` 自动包含

## 构建

```bash
# 仅编译内核（可独立验证）
mvn -q -pl agent-core -am compile

# 跑内核单测
mvn -q -pl agent-core test

# 整仓构建
mvn -q clean package
```
