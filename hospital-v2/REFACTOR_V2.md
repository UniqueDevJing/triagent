# 智慧医院 AI 导诊就诊系统 · v2 重构交付报告

> 对齐 B 站 UP「武哥聊编程」《AI Agent 智慧医院智能问诊就诊系统》的 7 层架构，从零全栈重构。
> 分支：`refactor/v2`（本地验证，未上公网）。后端已全链路跑通 + 单测/集成测全绿；前端 Vite 构建通过并验证联调。

---

## 1. 技术栈（对齐视频）

| 层 | 选型 | 说明 |
|----|------|------|
| 语言 / 构建 | **JDK 21** + Maven 3.9 | `maven.compiler.release=21` |
| 框架 | **Spring Boot 3.5.16** | web + jdbc（NamedParameterJdbcTemplate） |
| AI 编排 | **LangChain4j 1.15** | 注意：1.15 接口已改名 `ChatLanguageModel`→`ChatModel`，`chat(ChatRequest)→ChatResponse` |
| 关系存储 | **H2（MySQL 兼容模式）** 本地零依赖 / **MySQL 8** 生产 | `spring.profiles.active=mysql` 切换 |
| 前端 | **Vue 3.5 + Vite 5 + Element Plus 2.8 + vue-router 4** | VITAL「生命体征」视觉语言 |
| 观测 | 自研 `ai_usage_log` / `agent_run` / `agent_step` / `mcp_tool_call_log` | 全链路可追溯 |

---

## 2. 架构（7 层 → 包结构）

```
用户 → 预问诊(SSE) → ReAct 分诊 Agent ─┬─ 危急硬拦截(red_flag_rule)
                                         ├─ 工具中心(mcp_tool，DB 驱动真开关)
                                         ├─ 医学知识库 RAG(向量余弦 TopK)
                                         └─ 代码二次核验(buildVerified)
              ↓
        挂号(乐观锁+唯一约束) → 电子病历(草稿/签名) → 看板/观测
```

后端包：`com.hospital.v2`
- `common`：`Result` / `BizException` / `JsonUtils` / `GlobalExceptionHandler`（业务异常→`code:400`）
- `config`：`HospitalProps` / `LlmGateway`(含离线 Mock 模型) / `EmbeddingGateway`(本地哈希降级向量)
- `dao`：`Db`（命名参数 + 自增主键回填）
- `service`：`KnowledgeService` / `ToolCenter` / `ReActTriageAgent` / `PreTriageService` / `RegistrationService` / `MedicalRecordService` / `DashboardService` / `UsageService`
- `controller`：`AdminController` / `PatientController`
- `boot`：`StartupVectorFill`（启动补齐 PENDING 向量）
- 数据：`resources/db/schema.sql`（**23 张表**）+ `data.sql`（种子）

前端：`hospital-web-v2`
- 管理端：工具注册表 / 知识库 / 运营看板(Agent 时间线+工具日志) / AI 观测 / 基础数据
- 患者端：智能预问诊(SSE 真流式) / 分诊建议 / 挂号 / 电子病历

---

## 3. 本地启动（零依赖，开箱即跑）

```bash
# 1) 后端（必须用 JDK 21 绝对路径，本机默认是 JDK 17）
export JAVA_HOME="$PWD/.workbuddy/jdk/jdk-21.0.13+9"
export PATH="$JAVA_HOME/bin:/c/Users/Dominion/apache-maven-3.9.16/bin:$PATH"
cd hospital-v2
mvn -B package -DskipTests
"$JAVA_HOME/bin/java" -jar target/hospital-v2.jar --server.port=18092
#   默认 H2 MySQL 兼容模式，schema+种子自动初始化；context-path=/v2

# 2) 前端
cd ../hospital-web-v2
npm install
npm run dev          # 开发：http://127.0.0.1:5192/v2-web/ ，/v2/api 反向代理到 18092
#   或 npm run build  → dist/ （base=/v2-web/），由 nginx 以 /v2-web/ 提供静态、/v2 代理后端

# 3) 生产切真实 MySQL
"$JAVA_HOME/bin/java" -jar target/hospital-v2.jar --spring.profiles.active=mysql
```

