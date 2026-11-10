# TriAgent 离线评测基线（T2/T3）

150 个确定性案例，不依赖 LLM API Key，全部可在本地/CI 离线执行。

## 一条命令出报告

```bash
mvn test -pl health-admin -Dtest=TriageEvalHarnessTest
```

报告落盘 `evals/reports/latest.md` 与 `latest-report.json`（含时间戳快照）。

## 数据集构成（150 案例）

| 文件 | 维度 | 案例 | 评测对象 |
|---|---|---|---|
| routing.json | 意图路由 | 45 | AgentRegistry.resolve（预约/报告/分诊关键词 + 冲突优先级） |
| continuity.json | 会话连续性 | 20 | AssistantOrchestrator.decideAgent（连任/切换/求助词抢占） |
| state-machine.json | 分诊状态机轨迹 | 30 | SymptomStateMachine.decide 多轮决策序列与缺失字段 |
| emergency.json | 红旗与安全 | 30 | 17 类红旗词命中 + 急诊澄清放行（含红旗词+override 组合的防绕过） |
| permission.json | 工具权限矩阵 | 15 | ToolAccessPolicy 3 角色 x 5 工具全组合 |
| budget.json | 预算熔断 | 10 | BudgetGuard 工具次数/token 上限、降级原因 |

## 重构门禁

任何编排层/治理组件重构后重跑本 harness：

- 通过率降幅 <= 2pp 视为行为保持（方案 v0.9 门禁）
- 任一安全类案例（emergency/permission）失败 = 直接不通过，不允许按平均值掩盖
- 首轮基线：150/150（见 reports/latest-report.json）
