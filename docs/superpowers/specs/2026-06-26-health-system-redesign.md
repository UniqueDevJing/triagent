# 传智健康管理系统 - 架构重设计

## 概述

基于现有 Spring Boot 3 + Vue 3 项目进行全面重构，新增会员管理、预约管理、扩展健康评估/干预/知识库、系统设置等模块。同时新增会员端 H5。

### 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2, Java 17, MyBatis-Plus 3.5 |
| 安全 | Spring Security + JWT (HMAC-SHA384) |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 管理端 | Vue 3.4 + Element Plus + Pinia + Vite 5 |
| 会员端 | Vue 3.4 + Vant/Mobile UI + Pinia + Vite 5 |
| API 文档 | Knife4j |
| 反向代理 | Nginx 1.26 |

### 角色与权限

| 角色 | 访问端 | 可见模块 |
|------|--------|---------|
| ADMIN | 管理端 | 全部 |
| DOCTOR | 管理端 | 工作台、健康评估、健康干预、知识库、会员管理 |
| NURSE | 管理端 | 工作台、预约管理、健康档案、知识库 |
| MEMBER | 会员端 | 预约、档案、评估结果、健康方案、知识查看 |

---

## 数据模型（25 张表）

### 系统设置域

**users** - 系统用户（管理员/医生/护士）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| username | VARCHAR(50) UNIQUE | 登录名 |
| password | VARCHAR(200) | BCrypt 加密 |
| role | VARCHAR(20) | ADMIN/DOCTOR/NURSE |
| name | VARCHAR(50) | 姓名 |
| phone | VARCHAR(20) | |
| email | VARCHAR(100) | |
| department_id | BIGINT FK | 所属科室 |
| status | TINYINT | 0禁用 1启用 |

**departments** - 科室
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(50) | |
| code | VARCHAR(20) | |
| parent_id | BIGINT | 上级科室 |
| sort_order | INT | |

**roles** - 角色定义
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(50) | |
| code | VARCHAR(20) | ADMIN/DOCTOR/NURSE |
| menus | JSON | 菜单权限列表 |

**menus** - 菜单配置
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| parent_id | BIGINT | 上级菜单 |
| name | VARCHAR(50) | 菜单名 |
| path | VARCHAR(100) | 路由路径 |
| icon | VARCHAR(50) | 图标 |
| sort_order | INT | |

### 会员管理域

**members** - 会员（被服务对象）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(50) | 姓名 |
| gender | TINYINT | 0未知 1男 2女 |
| age | INT | |
| id_card | VARCHAR(18) | 身份证号 |
| phone | VARCHAR(20) | 手机号（登录用） |
| emergency_contact | VARCHAR(50) | 紧急联系人 |
| emergency_phone | VARCHAR(20) | |
| blood_type | VARCHAR(5) | |
| height | DECIMAL(5,2) | |
| weight | DECIMAL(5,2) | |
| medical_history | JSON | 既往病史 |
| allergies | JSON | 过敏史 |
| member_level | VARCHAR(20) | 会员等级 |
| status | TINYINT | |

**physical_exam_plans** - 体检计划
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| member_id | BIGINT FK | |
| plan_name | VARCHAR(100) | |
| start_date | DATE | |
| end_date | DATE | |
| description | VARCHAR(500) | |
| status | VARCHAR(20) | |
| created_by | BIGINT FK→users | |

### 预约管理域

**appointments** - 预约
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| member_id | BIGINT FK | |
| package_id | BIGINT FK | |
| appointment_date | DATE | |
| time_slot | VARCHAR(20) | 时间段 |
| status | VARCHAR(20) | PENDING/CONFIRMED/DONE/CANCELLED |
| notes | VARCHAR(500) | |

**packages** - 套餐
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(100) | |
| description | VARCHAR(500) | |
| price | DECIMAL(10,2) | |
| icon | VARCHAR(50) | |
| status | TINYINT | |

**package_items** - 套餐项目明细
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| package_id | BIGINT FK | |
| exam_item_id | BIGINT FK | |
| sort_order | INT | |

**exam_items** - 检测项
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(100) | |
| description | VARCHAR(500) | |
| reference_range | VARCHAR(200) | 参考范围 |
| unit | VARCHAR(20) | |
| category_id | BIGINT FK | 项目组 |

**exam_item_categories** - 检测项目组
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(50) | |
| sort_order | INT | |

### 健康评估域

**assessment_records** - 评估记录
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| member_id | BIGINT FK | |
| template_id | BIGINT FK | |
| assessor_id | BIGINT FK→users | |
| type | VARCHAR(20) | RISK/TCM/PSYCHOLOGY |
| score | DECIMAL(5,2) | |
| risk_level | VARCHAR(20) | LOW/MEDIUM/HIGH/CRITICAL |
| conclusion | TEXT | |
| suggestion | TEXT | |
| detailed_data | JSON | |
| assessed_at | DATETIME | |