> ⚠️ 部署注意：nginx 必须用 `location /v2/`（带尾斜杠）代理后端，否则 `location /v2` 前缀会误伤前端 `/v2-web/`（dev 侧已把 Vite 代理收敛为 `/v2/api` 规避）。
> ⚠️ 配置真实模型：在 `application.yml` 的 `hospital.llm` 设 `mode: real` + `apiKey` + `baseUrl`（OpenAI 兼容，如 DashScope），`EmbeddingGateway`/`LlmGateway` 自动切换真实调用；否则走本地 Mock/哈希降级。

---

## 4. 验证结果（均通过）

### 4.1 接口冒烟（curl 直连 18092）
| 场景 | 结果 |
|------|------|
| 工具中心列表 | 6 个工具全部 `enabled=1` ✅ |
| 工具**真开关** `toggle` | `knowledge_retrieve` 由 1→0，运行时现查现建 ✅ |
| 知识检索 `哮喘` | 本地哈希向量修复后**命中**（阈值 0.08）→ 返回哮喘片段 ✅ |
| ReAct 普通分诊 `咳嗽一周` | → 呼吸内科 / 王慧 / 2026-11-12 AM，置信 0.92 ✅ |
| ReAct **危急硬拦截** `突发胸痛大汗` | → 急诊科(id=5)，`red_flag=1`，置信 0.99 ✅ |
| 挂号（乐观锁扣号） | 返回 `registration_id`，号源 -1 ✅ |
| 重复挂号 | 旧版抛 500 → 已修复为 `code:400 您已预约该时段` ✅ |
| 看板 overview / by-department | 命中率 50%、呼吸内科 100% / 急诊 0% ✅ |
| Agent 时间线 / 工具日志 | 完整 step + SUCCESS 日志 ✅ |
| 知识入库+检索 `肺炎` | 新增文档切片向量化，检索命中 ✅ |
| SSE 预问诊 `chat` | 真流式分块推送，结尾 `[DONE]` ✅ |
| 病历签名 | DRAFT→SIGNED ✅ |

### 4.2 自动化测试（10/10 通过）
- `EmbeddingGatewayTest`（7）：余弦正交/相反/零向量、向量 JSON 往返、空串容错。
- `HospitalV2FlowTest`（3，@SpringBootTest on H2）：普通分诊出有效科室；危急硬转急诊(id=5)；重复挂号抛 `BizException`。

---

## 5. 已修复的关键坑
1. **LangChain4j 1.15 API 偏差**：`ChatModel`/`ChatRequest`/`ToolExecutionResultMessage.from(req,result)`/`JsonObjectSchema` 新包路径——全部以 `javap` 实证修正。
2. **本地哈希向量中文失配**：原按整串做 token（中文无空格）→ 0 命中；改为**按字符 Unigram 哈希累加 + L2 归一化**，阈值降到 `0.08` 适配稀疏向量。
3. **重复挂号 500**：缺全局异常处理器，`BizException` 直接抛出被 Spring 当 500；新增 `GlobalExceptionHandler`（BizException→400，其余→500）。
4. **Vite 代理误伤前端**：`proxy:'/v2'` 前缀匹配会劫持 `/v2-web/`；收敛为 `proxy:'/v2/api'`。
5. **JDK 版本**：本机默认 JDK 17（class 61），工程需 class 65；统一用 JDK 21 绝对路径启动。
6. **ai_usage_log.error_msg 列宽溢出**：DashScope 限流错误 JSON（262 字符）超 VARCHAR(255)，降级日志写入本身抛异常 → 500；列宽放宽到 1000 + 代码截断 200 字符。
7. **EmbeddingGateway 鸡生蛋 bug**：构造器 `if (useReal())` 才创建 `remote`，而 `useReal()` 又要求 `remote != null` → 真实 embedding 永远不启用；拆分 `shouldUseReal()`（仅看配置）修复。
8. **qwen-turbo 无配额**：该模型在账号上被 `limit_requests` 限流；实测 `qwen-flash`/`qwen-plus` 可用，默认改 `qwen-flash`。
9. **真实模型 ReAct 不收敛**（4 步收敛的关键改造）：
   - 系统提示词动态注入**今天日期**（原来模型幻觉出 2024 日期）+ 步数预算 + 「查到号源立即输出 JSON」硬约束；
   - `department_query` 容错：按科室名查不到时自动联动 `symptom_department_map`（模型爱用「咳嗽」当关键词，科室名里根本没有）；
   - `schedule_query` 容错：period 归一化（morning/上午→AM）+ 指定日期无排班时自动返回未来 7 天号源。
   - 效果：7 步不收敛 → **4 步收敛**，confidence 0.9，reason 为模型真实生成。

