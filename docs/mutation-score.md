# 变异测试（mutation score）· P2 / Round 6

> 目的：回答**"把代码改坏，测试抓不抓得住"**——覆盖率只能说明"执行过"，变异分才说明"断言够不够强"。
> 口径：本文件只覆盖 4 个安全/治理关键类；不追求全仓分值，追求**安全路径的断言强度可信**。

## 怎么跑

```bash
# 首次需要联网拉 pitest 插件（已固化在 health-admin/pom.xml，依赖 junit5 插件 1.2.1）
mvn -o -pl health-admin org.pitest:pitest-maven:mutationCoverage
# 报告：health-admin/target/pit-reports/（HTML + XML + CSV）
```

pom 里已固定的靶子：

| 目标类 | 为什么是它 |
|---|---|
| `advisor.InputGuardrailAdvice` | 处方/剂量护栏（红队 75 例打的就是它） |
| `tool.ToolAccessPolicy` | 角色→能力→工具的唯一权威 |
| `tool.ToolAccessGuard` | 真实越权拦截入口（P0-1 的落点） |
| `agent.SymptomStateMachine` | 分诊状态机（急症不可降级） |

## 结果（2026-11-04 · JDK 17 · 本机离线）

```
>> Generated 24 mutations  Killed 11 (46%)
   SURVIVED 0
```

| 指标 | 数值 | 含义 |
|---|---|---|
| 生成变异体 | 24 | pitest 默认 mutator 组合 |
| 被杀死 | 11 | 46% |
| **存活** | **0** | **被试到的分支，断言强度是够的（test strength 100%）** |
| 未覆盖 | 其余 | 落在没有任何测试执行到的分支上 |

### 关键解读（这条比分数本身重要）

46% 的分值**不是断言太弱**，而是**有一批变异点根本没被任何测试执行**（`NO_COVERAGE`）。
两者要分开治：

- **存活 0 ⇒ 断言质量没问题**：凡是被测到的分支，改坏就会被抓到——这与红队 75 例 + 契约快照的投入一致；
- **未覆盖 ⇒ 覆盖缺口**：需要补测试把分支打到，而不是加更多断言。

## 已知缺口与下一步（明确、可执行）

1. `InputGuardrailAdvice.before()`：**空/非法 sessionId、超长消息（MAX_MESSAGE_LENGTH）、空消息**三个分支没有测试打到 → 补 3 条边界用例即可覆盖。
2. `SymptomStateMachine` 深层分支（多轮澄清后的状态迁移）目前只测了主干 → 补"澄清→再澄清→转急诊"的序列用例。
3. 目标：把 killed 从 11/24 提到 **≥60%**（即 15/24），且**保持 survived=0**。

## 口径与限制（避免误读）

- 只跑 4 个安全关键类，**不代表全仓质量**；
- 变异分与"业务 bug 数量"无直接换算关系，仅用于**同仓纵向比较**（这轮 46%，下一轮补完覆盖后应上升）；
- 未接入 CI：pitest 单轮约 1 分钟，且会显著拉长流水线；作为**改动安全代码后的手动门禁**使用；
- 已知坑：pitest 1.15.8 + JUnit5 必须把 `pitest-junit5-plugin` 挂到 **plugin 的 `<dependencies>`** 里，否则会报 "Pitest could not run any tests"（本 pom 已配好，勿删）。

## 与其它轮次的关系

| 轮次 | 手段 | 回答的问题 |
|---|---|---|
| Round 1–2 | 契约快照 | 协议有没有漂移 |
| Round 3 | 性能回归门禁 | 性能有没有退化 |
| Round 4 | 集成层（真 MySQL/Redis） | 真实依赖下对不对 |
| Round 5 | 红队 75 例 | 攻击面拦不拦得住 |
| **Round 6** | **变异测试** | **断言够不够强、覆盖漏在哪** |