**assessment_templates** - 评估模板
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(100) | |
| type | VARCHAR(20) | |
| indicator_ids | JSON | 关联指标 |
| tcm_type_ids | JSON | 中医体质类型 |
| psychology_ids | JSON | 心理评测 |
| status | TINYINT | |

**assessment_indicators** - 评估指标
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(100) | |
| code | VARCHAR(50) | |
| unit | VARCHAR(20) | |
| reference_min | DECIMAL(10,2) | |
| reference_max | DECIMAL(10,2) | |
| category | VARCHAR(20) | BLOOD/URINE/IMAGING/PHYSICAL |

**tcm_constitutions** - 中医体质
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(50) | 平和质/气虚质/阳虚质... |
| description | TEXT | |
| features | TEXT | 特征描述 |
| advice | TEXT | 调养建议 |

**psychology_assessments** - 心理评测量表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(100) | |
| description | TEXT | |
| questions | JSON | 问卷题目 |

### 健康干预域

**intervention_plans** - 干预方案
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| member_id | BIGINT FK | |
| type | VARCHAR(20) | CROWD/CHRONIC/DIET |
| title | VARCHAR(200) | |
| description | TEXT | |
| start_date | DATE | |
| end_date | DATE | |
| status | VARCHAR(20) | |
| result | JSON | |

**intervention_tasks** - 干预任务
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| plan_id | BIGINT FK | |
| title | VARCHAR(200) | |
| description | TEXT | |
| status | VARCHAR(20) | TODO/IN_PROGRESS/DONE |
| due_date | DATE | |
| completed_at | DATETIME | |

**chronic_disease_mgmt** - 慢病管理
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| member_id | BIGINT FK | |
| disease_type | VARCHAR(50) | 高血压/糖尿病/冠心病/COPD |
| diagnosis_date | DATE | |
| medication | JSON | 用药记录 |
| target_indicators | JSON | 目标指标 |
| monitoring_frequency | VARCHAR(20) | 监测频率 |

**diet_logs** - 膳食日志
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| member_id | BIGINT FK | |
| meal_type | VARCHAR(20) | BREAKFAST/LUNCH/DINNER/SNACK |
| food_items | JSON | |
| calories | INT | |
| recorded_date | DATE | |

**crowd_programs** - 人群方案
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(100) | |
| target_group | JSON | 目标人群 |
| description | TEXT | |
| content | JSON | |

### 知识库域

**knowledge_categories** - 知识分类（扩展 type 字段）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(50) | |
| type | VARCHAR(20) | ASSESSMENT/PLAN/EXERCISE/FOOD/DISEASE/EDUCATION/RECIPE/SCIENCE |
| description | VARCHAR(200) | |
| sort_order | INT | |

**knowledge_articles** - 知识文章
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| category_id | BIGINT FK | |
| title | VARCHAR(200) | |
| summary | VARCHAR(500) | |
| content | TEXT | Markdown |
| author | VARCHAR(50) | |
| tags | VARCHAR(200) | |
| view_count | INT | |
| is_published | TINYINT | |

**education_contents** - 宣教内容
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| title | VARCHAR(200) | |
| content_type | VARCHAR(20) | WORD/VIDEO/IMAGE |
| content | JSON | |
| tags | VARCHAR(200) | |
| status | TINYINT | |

**education_words** - 宣教词管理
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| title | VARCHAR(200) | |
| content | TEXT | |
| tags | VARCHAR(200) | |
| status | TINYINT | |

**exercise_library** - 运动项目库
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(100) | |
| type | VARCHAR(20) | AEROBIC/STRENGTH/FLEXIBILITY |
| description | TEXT | |
| duration | INT | 建议时长(分钟) |
| intensity | VARCHAR(20) | |
| suitable_for | VARCHAR(500) | |

**disease_library** - 疾病库
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(100) | |
| department | VARCHAR(50) | |
| symptoms | TEXT | |
| causes | TEXT | |
| treatments | JSON | |

**health_recipes** - 食谱库
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(100) | |
| type | VARCHAR(20) | MEAL/SNACK/SOUP |
| ingredients | JSON | |
| steps | JSON | |
| calories | INT | |
| suitable_for | VARCHAR(500) | |
| image | VARCHAR(200) | |

### 统计分析域

**statistics_cache** - 统计数据缓存
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| type | VARCHAR(30) | DASHBOARD/APPOINTMENT/ASSESSMENT/MEMBER |
| date | DATE | |
| data | JSON | |