---

## 6. 真实链路实测（2026-11-11 补全后）

| 项 | 结果 |
|---|---|
| 真实对话模型 | ✅ qwen-flash（DashScope），分诊 5 次调用 5718 token、均耗 ~700ms、成功率 100% |
| 真实语义向量 | ✅ text-embedding-v3（1024 维），启动补齐 4 片段 135 token；「哮喘急性发作怎么办」top1 正确命中急救知识 score 0.878 |
| ReAct 收敛 | ✅ 4 步收敛（symptom→department→doctor→schedule→JSON），危急主诉代码硬拦截优先于模型 |
| 生产 MySQL | ✅ 本机 MySQL 9.2 实跑：23 表一次导入成功，6 工具真开关/分诊/挂号/看板全链路在 MySQL 上复验通过 |
| 浏览器回归 | ✅ agent-browser 真实点击：工具注册表→看板→预问诊（SSE 追问）→发起分诊（真实模型）→挂号预约，全程无 JS 报错，截图确认 VITAL 视觉无错乱 |

> **已部署公网（2026-11-11）**：
> - 入口：`http://114.215.186.113/v2-web/` 与 `https://uniquedevjing.me:8443/v2-web/`
> - 服务器：root@114.215.186.113；后端 systemd `hospital-v2.service`（`/opt/hospital-v2/hospital-v2.jar`，JVM `-Xmx256m -XX:+UseSerialGC`，端口 127.0.0.1:18092，`SPRING_PROFILES_ACTIVE=mysql`）
> - 数据库：服务器 MySQL `hospital_v2` 库（23 表，用户复用 `health`）；真实模型 Key 在 `/opt/hospital-v2/hospital-v2.env`（chmod 600）
> - 前端：`/opt/hospital-v2/web/`，nginx `hospital-v2-locations.conf`（assets 长缓存 + index no-cache + SPA fallback + SSE 禁压），include 挂在 crossborder(80)/domain-https(80,8443)/ip-root(80) 四个 server 块
> - 注意：IP 直连入口是 `sites-enabled/ip-root`（不是 crossborder 的 `server_name _`），新增子路径必须同时在 ip-root 补 include，否则 IP 访问 404 落到 FastAPI catch-all
> - 回滚：备份 `dist.bak` 机制沿用；停服 `systemctl stop hospital-v2`；nginx 摘除 include + reload 即完全下线

---

## 7. 交付物清单
- `hospital-v2/`：后端工程（21 个 Java 类 + 23 表 DDL/种子 + 2 测试类），`target/hospital-v2.jar`（31MB）。
- `hospital-web-v2/`：前端工程（7 管理/患者视图 + 路由 + API 层），`dist/` 构建产物。
- 本报告 `REFACTOR_V2.md`。

---

## 8. 课程清单补全（2026-11-11 晚，Task #33–#37）

对照 aigcbaba.com/course/120 官方功能清单深度学习后补全，已部署公网并全链路回归。

