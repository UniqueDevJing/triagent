# 传智健康管理系统 — 若依框架重构设计文档

**日期：** 2026-06-29
**状态：** 已确认

## 一、技术选型

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.2 + Java 17 |
| ORM | MyBatis-Plus 3.5 |
| 认证授权 | Sa-Token（若依默认） |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 前端 | Vue 3.4 + Element Plus + Vite 5（若依 Vue3 官方模板） |
| API 文档 | Knife4j (Swagger) |
| AI | DeepSeek API + SSE 流式推送 |
| 容器化 | Docker Compose（MySQL + Redis + Nginx） |
| 构建工具 | Maven 多模块 + Vite |

## 二、Maven 模块依赖

```
health-admin ──→ health-framework ──→ health-system ──→ health-common
     │                                                         ↑
     ├────→ health-generator ──────────────────────────────────┤
     └────→ health-quartz ─────────────────────────────────────┘
```

## 三、模块职责与包结构

### health-common — 底层公共基础包
```
com.health.common
├── core/          # BaseEntity、BaseController
├── enums/         # 通用枚举（状态、删除标记）
├── exception/     # 全局异常 + 业务异常
├── annotation/    # 自定义注解（日志、权限、重复提交）
├── filter/        # XSS 过滤器
├── utils/         # 通用工具（字符串、日期、反射、IP 等）
└── config/        # 通用配置（线程池、Jackson 等）
```

### health-system — 系统 + 业务模块
```
com.health.system
├── domain/        # DO 实体
│   ├── SysUser、SysRole、SysMenu、SysDept、SysPost     # 若依原生
│   ├── Member、ExamPlan                               # 会员管理
│   ├── Appointment、Package、ExamItem、ExamItemGroup    # 预约管理
│   ├── AssessmentRecord、Indicator、TcmConstitution    # 健康评估
│   ├── PsychologyAssessment                          # 心理评测
│   ├── InterventionPlan、CrowdProgram、ChronicDisease  # 健康干预
│   ├── DietLog                                       # 膳食日志
│   ├── KnowledgeArticle、Exercise、Recipe、Disease     # 知识库
│   ├── EducationContent、EducationWord                # 宣教内容
│   ├── AiConversation、AiMessage                      # AI对话
│   └── SysOperLog、SysLogininfor                       # 系统日志
├── mapper/        # MyBatis Mapper 接口
├── service/       # 业务服务接口 + impl
└── vo/            # 返回视图对象
```

### health-framework — 框架基础层
```
com.health.framework
├── security/      # Sa-Token 认证、权限校验
├── interceptor/   # 拦截器（日志、防重复提交）
├── aspect/        # AOP 切面（操作日志、数据权限）
├── config/        # MyBatis-Plus、Swagger、文件上传配置
├── ai/            # DeepSeek 客户端、AI 配置、提示词模板
└── manager/       # 异步工厂、缓存管理
```

### health-admin — 启动主模块
```
com.health.web
├── controller/
│   ├── system/        # 用户/角色/菜单/部门 控制器（若依原生）
│   ├── member/        # 会员档案、体检计划、统计上报
│   ├── appointment/   # 预约列表、套餐管理、检测项/组管理
│   ├── assessment/    # 风险评估、指标管理、中医体质、心理评测
│   ├── intervention/  # 人群方案、慢病管理、膳食日志
│   ├── knowledge/     # 运动库、食谱库、疾病库、宣教内容
│   └── ai/            # AI 对话接口（SSE 流式推送）
└── HealthApplication.java
```

### health-generator — 代码生成器
```
com.health.generator
├── controller/    # 代码生成控制器
├── service/       # 模板引擎服务
└── config/        # 生成器配置
```

### health-quartz — 定时任务
```
com.health.quartz
├── controller/    # 任务管理控制器
├── service/       # 定时任务服务
└── task/          # 具体任务实现
    ├── ExamReminderTask       # 体检提醒
    ├── ReportGenerateTask     # 报告生成
    └── StatisticsArchiveTask  # 数据统计归档
```

## 四、数据库表

### 系统管理（若依原生）
sys_user、sys_role、sys_menu、sys_dept、sys_post、sys_dict_data、sys_dict_type、sys_config、sys_oper_log、sys_logininfor

### 业务模块
| 模块 | 表名 |
|------|------|
| 会员管理 | member、exam_plan、member_statistics |
| 预约管理 | appointment、package_info、package_item_detail、exam_item、exam_item_group |
| 健康评估 | assessment_record、assessment_indicator、tcm_constitution、psychology_assessment |
| 健康干预 | intervention_plan、crowd_program、chronic_disease、diet_log |
| 知识库 | knowledge_article、exercise_library、recipe_library、disease_library、education_content、education_word |
| AI对话 | ai_conversation、ai_message |

## 五、前端路由

```
/                          → 首页仪表盘
/login                     → 登录页
/system/user|role|menu|dept → 系统管理（若依原生）
/member/*                  → 会员管理（档案/体检计划/统计）
/appointment/*             → 预约管理（预约/套餐/检测项）
/assessment/*              → 健康评估（风险/指标/体质/心理）
/intervention/*            → 健康干预（方案/慢病/膳食）
/knowledge/*               → 知识库（运动/食谱/疾病/宣教）
/ai-agent                  → AI 健康助手（DeepSeek 对话）
/monitor/*                 → 监控（在线用户/定时任务/数据监控）
```

## 六、实施步骤

1. 创建父工程 pom.xml，定义 6 个子模块
2. 搭建 health-common（基础类+工具）
3. 搭建 health-system（实体+Mapper+Service）
4. 搭建 health-framework（安全+AI客户端）
5. 搭建 health-admin（Controller+启动类）
6. 搭建 health-generator + health-quartz
7. 初始化若依 Vue3 前端项目，配置路由和权限
8. 开发业务页面（会员/预约/评估/干预/知识库/AI对话）
9. Docker Compose 整合 + 测试

## 七、IDEA 开发支持

- 项目根目录作为 Maven 多模块项目，IDEA `File → Open → 选根 pom.xml` 自动识别全部子模块
- 每个模块标准目录：`src/main/java`、`src/main/resources`、`src/test/java`
- 父 POM 统一版本管理，子模块继承
- 包含 `.idea/runConfigurations/` 启动配置
- Maven Wrapper (mvnw) 确保无需全局 Maven 安装
