# Changelog

## v1.0-rc（2026-11-10）

工程化收敛版本：编排器拆分、离线评测基线、工具治理统一入口、标准化 Trace、演示环境安全化、前端依赖全量升级。全量测试 28 测试类 270 用例 0 失败（含 150 例离线评测）。

### Added（P0/P1 工程化）
- **离线评测基线**：150 个版本化案例（路由 45/连续性 20/状态机 30/红旗安全 30/权限 15/预算 10），0 LLM 调用一条命令复现（`mvn test -pl health-admin -Dtest=TriageEvalHarnessTest`），报告落盘 `evals/reports/latest.md`；基线 150/150 通过，重构门禁通过率降幅 ≤2pp
- **ToolGateway 统一工具入口**（T5）：权限 → 熔断 → 幂等 → 超时 → 审计唯一入口，AgentToolkit 全部工具经 invoke 执行，无绕过路径
- **标准化 Trace**（T6）：OTel 风格 span 树（traceId/spanId/parent/attributes）+ SensitiveRedactor 脱敏（手机/身份证/邮箱/JWT/密钥字段），工具调用挂子 span，`GET /api/v1/assistant/trace` 只读查询
- **演示环境安全化**（T8）：DemoAccessGuard 只读拦截（写操作白名单仅 chat/logout，越权 403）+ DemoQuotaService 每日聊天额度（自然日滚动 20 次，超限 429，被拒不虚增计数）+ visitor 账号初始化与数据重置脚本（`sql/demo_account.sql`、`scripts/reset_demo_data.sql`）；`triagent.demo.enabled` 按需装配，默认关闭
- **最小独立 demo**（T4）：`triagent-demo` 模块只依赖 agent-core，一条命令跑通 5 条确定性演示（红旗短路/多轮澄清/权限拦截/预算熔断/协作拦截）
- 架构说明文档（docs/agent/架构说明.md）与评测口径文档（evals/README.md）

### Changed（架构重构）
- **AssistantOrchestrator 拆分**：569 行 → 薄编排器 + 6 策略组件（AgentRegistry 路由 / TriageDeterministicFlow 确定性分诊流 / LlmTurnAssembler LLM 轮组装 / TriageJsonParser / TurnFinalizer），策略组件可独立测试与替换；重构经 150 例 harness 门禁验证行为保持
- **前端依赖全量升级**（T7）：npm audit 8 个漏洞全量清零（vue 3.5 / echarts 6 / vite 5→8 rolldown / vitest 5，glob/minimatch overrides）；manualChunks 函数化、oxc.drop、构建 1.8s，8 项前端测试全过（构建报告 docs/t7-frontend-build-report.md）
- **依赖漏洞扫描进 CI**：dependency-scan job（OSS Index audit，CVSS≥7 门禁 + 匿名限流重试 3 次，聚合全部 Maven 模块），本地实测 0 高危通过；CI frontend job Node 18→20（vite 8 要求）并补 vitest 8 项

## v0.9（2026-11-10）

基线冻结版本（P0 之前）：Supervisor 规划、多 Agent 路由、红旗状态机、RAG 溯源、预约两步确认 + 幂等、预算熔断与确定性降级、红队四维度对抗评测（32 断言）进 CI、30 例 golden set + LLM-as-Judge 双轨评估（紧急度 0.88 / 科室 28/29 / 规则 9.24 / 判轻 0 例）。