**功能补全**
- 管理端：`BaseData` 六页签（科室/医生含账号开通/排班号源含一键顺延/患者档案/症状标签/危急征象）；`AiConfig`（模型 CRUD + Prompt 模板 + LangChain4j 探针连通测试）；知识库 txt/md 文件上传解析入库；工具注册/编辑/删除；看板增强（挂号量、号源使用率、按医生汇总三表 JOIN）。
- 医生端：新增 `DoctorConsole`——接诊列表（挂到自己名下的患者含既往史/过敏史）+ 七段病历结构化录入（主诉/现病史/既往史/过敏史/辅助检查/诊断/处理意见），DRAFT 可编辑、SUBMITTED 锁定，提交联动挂号 DONE。
- 患者端：个人档案维护（性别/年龄/慢病史/过敏史）；七段病历查看对话框；签名按钮移除（签名归医生操作）。

**数据补齐**（依据公开医学资料）：10 科室、16 医生、44 班次（868 号源）、6 患者档案、20 症状标签、10 危急规则（+消化道大出血/严重过敏/糖尿病急症/高血压急症/活动性咯血）、41 条症状映射、3 模型配置（qwen-flash CHAT 含 prompt_template / text-embedding-v3 / qwen-plus 备用）、4 知识库 6 文档 26 片段真实医学内容。

**权限体系（本轮已补齐）**：患者自助注册与三角色登录、BCrypt 密码、JWT 无状态认证、前后端 RBAC、患者/医生业务数据归属校验，以及模型密钥脱敏。生产环境必须设置 `AUTH_JWT_SECRET`（至少 32 字符）。

**部署验证**（2026-11-11 19:07 公网实测）
- 服务器重建 `hospital_v2` 库导入新 schema/data；26 片段启动即经 text-embedding-v3 补齐 READY。
- 公网闭环实测：分诊（危急胸痛→急诊硬拦截 / 咳嗽发烧→呼吸内科真实模型 4 步收敛）→ 挂号（含重复预约幂等 400 拦截）→ 医生写病历 → 提交 → 挂号 DONE 联动，全部通过。
- 过程修复：① 病历提交的多表 `UPDATE JOIN` 改子查询（H2 兼容）；② pom `finalName` 参数化为 `${jar.name}`（本机 jar 被锁时换名构建）。
- 部署物：`/opt/hospital-v2/hospital-v2.jar` + `sql/` + `web/`，备份在 `backup-20261111/`；git `refactor/v2` 分支 commit `0371d3b` 已推送。

### 8.2 鉴权、运行时配置与真实流式（本轮本地补齐）

- 新增 `/api/auth/login|register|me`，患者可自助注册；管理员、医生、患者接口按角色隔离，患者会话/建议/挂号/病历与医生接诊/病历均校验数据归属。
- AI 模型配置与 Prompt 模板成为运行时数据源，后台启用的 CHAT 配置无需重启即可生效；密钥仅从环境变量读取，后台列表不回显。
- 预问诊改为模型 token 回调驱动的真实 SSE，不再等待整段结果后模拟切片；完成后再一次性持久化回复。
- 前端统一 `/v2/api`、自动携带 Bearer Token、401 回登录，并移除患者/医生 ID 手工选择及患者写病历入口。

### 8.1 症状标签库（2026-11-11 19:26 追加）

课程管理端第 4 项「症状标签库：维护症状标签与科室映射，标记危急征象」落地：
- 新增 `SymptomLibrary.vue`（`/admin/symptoms`）三卡整合：① 症状标签 CRUD（行内「危急」徽标 = 标签名与危急规则命中关键词的双向包含判断）② 症状→科室映射 CRUD（`symptom_department_map` 首次暴露管理端，重复映射 400 拦截，这是分诊 Agent `department_query` 容错回退的真实载体）③ 危急征象规则 CRUD（紧急/高危/注意三级色标）。
- 后端补 `/symptom-maps` GET/save/delete + `/symptom-tags/delete` + `/red-flags/delete`；修复 save 参数名缺 `k` 的绑定 bug。
- 本地 H2 冒烟（新增/重复拦截/更新/删除全通）→ 公网部署（jar+dist，服务 active）→ 公网 API 41 条映射返回正常、SPA 200、新 JS bundle 加载成功。浏览器层回归被本地代理拦截（ERR_BLOCKED_BY_CLIENT，curl 证据已充分）。
- git `a4b39c8` 已推送。