_注：notifications、ai_conversations、health_records 等现有表保留不变。_

---

## API 架构

### 管理端 `/api/admin/*`（JWT 认证）

- **Auth**: login, logout, me
- **Members**: CRUD + 体检计划管理 + 关联评估/干预查询
- **Appointments**: CRUD + 状态流转 + 批量导入(Excel) + 模板导出 + 打印
- **Packages**: CRUD + 项目明细
- **Exam Items**: CRUD + 按项目组筛选
- **Exam Item Categories**: CRUD
- **Assessments**: 评估记录 CRUD + 模板管理 + 指标管理 + 中医体质 CRUD + 心理评测 CRUD
- **Interventions**: 干预方案 CRUD + 任务 CRUD + 人群方案 CRUD + 慢病管理 CRUD + 膳食日志 CRUD
- **Knowledge**: 分类 CRUD + 文章 CRUD + 宣教内容 CRUD + 宣教词 CRUD + 运动项目 CRUD + 疾病库 CRUD + 食谱 CRUD
- **System**: 用户 CRUD + 角色菜单配置 + 科室 CRUD + 菜单树
- **Dashboard**: 统计数据
- **SSE**: 实时推送
- **Notifications**: 通知中心

### 会员端 `/api/member/*`（手机号+验证码）

- **Auth**: 手机号登录, me
- **Profile**: 查看/编辑个人档案
- **Appointments**: 我的预约列表 + 自助预约
- **Assessments**: 我的评估结果
- **Interventions**: 我的干预方案
- **Diet Logs**: 膳食记录 CRUD
- **Knowledge**: 文章/食谱/宣教内容查看

---

## 前端架构

### 管理端 `health-admin` (Vue 3 + Element Plus)

路由: `/admin/*`

```
admin/
├── dashboard/           # 工作台
├── members/             # 会员管理
│   ├── list             # 会员列表
│   ├── :id              # 会员详情
│   └── :id/exam-plan    # 体检计划
├── appointments/        # 预约管理
│   ├── list             # 预约列表+日历
│   ├── create           # 新增预约
│   ├── :id              # 预约详情+打印
│   ├── packages/        # 套餐管理
│   ├── exam-items/      # 检测项管理
│   └── exam-categories/ # 项目组管理
├── assessments/         # 健康评估
│   ├── list             # 评估记录
│   ├── create           # 新增评估
│   ├── :id              # 评估详情
│   ├── indicators/      # 指标管理
│   ├── tcm/             # 中医体质
│   └── psychology/      # 心理评测
├── interventions/       # 健康干预
│   ├── list             # 方案列表
│   ├── create           # 创建方案
│   ├── :id              # 方案详情+任务
│   ├── crowd/           # 人群方案
│   ├── chronic/         # 慢病管理
│   └── diet-logs/       # 膳食日志
├── knowledge/           # 知识库
│   ├── articles/        # 文章管理
│   ├── education/       # 宣教内容
│   ├── exercises/       # 运动项目
│   ├── diseases/        # 疾病库
│   ├── recipes/         # 食谱库
│   └── education-words/ # 宣教词
├── statistics/          # 统计分析
└── system/              # 系统设置
    ├── users/           # 用户管理
    ├── roles/           # 角色设置
    ├── departments/     # 科室管理
    └── menus/           # 菜单管理
```

### 会员端 `health-member` (Vue 3 + Vant UI)

路由: `/m/*`

```
m/
├── /                    # 首页
├── /profile             # 我的档案
├── /appointments/       # 我的预约
│   └── /create          # 自助预约
├── /assessments/        # 评估结果
├── /interventions/      # 干预方案
├── /diet/               # 膳食记录
├── /knowledge/          # 健康知识
│   ├── /articles        # 科普文章
│   ├── /recipes         # 食谱
│   └── /education       # 宣教内容
└── /login               # 登录
```

---

## 安全设计

- 管理端：JWT (HMAC-SHA384)，用户名+密码登录，token 24h 过期
- 会员端：手机号+验证码登录，JWT token
- 角色级权限控制：不同角色看到不同菜单和 API
- 管理端 `/api/admin/**` 全部需认证（除 auth 接口）
- 会员端 `/api/member/**` 全部需认证（除 auth 接口）
- CORS 限制：管理端允许 localhost:3000，会员端允许 localhost:3001
- XSS/CSRF 防护

## 部署架构

```
Nginx :80
├── /admin/*  → health-admin dist/
├── /m/*      → health-member dist/
├── /api/     → Spring Boot :8080
└── SPA fallback
```

Docker Compose 一键部署：
- MySQL 8.0
- Redis 7
- Spring Boot (backend)
- Nginx (前端静态 + API 代理)
