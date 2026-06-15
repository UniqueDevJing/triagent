# 传智健康管理系统 (Health Management System)

全栈健康管理平台，基于 Spring Boot 3 + Vue 3 构建。支持用户健康档案管理、评估、干预计划、知识库及 AI 健康助手。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2, Java 17 |
| ORM | MyBatis-Plus 3.5 |
| 安全 | Spring Security + JWT (HMAC-SHA384) |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 前端框架 | Vue 3.4 + Element Plus + Pinia |
| 构建工具 | Vite 5 / Maven |
| API 文档 | Knife4j (Swagger) |
| 反向代理 | Nginx 1.26 |

## 功能模块

- **工作台** — 数据概览、统计图表、年龄分布、评估趋势
- **用户管理** — 健康档案 CRUD、BMI 计算
- **健康评估** — 量表模板、评分计算、风险等级判定
- **健康干预** — 干预计划制定、任务看板（Kanban）
- **知识库** — 分类管理、文章发布、搜索
- **AI 健康助手** — DeepSeek API 驱动、实时对话
- **实时推送** — SSE 事件广播

## 快速启动

### Docker 部署（推荐）

```bash
docker-compose up -d
```

访问 http://localhost 即可使用。

### 本地开发

**环境要求：** JDK 17+, Maven 3.9+, Node.js 20+, MySQL 8.0+

**1. 初始化数据库**

```bash
mysql -u root -p < health-server/db/init.sql
```

**2. 启动后端**

```bash
cd health-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**3. 启动前端**

```bash
cd health-web
npm install
npm run dev
```

访问 http://localhost:3000。

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 系统管理员 |

## API 文档

启动后端后访问 http://localhost:8080/doc.html

## 项目结构

```
health-management-system/
├── health-server/          # Spring Boot 后端
│   ├── src/main/java/com/chuanzhi/health/
│   │   ├── config/         # Security、JWT、Redis 配置
│   │   ├── controller/     # REST 控制器
│   │   ├── service/        # 业务逻辑层
│   │   ├── mapper/         # MyBatis-Plus Mapper
│   │   ├── entity/         # 数据实体
│   │   ├── dto/            # 数据传输对象
│   │   ├── enums/          # 枚举（状态、风险等级）
│   │   └── common/         # 统一返回、异常处理
│   └── db/                 # 数据库脚本
├── health-web/             # Vue 3 前端
│   └── src/
│       ├── views/          # 页面组件
│       ├── stores/         # Pinia 状态管理
│       ├── api/            # API 请求层
│       ├── router/         # 路由配置
│       └── components/     # 通用组件
├── nginx-1.26.2/           # Nginx 配置
├── docker-compose.yml
└── Dockerfile
```
