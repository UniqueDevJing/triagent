# 传智健康管理系统 — 若依框架重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于若依 Vue3 架构，从零搭建传智健康管理系统（Maven 多模块 + Vue3 前端）

**Architecture:** 6 个 Maven 子模块按依赖关系分层构建，前端采用若依 Vue3 官方模板重写

**Tech Stack:** Spring Boot 3.2, Java 17, MyBatis-Plus 3.5, Sa-Token, MySQL 8.0, Redis 7, Vue 3.4, Element Plus, Vite 5, DeepSeek API

## Global Constraints

- Java 17+, Spring Boot 3.2, Maven 3.9+
- 包根路径: `com.health`
- Maven GroupId: `com.health`, ArtifactId 分别对应各模块名
- IDEA 可直接 Open 根 pom.xml 识别全部子模块
- 前端使用若依 Vue3 官方模板 (ruoyi-vue3)
- AI 使用 DeepSeek API，SSE 流式推送
- 父 POM 统一版本管理，子模块只声明特有依赖

---

## 文件结构总览

```
health-management-system/
├── pom.xml                              # 父 POM
├── health-common/
│   ├── pom.xml
│   └── src/main/java/com/health/common/
│       ├── core/BaseController.java
│       ├── core/BaseEntity.java
│       ├── core/PageResult.java
│       ├── core/AjaxResult.java
│       ├── enums/UserStatus.java
│       ├── enums/HttpStatus.java
│       ├── exception/ServiceException.java
│       ├── exception/GlobalExceptionHandler.java
│       ├── annotation/Log.java
│       ├── annotation/RepeatSubmit.java
│       ├── utils/StringUtils.java
│       ├── utils/ServletUtils.java
│       └── config/HealthCommonConfig.java
├── health-system/
│   ├── pom.xml
│   └── src/main/java/com/health/system/
│       ├── domain/SysUser.java
│       ├── domain/SysRole.java
│       ├── domain/SysMenu.java
│       ├── domain/SysDept.java
│       ├── domain/Member.java
│       ├── domain/ExamPlan.java
│       ├── domain/Appointment.java
│       ├── domain/PackageInfo.java
│       ├── domain/ExamItem.java
│       ├── domain/ExamItemGroup.java
│       ├── domain/AssessmentRecord.java
│       ├── domain/AssessmentIndicator.java
│       ├── domain/TcmConstitution.java
│       ├── domain/PsychologyAssessment.java
│       ├── domain/InterventionPlan.java
│       ├── domain/CrowdProgram.java
│       ├── domain/ChronicDisease.java
│       ├── domain/DietLog.java
│       ├── domain/KnowledgeArticle.java
│       ├── domain/ExerciseLibrary.java
│       ├── domain/RecipeLibrary.java
│       ├── domain/DiseaseLibrary.java
│       ├── domain/EducationContent.java
│       ├── domain/AiConversation.java
│       ├── domain/AiMessage.java
│       ├── mapper/SysUserMapper.java
│       ├── mapper/SysRoleMapper.java
│       ├── mapper/SysMenuMapper.java
│       ├── mapper/MemberMapper.java
│       ├── mapper/AppointmentMapper.java
│       ├── mapper/AssessmentRecordMapper.java
│       ├── mapper/InterventionPlanMapper.java
│       ├── mapper/KnowledgeArticleMapper.java
│       ├── mapper/AiConversationMapper.java
│       ├── service/ISysUserService.java
│       ├── service/IMemberService.java
│       ├── service/IAppointmentService.java
│       ├── service/IAssessmentService.java
│       ├── service/IInterventionService.java
│       ├── service/IKnowledgeService.java
│       ├── service/IAiConversationService.java
│       └── service/impl/*.java
├── health-framework/
│   ├── pom.xml
│   └── src/main/java/com/health/framework/
│       ├── security/SaTokenConfig.java
│       ├── security/StpInterfaceImpl.java
│       ├── interceptor/LogInterceptor.java
│       ├── aspect/LogAspect.java
│       ├── ai/DeepSeekClient.java
│       ├── ai/AiConfig.java
│       └── config/FrameworkConfig.java
├── health-admin/
│   ├── pom.xml
│   └── src/main/java/com/health/web/
│       ├── controller/system/SysUserController.java
│       ├── controller/system/SysRoleController.java
│       ├── controller/system/SysMenuController.java
│       ├── controller/member/MemberController.java
│       ├── controller/appointment/AppointmentController.java
│       ├── controller/appointment/PackageController.java
│       ├── controller/appointment/ExamItemController.java
│       ├── controller/assessment/AssessmentController.java
│       ├── controller/assessment/TcmController.java
│       ├── controller/assessment/PsychologyController.java
│       ├── controller/intervention/InterventionController.java
│       ├── controller/intervention/DietLogController.java
│       ├── controller/knowledge/KnowledgeController.java
│       ├── controller/ai/AiController.java
│       ├── HealthApplication.java
│       └── resources/application.yml
├── health-generator/
│   ├── pom.xml
│   └── src/main/java/com/health/generator/
│       ├── controller/GenController.java
│       └── service/GenService.java
├── health-quartz/
│   ├── pom.xml
│   └── src/main/java/com/health/quartz/
│       ├── controller/SysJobController.java
│       ├── service/ISysJobService.java
│       └── task/ExamReminderTask.java
├── health-web/                     # 从 ruoyi-vue3 模板初始化
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── router/index.js
│       ├── stores/user.js
│       ├── api/
│       │   ├── system.js
│       │   ├── member.js
│       │   ├── appointment.js
│       │   ├── assessment.js
│       │   ├── intervention.js
│       │   ├── knowledge.js
│       │   └── ai.js
│       └── views/
│           ├── login/index.vue
│           ├── index.vue
│           ├── system/user/index.vue
│           ├── system/role/index.vue
│           ├── member/index.vue
│           ├── appointment/index.vue
│           ├── assessment/index.vue
│           ├── intervention/index.vue
│           ├── knowledge/index.vue
│           └── ai/index.vue
├── sql/init.sql
├── docker-compose.yml
└── .env.example
```

---

### Task 1: 创建父工程 POM

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: 编写父 POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.health</groupId>
    <artifactId>health-management-system</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>Health Management System</name>
    <description>传智健康管理系统 - 基于若依框架</description>

    <modules>
        <module>health-common</module>
        <module>health-system</module>
        <module>health-framework</module>
        <module>health-admin</module>
        <module>health-generator</module>
        <module>health-quartz</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.2.0</spring-boot.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <sa-token.version>1.37.0</sa-token.version>
        <knife4j.version>4.3.0</knife4j.version>
        <druid.version>1.2.20</druid.version>
        <hutool.version>5.8.24</hutool.version>
        <mysql.version>8.0.33</mysql.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- 子模块 -->
            <dependency>
                <groupId>com.health</groupId>
                <artifactId>health-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.health</groupId>
                <artifactId>health-system</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.health</groupId>
                <artifactId>health-framework</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- MyBatis-Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>

            <!-- Sa-Token -->
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-spring-boot3-starter</artifactId>
                <version>${sa-token.version}</version>
            </dependency>
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-redis-jackson</artifactId>
                <version>${sa-token.version}</version>
            </dependency>

            <!-- Knife4j -->
            <dependency>
                <groupId>com.github.xiaoymin</groupId>
                <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
                <version>${knife4j.version}</version>
            </dependency>

            <!-- Druid -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>druid-spring-boot-3-starter</artifactId>
                <version>${druid.version}</version>
            </dependency>

            <!-- Hutool -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>

            <!-- MySQL -->
            <dependency>
                <groupId>com.mysql</groupId>
                <artifactId>mysql-connector-j</artifactId>
                <version>${mysql.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 验证 POM**

```bash
cd C:\Users\Dominion\health-management-system
mvn validate
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: 创建父工程 POM，定义子模块与依赖版本管理"
```

---

### Task 2: 创建 health-common 模块

**Files:**
- Create: `health-common/pom.xml`
- Create: `health-common/src/main/java/com/health/common/core/BaseEntity.java`
- Create: `health-common/src/main/java/com/health/common/core/BaseController.java`
- Create: `health-common/src/main/java/com/health/common/core/PageResult.java`
- Create: `health-common/src/main/java/com/health/common/core/AjaxResult.java`
- Create: `health-common/src/main/java/com/health/common/enums/UserStatus.java`
- Create: `health-common/src/main/java/com/health/common/enums/HttpStatus.java`
- Create: `health-common/src/main/java/com/health/common/exception/ServiceException.java`
- Create: `health-common/src/main/java/com/health/common/exception/GlobalExceptionHandler.java`
- Create: `health-common/src/main/java/com/health/common/annotation/Log.java`
- Create: `health-common/src/main/java/com/health/common/utils/StringUtils.java`

**Interfaces:**
- Produces: `AjaxResult.success()`, `AjaxResult.error()`, `PageResult<T>`, `BaseEntity` (id, createTime, updateTime), `ServiceException`, `@Log` annotation

- [ ] **Step 1: 创建模块 POM**

`health-common/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.health</groupId>
        <artifactId>health-management-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>health-common</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建核心类**

`health-common/src/main/java/com/health/common/core/BaseEntity.java`:
```java
package com.health.common.core;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BaseEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

`health-common/src/main/java/com/health/common/core/AjaxResult.java`:
```java
package com.health.common.core;

import lombok.Data;

@Data
public class AjaxResult {
    private int code;
    private String msg;
    private Object data;

    private AjaxResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static AjaxResult success() {
        return new AjaxResult(200, "操作成功", null);
    }

    public static AjaxResult success(Object data) {
        return new AjaxResult(200, "操作成功", data);
    }

    public static AjaxResult success(String msg, Object data) {
        return new AjaxResult(200, msg, data);
    }

    public static AjaxResult error() {
        return new AjaxResult(500, "操作失败", null);
    }

    public static AjaxResult error(String msg) {
        return new AjaxResult(500, msg, null);
    }

    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }
}
```

`health-common/src/main/java/com/health/common/core/PageResult.java`:
```java
package com.health.common.core;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private List<T> rows;

    private PageResult(long total, List<T> rows) {
        this.total = total;
        this.rows = rows;
    }

    public static <T> AjaxResult page(long total, List<T> rows) {
        return AjaxResult.success(new PageResult<>(total, rows));
    }
}
```

`health-common/src/main/java/com/health/common/core/BaseController.java`:
```java
package com.health.common.core;

public class BaseController {
    protected AjaxResult success() { return AjaxResult.success(); }
    protected AjaxResult success(Object data) { return AjaxResult.success(data); }
    protected AjaxResult success(String msg, Object data) { return AjaxResult.success(msg, data); }
    protected AjaxResult error() { return AjaxResult.error(); }
    protected AjaxResult error(String msg) { return AjaxResult.error(msg); }
    protected AjaxResult toPage(long total, java.util.List<?> rows) {
        return PageResult.page(total, rows);
    }
}
```

- [ ] **Step 3: 创建枚举**

`health-common/src/main/java/com/health/common/enums/UserStatus.java`:
```java
package com.health.common.enums;

import lombok.Getter;

@Getter
public enum UserStatus {
    OK("0", "正常"),
    DISABLE("1", "停用"),
    DELETED("2", "删除");

    private final String code;
    private final String info;

    UserStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }
}
```

`health-common/src/main/java/com/health/common/enums/HttpStatus.java`:
```java
package com.health.common.enums;

public class HttpStatus {
    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int ERROR = 500;
}
```

- [ ] **Step 4: 创建异常类**

`health-common/src/main/java/com/health/common/exception/ServiceException.java`:
```java
package com.health.common.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    private final int code;

    public ServiceException(String message) {
        super(message);
        this.code = 500;
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 5: 创建注解**

`health-common/src/main/java/com/health/common/annotation/Log.java`:
```java
package com.health.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    String title() default "";
    String businessType() default "OTHER";
}
```

- [ ] **Step 6: 验证编译**

```bash
cd C:\Users\Dominion\health-management-system
mvn compile -pl health-common
```
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add health-common/
git commit -m "feat: 创建 health-common 公共基础模块（核心类、异常、枚举、注解）"
```

---

### Task 3: 创建 health-system 模块（系统管理 + 业务实体）

**Files:**
- Create: `health-system/pom.xml`
- Create: `health-system/src/main/java/com/health/system/domain/SysUser.java`
- Create: `health-system/src/main/java/com/health/system/domain/SysRole.java`
- Create: `health-system/src/main/java/com/health/system/domain/SysMenu.java`
- Create: `health-system/src/main/java/com/health/system/domain/SysDept.java`
- Create: `health-system/src/main/java/com/health/system/domain/Member.java`
- Create: `health-system/src/main/java/com/health/system/domain/ExamPlan.java`
- Create: `health-system/src/main/java/com/health/system/domain/Appointment.java`
- Create: `health-system/src/main/java/com/health/system/domain/PackageInfo.java`
- Create: `health-system/src/main/java/com/health/system/domain/ExamItem.java`
- Create: `health-system/src/main/java/com/health/system/domain/ExamItemGroup.java`
- Create: `health-system/src/main/java/com/health/system/domain/AssessmentRecord.java`
- Create: `health-system/src/main/java/com/health/system/domain/AssessmentIndicator.java`
- Create: `health-system/src/main/java/com/health/system/domain/TcmConstitution.java`
- Create: `health-system/src/main/java/com/health/system/domain/PsychologyAssessment.java`
- Create: `health-system/src/main/java/com/health/system/domain/InterventionPlan.java`
- Create: `health-system/src/main/java/com/health/system/domain/CrowdProgram.java`
- Create: `health-system/src/main/java/com/health/system/domain/ChronicDisease.java`
- Create: `health-system/src/main/java/com/health/system/domain/DietLog.java`
- Create: `health-system/src/main/java/com/health/system/domain/KnowledgeArticle.java`
- Create: `health-system/src/main/java/com/health/system/domain/ExerciseLibrary.java`
- Create: `health-system/src/main/java/com/health/system/domain/RecipeLibrary.java`
- Create: `health-system/src/main/java/com/health/system/domain/DiseaseLibrary.java`
- Create: `health-system/src/main/java/com/health/system/domain/EducationContent.java`
- Create: `health-system/src/main/java/com/health/system/domain/AiConversation.java`
- Create: `health-system/src/main/java/com/health/system/domain/AiMessage.java`

- [ ] **Step 1: 创建模块 POM**

`health-system/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.health</groupId>
        <artifactId>health-management-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>health-system</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.health</groupId>
            <artifactId>health-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建系统管理实体**

`health-system/src/main/java/com/health/system/domain/SysUser.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String userName;
    private String nickName;
    private String password;
    private String email;
    private String phoneNumber;
    private String sex;
    private String avatar;
    private String status;
    private Long deptId;
    private String loginIp;
    private java.time.LocalDateTime loginDate;
    private String remark;
}
```

`health-system/src/main/java/com/health/system/domain/SysRole.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private String remark;
}
```

`health-system/src/main/java/com/health/system/domain/SysMenu.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    private String menuName;
    private Long parentId;
    private Integer orderNum;
    private String path;
    private String component;
    private String query;
    private String perms;
    private String icon;
    private String menuType;
    private String visible;
    private String status;
}
```

`health-system/src/main/java/com/health/system/domain/SysDept.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {
    private String deptName;
    private Long parentId;
    private Integer orderNum;
    private String leader;
    private String phone;
    private String email;
    private String status;
}
```

- [ ] **Step 3: 创建业务实体**

`health-system/src/main/java/com/health/system/domain/Member.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member")
public class Member extends BaseEntity {
    private String name;
    private String gender;
    private LocalDate birthday;
    private String phone;
    private String idCard;
    private String address;
    private String bloodType;
    private BigDecimal height;
    private BigDecimal weight;
    private String allergyHistory;
    private String familyHistory;
    private String smokingStatus;
    private String drinkingStatus;
    private String remark;
}
```

`health-system/src/main/java/com/health/system/domain/ExamPlan.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_plan")
public class ExamPlan extends BaseEntity {
    private Long memberId;
    private String planName;
    private LocalDate planDate;
    private Long packageId;
    private String status;
    private String reportPath;
    private String conclusion;
}
```

`health-system/src/main/java/com/health/system/domain/Appointment.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appointment")
public class Appointment extends BaseEntity {
    private Long memberId;
    private Long packageId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String status;
    private String remark;
}
```

`health-system/src/main/java/com/health/system/domain/PackageInfo.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("package_info")
public class PackageInfo extends BaseEntity {
    private String packageName;
    private String description;
    private BigDecimal price;
    private String suitableFor;
    private String status;
}
```

`health-system/src/main/java/com/health/system/domain/ExamItem.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_item")
public class ExamItem extends BaseEntity {
    private String itemName;
    private String itemCode;
    private String unit;
    private BigDecimal price;
    private String referenceRange;
    private Long categoryId;
    private String remark;
}
```

`health-system/src/main/java/com/health/system/domain/ExamItemGroup.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_item_group")
public class ExamItemGroup extends BaseEntity {
    private String groupName;
    private String description;
    private Integer sortOrder;
}
```

`health-system/src/main/java/com/health/system/domain/AssessmentRecord.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_record")
public class AssessmentRecord extends BaseEntity {
    private Long memberId;
    private Long templateId;
    private BigDecimal totalScore;
    private String riskLevel;
    private String conclusion;
    private String suggestion;
    private Long assessorId;
    private LocalDate assessDate;
}
```

`health-system/src/main/java/com/health/system/domain/AssessmentIndicator.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_indicator")
public class AssessmentIndicator extends BaseEntity {
    private String indicatorName;
    private String indicatorType;
    private String unit;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String riskLevel;
    private String status;
}
```

`health-system/src/main/java/com/health/system/domain/TcmConstitution.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tcm_constitution")
public class TcmConstitution extends BaseEntity {
    private Long memberId;
    private String constitutionType;
    private Integer score;
    private String description;
    private String healthAdvice;
}
```

`health-system/src/main/java/com/health/system/domain/PsychologyAssessment.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("psychology_assessment")
public class PsychologyAssessment extends BaseEntity {
    private Long memberId;
    private String assessmentType;
    private Integer totalScore;
    private String resultLevel;
    private String analysis;
    private String suggestion;
    private LocalDate assessDate;
}
```

`health-system/src/main/java/com/health/system/domain/InterventionPlan.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("intervention_plan")
public class InterventionPlan extends BaseEntity {
    private Long memberId;
    private String planName;
    private String planType;
    private String targetGoal;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long creatorId;
}
```

`health-system/src/main/java/com/health/system/domain/CrowdProgram.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crowd_program")
public class CrowdProgram extends BaseEntity {
    private String programName;
    private String targetCrowd;
    private String programContent;
    private String frequency;
    private String status;
}
```

`health-system/src/main/java/com/health/system/domain/ChronicDisease.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chronic_disease")
public class ChronicDisease extends BaseEntity {
    private Long memberId;
    private String diseaseName;
    private String diagnosisDate;
    private String severity;
    private String medication;
    private String controlStatus;
    private String remark;
}
```

`health-system/src/main/java/com/health/system/domain/DietLog.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("diet_log")
public class DietLog extends BaseEntity {
    private Long memberId;
    private LocalDate logDate;
    private String mealType;
    private String foodName;
    private BigDecimal quantity;
    private BigDecimal calories;
    private String remark;
}
```

`health-system/src/main/java/com/health/system/domain/KnowledgeArticle.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_article")
public class KnowledgeArticle extends BaseEntity {
    private String title;
    private String content;
    private String category;
    private String author;
    private String coverImage;
    private Integer viewCount;
    private String status;
}
```

`health-system/src/main/java/com/health/system/domain/ExerciseLibrary.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exercise_library")
public class ExerciseLibrary extends BaseEntity {
    private String exerciseName;
    private String exerciseType;
    private String difficulty;
    private Integer duration;
    private BigDecimal caloriesBurn;
    private String description;
    private String videoUrl;
    private String status;
}
```

`health-system/src/main/java/com/health/system/domain/RecipeLibrary.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recipe_library")
public class RecipeLibrary extends BaseEntity {
    private String recipeName;
    private String mealType;
    private String suitableFor;
    private BigDecimal totalCalories;
    private String ingredients;
    private String steps;
    private String nutritionInfo;
    private String status;
}
```

`health-system/src/main/java/com/health/system/domain/DiseaseLibrary.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("disease_library")
public class DiseaseLibrary extends BaseEntity {
    private String diseaseName;
    private String category;
    private String symptoms;
    private String causes;
    private String treatment;
    private String prevention;
    private String status;
}
```

`health-system/src/main/java/com/health/system/domain/EducationContent.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("education_content")
public class EducationContent extends BaseEntity {
    private String title;
    private String content;
    private String contentType;
    private String targetAudience;
    private Long wordId;
    private String status;
}
```

`health-system/src/main/java/com/health/system/domain/AiConversation.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {
    private Long userId;
    private String title;
    private String lastMessage;
    private Integer messageCount;
}
```

`health-system/src/main/java/com/health/system/domain/AiMessage.java`:
```java
package com.health.system.domain;

import com.health.common.core.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_message")
public class AiMessage extends BaseEntity {
    private Long conversationId;
    private String role;
    private String content;
    private String model;
    private Integer tokens;
}
```

- [ ] **Step 4: 验证编译**

```bash
cd C:\Users\Dominion\health-management-system
mvn compile -pl health-common,health-system
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add health-system/
git commit -m "feat: 创建 health-system 模块，定义全部业务实体与系统管理实体"
```

---

### Task 4: 创建 health-system Mapper 接口

**Files:**
- Create: `health-system/src/main/java/com/health/system/mapper/SysUserMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/SysRoleMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/SysMenuMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/MemberMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/AppointmentMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/PackageInfoMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/ExamItemMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/AssessmentRecordMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/AssessmentIndicatorMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/TcmConstitutionMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/PsychologyAssessmentMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/InterventionPlanMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/ChronicDiseaseMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/KnowledgeArticleMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/ExerciseLibraryMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/RecipeLibraryMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/DiseaseLibraryMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/EducationContentMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/AiConversationMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/DietLogMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/ExamPlanMapper.java`
- Create: `health-system/src/main/java/com/health/system/mapper/CrowdProgramMapper.java`

**Interfaces:**
- Produces: 所有 Mapper 接口继承 MyBatis-Plus `BaseMapper<T>`，返回类型为对应的 Domain 实体

- [ ] **Step 1: 编写全部 Mapper 接口**

每个 Mapper 遵循统一模式：

```java
package com.health.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

需要创建以下 Mapper 文件（每个都遵循上述模式，替换泛型类型）：

| 文件名 | 泛型类型 |
|---|---|
| `SysUserMapper.java` | `SysUser` |
| `SysRoleMapper.java` | `SysRole` |
| `SysMenuMapper.java` | `SysMenu` |
| `MemberMapper.java` | `Member` |
| `ExamPlanMapper.java` | `ExamPlan` |
| `AppointmentMapper.java` | `Appointment` |
| `PackageInfoMapper.java` | `PackageInfo` |
| `ExamItemMapper.java` | `ExamItem` |
| `AssessmentRecordMapper.java` | `AssessmentRecord` |
| `AssessmentIndicatorMapper.java` | `AssessmentIndicator` |
| `TcmConstitutionMapper.java` | `TcmConstitution` |
| `PsychologyAssessmentMapper.java` | `PsychologyAssessment` |
| `InterventionPlanMapper.java` | `InterventionPlan` |
| `CrowdProgramMapper.java` | `CrowdProgram` |
| `ChronicDiseaseMapper.java` | `ChronicDisease` |
| `DietLogMapper.java` | `DietLog` |
| `KnowledgeArticleMapper.java` | `KnowledgeArticle` |
| `ExerciseLibraryMapper.java` | `ExerciseLibrary` |
| `RecipeLibraryMapper.java` | `RecipeLibrary` |
| `DiseaseLibraryMapper.java` | `DiseaseLibrary` |
| `EducationContentMapper.java` | `EducationContent` |
| `AiConversationMapper.java` | `AiConversation` |

- [ ] **Step 2: 编译验证**

```bash
cd C:\Users\Dominion\health-management-system
mvn compile -pl health-common,health-system
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add health-system/src/main/java/com/health/system/mapper/
git commit -m "feat: 创建全部 MyBatis-Plus Mapper 接口"
```

---

### Task 5: 创建 health-framework 模块

**Files:**
- Create: `health-framework/pom.xml`
- Create: `health-framework/src/main/java/com/health/framework/security/SaTokenConfig.java`
- Create: `health-framework/src/main/java/com/health/framework/security/StpInterfaceImpl.java`
- Create: `health-framework/src/main/java/com/health/framework/aspect/LogAspect.java`
- Create: `health-framework/src/main/java/com/health/framework/ai/AiConfig.java`
- Create: `health-framework/src/main/java/com/health/framework/ai/DeepSeekClient.java`
- Create: `health-framework/src/main/java/com/health/framework/config/FrameworkConfig.java`

**Interfaces:**
- Produces: `DeepSeekClient.chat(String message, List<Map> history)` 返回 `Flux<String>` (SSE 流式)
- Consumes: `health-system` (依赖其 Mapper 进行权限数据查询)

- [ ] **Step 1: 创建模块 POM**

`health-framework/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.health</groupId>
        <artifactId>health-management-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>health-framework</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.health</groupId>
            <artifactId>health-system</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-redis-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 Sa-Token 安全配置**

`health-framework/src/main/java/com/health/framework/security/SaTokenConfig.java`:
```java
package com.health.framework.security;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/captchaImage", "/swagger-ui/**", "/v3/api-docs/**", "/doc.html");
    }
}
```

`health-framework/src/main/java/com/health/framework/security/StpInterfaceImpl.java`:
```java
package com.health.framework.security;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.domain.SysRole;
import com.health.system.mapper.SysRoleMapper;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysRoleMapper roleMapper;

    public StpInterfaceImpl(SysRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<>());
        return roles.stream().map(SysRole::getRoleKey).toList();
    }
}
```

- [ ] **Step 3: 创建 AI 客户端**

`health-framework/src/main/java/com/health/framework/ai/AiConfig.java`:
```java
package com.health.framework.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {
    private String apiKey;
    private String model = "deepseek-chat";
    private String baseUrl = "https://api.deepseek.com/v1";
}
```

`health-framework/src/main/java/com/health/framework/ai/DeepSeekClient.java`:
```java
package com.health.framework.ai;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient {

    private final AiConfig aiConfig;
    private final WebClient webClient;

    public DeepSeekClient(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.webClient = WebClient.builder()
                .baseUrl(aiConfig.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .build();
    }

    public Flux<String> chat(String message, List<Map<String, String>> history) {
        List<Map<String, String>> messages = new java.util.ArrayList<>(history);
        messages.add(Map.of("role", "user", "content", message));

        Map<String, Object> body = Map.of(
                "model", aiConfig.getModel(),
                "messages", messages,
                "stream", true
        );

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(data -> !"[DONE]".equals(data.trim()))
                .map(this::extractContent);
    }

    private String extractContent(String raw) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(raw);
            return node.path("choices").get(0).path("delta").path("content").asText("");
        } catch (Exception e) {
            return "";
        }
    }
}
```

- [ ] **Step 4: 创建 AOP 日志切面**

`health-framework/src/main/java/com/health/framework/aspect/LogAspect.java`:
```java
package com.health.framework.aspect;

import com.health.common.annotation.Log;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogAspect {
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint point, Log logAnnotation) throws Throwable {
        log.info("[{}] 开始执行 - {}", logAnnotation.title(), point.getSignature().toShortString());
        long start = System.currentTimeMillis();
        try {
            Object result = point.proceed();
            log.info("[{}] 执行完成 - 耗时: {}ms", logAnnotation.title(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[{}] 执行异常 - {}", logAnnotation.title(), e.getMessage());
            throw e;
        }
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd C:\Users\Dominion\health-management-system
mvn compile -pl health-common,health-system,health-framework
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add health-framework/
git commit -m "feat: 创建 health-framework 模块（认证安全、AI客户端、日志切面）"
```

---

### Task 6: 创建 health-generator 和 health-quartz 骨架

**Files:**
- Create: `health-admin/pom.xml`
- Create: `health-admin/src/main/java/com/health/web/HealthApplication.java`
- Create: `health-admin/src/main/java/com/health/web/controller/system/SysUserController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/system/SysRoleController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/system/SysMenuController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/member/MemberController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/appointment/AppointmentController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/appointment/PackageController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/appointment/ExamItemController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/assessment/AssessmentController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/assessment/TcmController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/assessment/PsychologyController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/intervention/InterventionController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/intervention/DietLogController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/knowledge/KnowledgeController.java`
- Create: `health-admin/src/main/java/com/health/web/controller/ai/AiController.java`
- Create: `health-admin/src/main/resources/application.yml`

**Interfaces:**
- Consumes: `health-framework` 的所有能力
- Produces: REST API 端点，遵循 `/api/v1/<module>/**` 规范

- [ ] **Step 1: 创建模块 POM**

`health-admin/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.health</groupId>
        <artifactId>health-management-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>health-admin</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>com.health</groupId>
            <artifactId>health-framework</artifactId>
        </dependency>
        <dependency>
            <groupId>com.health</groupId>
            <artifactId>health-generator</artifactId>
        </dependency>
        <dependency>
            <groupId>com.health</groupId>
            <artifactId>health-quartz</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建启动类**

`health-admin/src/main/java/com/health/web/HealthApplication.java`:
```java
package com.health.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.health")
public class HealthApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

`health-admin/src/main/resources/application.yml`:
```yaml
server:
  port: 8080

spring:
  application:
    name: health-admin
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/health_management?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: ${DB_PASSWORD:root}
  data:
    redis:
      host: localhost
      port: 6379

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

sa-token:
  token-name: Authorization
  timeout: 2592000
  is-concurrent: true
  is-share: false
  token-style: tik

springdoc:
  swagger-ui:
    path: /swagger-ui.html

knife4j:
  enable: true
  setting:
    language: zh_cn

ai:
  api-key: ${AI_API_KEY:sk-your-key}
  model: deepseek-chat
  base-url: https://api.deepseek.com/v1
```

- [ ] **Step 4: 创建系统管理 Controller**

`health-admin/src/main/java/com/health/web/controller/system/SysUserController.java`:
```java
package com.health.web.controller.system;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.SysUser;
import com.health.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system/user")
public class SysUserController extends BaseController {

    private final SysUserMapper userMapper;

    public SysUserController(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    @Log(title = "用户查询")
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<SysUser> p = userMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getCreateTime)
        );
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        return success(userMapper.selectById(id));
    }

    @PostMapping
    @Log(title = "新增用户")
    public AjaxResult create(@RequestBody SysUser user) {
        userMapper.insert(user);
        return success(user);
    }

    @PutMapping
    @Log(title = "修改用户")
    public AjaxResult update(@RequestBody SysUser user) {
        userMapper.updateById(user);
        return success();
    }

    @DeleteMapping("/{ids}")
    @Log(title = "删除用户")
    public AjaxResult delete(@PathVariable List<Long> ids) {
        userMapper.deleteBatchIds(ids);
        return success();
    }
}
```

`health-admin/src/main/java/com/health/web/controller/system/SysRoleController.java`:
```java
package com.health.web.controller.system;

import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.SysRole;
import com.health.system.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/role")
public class SysRoleController extends BaseController {

    private final SysRoleMapper roleMapper;

    public SysRoleController(SysRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @GetMapping
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<SysRole> p = roleMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getRoleSort));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(roleMapper.selectById(id)); }

    @PostMapping
    public AjaxResult create(@RequestBody SysRole role) { roleMapper.insert(role); return success(role); }

    @PutMapping
    public AjaxResult update(@RequestBody SysRole role) { roleMapper.updateById(role); return success(); }

    @DeleteMapping("/{ids}")
    public AjaxResult delete(@PathVariable java.util.List<Long> ids) { roleMapper.deleteBatchIds(ids); return success(); }
}
```

`health-admin/src/main/java/com/health/web/controller/system/SysMenuController.java`:
```java
package com.health.web.controller.system;

import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.SysMenu;
import com.health.system.mapper.SysMenuMapper;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/system/menu")
public class SysMenuController extends BaseController {

    private final SysMenuMapper menuMapper;

    public SysMenuController(SysMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @GetMapping
    public AjaxResult list() {
        List<SysMenu> menus = menuMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getOrderNum));
        return success(menus);
    }

    @PostMapping
    public AjaxResult create(@RequestBody SysMenu menu) { menuMapper.insert(menu); return success(menu); }

    @PutMapping
    public AjaxResult update(@RequestBody SysMenu menu) { menuMapper.updateById(menu); return success(); }

    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) { menuMapper.deleteById(id); return success(); }
}
```

- [ ] **Step 5: 创建业务 Controller（会员管理）**

`health-admin/src/main/java/com/health/web/controller/member/MemberController.java`:
```java
package com.health.web.controller.member;

import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.Member;
import com.health.system.mapper.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/member")
public class MemberController extends BaseController {

    private final MemberMapper memberMapper;

    public MemberController(MemberMapper memberMapper) { this.memberMapper = memberMapper; }

    @GetMapping
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) String name) {
        LambdaQueryWrapper<Member> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) qw.like(Member::getName, name);
        qw.orderByDesc(Member::getCreateTime);
        Page<Member> p = memberMapper.selectPage(new Page<>(page, pageSize), qw);
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) { return success(memberMapper.selectById(id)); }

    @PostMapping
    public AjaxResult create(@RequestBody Member member) { memberMapper.insert(member); return success(member); }

    @PutMapping
    public AjaxResult update(@RequestBody Member member) { memberMapper.updateById(member); return success(); }

    @DeleteMapping("/{ids}")
    public AjaxResult delete(@PathVariable java.util.List<Long> ids) { memberMapper.deleteBatchIds(ids); return success(); }
}
```

- [ ] **Step 6: 创建预约管理 Controller**

`health-admin/src/main/java/com/health/web/controller/appointment/AppointmentController.java`:
```java
package com.health.web.controller.appointment;

import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.system.domain.Appointment;
import com.health.system.mapper.AppointmentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/appointment")
public class AppointmentController extends BaseController {

    private final AppointmentMapper appointmentMapper;

    public AppointmentController(AppointmentMapper appointmentMapper) { this.appointmentMapper = appointmentMapper; }

    @GetMapping
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(Appointment::getStatus, status);
        qw.orderByDesc(Appointment::getAppointmentDate);
        Page<Appointment> p = appointmentMapper.selectPage(new Page<>(page, pageSize), qw);
        return toPage(p.getTotal(), p.getRecords());
    }

    @PostMapping
    public AjaxResult create(@RequestBody Appointment appointment) {
        appointment.setStatus("PENDING");
        appointmentMapper.insert(appointment);
        return success(appointment);
    }

    @PutMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id) {
        Appointment app = appointmentMapper.selectById(id);
        if (app != null) { app.setStatus("CANCELLED"); appointmentMapper.updateById(app); }
        return success();
    }

    @PutMapping
    public AjaxResult update(@RequestBody Appointment appointment) { appointmentMapper.updateById(appointment); return success(); }

    @DeleteMapping("/{ids}")
    public AjaxResult delete(@PathVariable java.util.List<Long> ids) { appointmentMapper.deleteBatchIds(ids); return success(); }
}
```

- [ ] **Step 7: 创建剩余业务 Controller（遵循统一 CRUD 模式）**

以下 Controller 均继承 `BaseController`，注入对应 `*Mapper`，提供 `GET list` / `GET {id}` / `POST` / `PUT` / `DELETE` 五项接口，模式与 `MemberController` 完全一致：

| Controller 文件 | Mapper 注入 | 路径前缀 |
|---|---|---|
| `controller/assessment/AssessmentController.java` | `AssessmentRecordMapper` | `/api/v1/assessment/record` |
| `controller/assessment/IndicatorController.java` | `AssessmentIndicatorMapper` | `/api/v1/assessment/indicator` |
| `controller/assessment/TcmController.java` | `TcmConstitutionMapper` | `/api/v1/assessment/tcm` |
| `controller/assessment/PsychologyController.java` | `PsychologyAssessmentMapper` | `/api/v1/assessment/psychology` |
| `controller/intervention/InterventionController.java` | `InterventionPlanMapper` | `/api/v1/intervention/plan` |
| `controller/intervention/CrowdProgramController.java` | `CrowdProgramMapper` | `/api/v1/intervention/crowd` |
| `controller/intervention/ChronicDiseaseController.java` | `ChronicDiseaseMapper` | `/api/v1/intervention/chronic` |
| `controller/intervention/DietLogController.java` | `DietLogMapper` | `/api/v1/intervention/diet` |
| `controller/knowledge/KnowledgeController.java` | `KnowledgeArticleMapper` | `/api/v1/knowledge/article` |
| `controller/knowledge/ExerciseController.java` | `ExerciseLibraryMapper` | `/api/v1/knowledge/exercise` |
| `controller/knowledge/RecipeController.java` | `RecipeLibraryMapper` | `/api/v1/knowledge/recipe` |
| `controller/knowledge/DiseaseController.java` | `DiseaseLibraryMapper` | `/api/v1/knowledge/disease` |
| `controller/knowledge/EducationController.java` | `EducationContentMapper` | `/api/v1/knowledge/education` |
| `controller/appointment/PackageController.java` | `PackageInfoMapper` | `/api/v1/appointment/package` |
| `controller/appointment/ExamItemController.java` | `ExamItemMapper` | `/api/v1/appointment/exam-item` |
| `controller/member/ExamPlanController.java` | `ExamPlanMapper` | `/api/v1/member/exam-plan` |

每个 Controller 文件约 30 行，结构与 `MemberController.java` 相同，只替换类名、Mapper 类型、路径即可。

`health-admin/src/main/java/com/health/web/controller/ai/AiController.java`:
```java
package com.health.web.controller.ai;

import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.framework.ai.DeepSeekClient;
import com.health.system.domain.AiConversation;
import com.health.system.domain.AiMessage;
import com.health.system.mapper.AiConversationMapper;
import com.health.system.mapper.AiMessageMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController extends BaseController {

    private final DeepSeekClient deepSeekClient;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;

    public AiController(DeepSeekClient deepSeekClient,
                        AiConversationMapper conversationMapper,
                        AiMessageMapper messageMapper) {
        this.deepSeekClient = deepSeekClient;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @GetMapping("/conversations")
    public AjaxResult conversations(Long userId) {
        return success(conversationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getCreateTime)));
    }

    @PostMapping("/conversations")
    public AjaxResult createConversation(@RequestBody AiConversation conv) {
        conv.setMessageCount(0);
        conversationMapper.insert(conv);
        return success(conv);
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String message,
                              @RequestParam Long conversationId,
                              @RequestParam(defaultValue = "false") boolean roast) {
        List<AiMessage> history = messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreateTime));

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        if (roast) {
            messages.add(Map.of("role", "system",
                    "content", "你是一个毒舌健康管家。请用犀利、幽默、一针见血的方式点评用户的健康数据和生活习惯。用词要狠，但出发点是关心。像损友一样毫不留情地指出问题，同时给出实际可行的改善建议。"));
        } else {
            messages.add(Map.of("role", "system",
                    "content", "你是一个专业的健康管理顾问，请根据用户的健康数据提供科学、专业的建议。"));
        }
        history.forEach(h -> messages.add(Map.of("role", h.getRole(), "content", h.getContent())));

        return deepSeekClient.chat(message, messages);
    }

    @DeleteMapping("/conversations/{id}")
    public AjaxResult deleteConversation(@PathVariable Long id) {
        messageMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, id));
        conversationMapper.deleteById(id);
        return success();
    }
}
```

- [ ] **Step 9: 编译验证**

```bash
cd C:\Users\Dominion\health-management-system
mvn compile
```
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add health-admin/
git commit -m "feat: 创建 health-admin 主应用（启动类、系统管理、业务、AI 控制器）"
```

---

### Task 7: 创建 health-admin 主应用

**Files:**
- Create: `health-generator/pom.xml`
- Create: `health-generator/src/main/java/com/health/generator/controller/GenController.java`
- Create: `health-quartz/pom.xml`
- Create: `health-quartz/src/main/java/com/health/quartz/controller/SysJobController.java`

- [ ] **Step 1: health-generator POM**

`health-generator/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.health</groupId>
        <artifactId>health-management-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>health-generator</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.health</groupId>
            <artifactId>health-common</artifactId>
        </dependency>
    </dependencies>
</project>
```

`health-generator/src/main/java/com/health/generator/controller/GenController.java`:
```java
package com.health.generator.controller;

import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/generator")
public class GenController extends BaseController {

    @GetMapping("/tables")
    public AjaxResult tables() {
        return success("代码生成器就绪 - 查询数据库表列表");
    }

    @PostMapping("/generate")
    public AjaxResult generate(@RequestParam String tableName) {
        return success("开始生成 " + tableName + " 的代码");
    }
}
```

- [ ] **Step 2: health-quartz POM + Controller**

`health-quartz/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.health</groupId>
        <artifactId>health-management-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>health-quartz</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.health</groupId>
            <artifactId>health-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </dependency>
    </dependencies>
</project>
```

`health-quartz/src/main/java/com/health/quartz/controller/SysJobController.java`:
```java
package com.health.quartz.controller;

import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/monitor/job")
public class SysJobController extends BaseController {

    @GetMapping
    public AjaxResult list() {
        return success("定时任务列表就绪");
    }

    @PostMapping
    public AjaxResult add(@RequestBody Object job) {
        return success("任务已添加");
    }

    @PutMapping("/{id}/pause")
    public AjaxResult pause(@PathVariable Long id) { return success("任务已暂停"); }

    @PutMapping("/{id}/resume")
    public AjaxResult resume(@PathVariable Long id) { return success("任务已恢复"); }
}
```

- [ ] **Step 3: 完整编译**

```bash
cd C:\Users\Dominion\health-management-system
mvn compile
```
Expected: BUILD SUCCESS - all 6 modules

- [ ] **Step 4: Commit**

```bash
git add health-generator/ health-quartz/
git commit -m "feat: 创建 health-generator 和 health-quartz 骨架模块"
```

---

### Task 8: 创建数据库初始化脚本

**Files:**
- Create: `sql/init.sql`

- [ ] **Step 1: 编写完整建表脚本**

`sql/init.sql`:
```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS health_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE health_management;

-- ==============================
-- 系统管理表
-- ==============================
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(64) NOT NULL UNIQUE,
    nick_name VARCHAR(64),
    password VARCHAR(128) NOT NULL,
    email VARCHAR(128),
    phone_number VARCHAR(20),
    sex CHAR(1) DEFAULT '0',
    avatar VARCHAR(256),
    status CHAR(1) DEFAULT '0',
    dept_id BIGINT,
    login_ip VARCHAR(128),
    login_date DATETIME,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(64) NOT NULL,
    role_key VARCHAR(64) NOT NULL UNIQUE,
    role_sort INT DEFAULT 0,
    data_scope CHAR(1) DEFAULT '1',
    status CHAR(1) DEFAULT '0',
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_name VARCHAR(64) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    order_num INT DEFAULT 0,
    path VARCHAR(256),
    component VARCHAR(256),
    query VARCHAR(256),
    perms VARCHAR(128),
    icon VARCHAR(64),
    menu_type CHAR(1) DEFAULT '',
    visible CHAR(1) DEFAULT '0',
    status CHAR(1) DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='菜单表';

CREATE TABLE sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_name VARCHAR(64) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    order_num INT DEFAULT 0,
    leader VARCHAR(64),
    phone VARCHAR(20),
    email VARCHAR(128),
    status CHAR(1) DEFAULT '0',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='部门表';

-- ==============================
-- 会员管理
-- ==============================
CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    gender CHAR(1),
    birthday DATE,
    phone VARCHAR(20),
    id_card VARCHAR(20),
    address VARCHAR(256),
    blood_type VARCHAR(8),
    height DECIMAL(5,2),
    weight DECIMAL(5,2),
    allergy_history TEXT,
    family_history TEXT,
    smoking_status VARCHAR(32),
    drinking_status VARCHAR(32),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='会员表';

CREATE TABLE exam_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    plan_name VARCHAR(128),
    plan_date DATE,
    package_id BIGINT,
    status VARCHAR(32) DEFAULT 'PENDING',
    report_path VARCHAR(256),
    conclusion TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='体检计划表';

-- ==============================
-- 预约管理
-- ==============================
CREATE TABLE appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    package_id BIGINT,
    appointment_date DATE NOT NULL,
    appointment_time TIME,
    status VARCHAR(32) DEFAULT 'PENDING',
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='预约表';

CREATE TABLE package_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_name VARCHAR(128) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    suitable_for VARCHAR(256),
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='套餐表';

CREATE TABLE package_item_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='套餐项目明细表';

CREATE TABLE exam_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(128) NOT NULL,
    item_code VARCHAR(64),
    unit VARCHAR(32),
    price DECIMAL(10,2),
    reference_range VARCHAR(256),
    category_id BIGINT,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='检测项表';

CREATE TABLE exam_item_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='检测项目组表';

-- ==============================
-- 健康评估
-- ==============================
CREATE TABLE assessment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    template_id BIGINT,
    total_score DECIMAL(8,2),
    risk_level VARCHAR(32),
    conclusion TEXT,
    suggestion TEXT,
    assessor_id BIGINT,
    assess_date DATE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='评估记录表';

CREATE TABLE assessment_indicator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_name VARCHAR(128) NOT NULL,
    indicator_type VARCHAR(64),
    unit VARCHAR(32),
    min_value DECIMAL(10,4),
    max_value DECIMAL(10,4),
    risk_level VARCHAR(32),
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='评估指标表';

CREATE TABLE tcm_constitution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    constitution_type VARCHAR(64),
    score INT,
    description TEXT,
    health_advice TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='中医体质辨识表';

CREATE TABLE psychology_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    assessment_type VARCHAR(64),
    total_score INT,
    result_level VARCHAR(64),
    analysis TEXT,
    suggestion TEXT,
    assess_date DATE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='心理评测表';

-- ==============================
-- 健康干预
-- ==============================
CREATE TABLE intervention_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    plan_name VARCHAR(128),
    plan_type VARCHAR(64),
    target_goal TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    creator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='干预方案表';

CREATE TABLE crowd_program (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    program_name VARCHAR(128) NOT NULL,
    target_crowd VARCHAR(256),
    program_content TEXT,
    frequency VARCHAR(128),
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='人群方案表';

CREATE TABLE chronic_disease (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    disease_name VARCHAR(128) NOT NULL,
    diagnosis_date VARCHAR(32),
    severity VARCHAR(32),
    medication TEXT,
    control_status VARCHAR(64),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='慢病管理表';

CREATE TABLE diet_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    log_date DATE NOT NULL,
    meal_type VARCHAR(32),
    food_name VARCHAR(128),
    quantity DECIMAL(10,3),
    calories DECIMAL(10,2),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='膳食日志表';

-- ==============================
-- 知识库
-- ==============================
CREATE TABLE knowledge_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    content LONGTEXT,
    category VARCHAR(64),
    author VARCHAR(64),
    cover_image VARCHAR(256),
    view_count INT DEFAULT 0,
    status VARCHAR(32) DEFAULT 'PUBLISHED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='科普文章表';

CREATE TABLE exercise_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exercise_name VARCHAR(128) NOT NULL,
    exercise_type VARCHAR(64),
    difficulty VARCHAR(32),
    duration INT,
    calories_burn DECIMAL(8,2),
    description TEXT,
    video_url VARCHAR(256),
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='运动项目库';

CREATE TABLE recipe_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_name VARCHAR(128) NOT NULL,
    meal_type VARCHAR(32),
    suitable_for VARCHAR(256),
    total_calories DECIMAL(8,2),
    ingredients TEXT,
    steps TEXT,
    nutrition_info TEXT,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='食谱库';

CREATE TABLE disease_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    disease_name VARCHAR(128) NOT NULL,
    category VARCHAR(64),
    symptoms TEXT,
    causes TEXT,
    treatment TEXT,
    prevention TEXT,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='疾病库';

CREATE TABLE education_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    content LONGTEXT,
    content_type VARCHAR(32),
    target_audience VARCHAR(128),
    word_id BIGINT,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='宣教内容表';

CREATE TABLE education_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word_name VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='宣教词管理表';

-- ==============================
-- AI对话
-- ==============================
CREATE TABLE ai_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(256) DEFAULT '新的对话',
    last_message TEXT,
    message_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='AI对话会话表';

CREATE TABLE ai_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT,
    model VARCHAR(64) DEFAULT 'deepseek-chat',
    tokens INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='AI消息表';

-- ==============================
-- 初始化数据
-- ==============================
INSERT INTO sys_user (user_name, nick_name, password, email, status) VALUES
('admin', '系统管理员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'admin@health.com', '0');

INSERT INTO sys_dept (dept_name, parent_id, order_num, status) VALUES
('总部', 0, 0, '0'),
('内科', 0, 1, '0'),
('外科', 0, 2, '0'),
('体检中心', 0, 3, '0');

INSERT INTO sys_role (role_name, role_key, role_sort, status) VALUES
('超级管理员', 'admin', 1, '0'),
('医生', 'doctor', 2, '0'),
('护士', 'nurse', 3, '0'),
('会员', 'member', 4, '0');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, icon, menu_type, visible, status) VALUES
('系统管理', 0, 1, '/system', '', 'system', 'M', '0', '0'),
('用户管理', 1, 1, '/system/user', 'system/user/index', 'user', 'C', '0', '0'),
('角色管理', 1, 2, '/system/role', 'system/role/index', 'peoples', 'C', '0', '0'),
('菜单管理', 1, 3, '/system/menu', 'system/menu/index', 'tree-table', 'C', '0', '0'),
('会员管理', 0, 2, '/member', 'member/index', 'people', 'C', '0', '0'),
('预约管理', 0, 3, '/appointment', 'appointment/index', 'date', 'C', '0', '0'),
('健康评估', 0, 4, '/assessment', 'assessment/index', 'chart', 'C', '0', '0'),
('健康干预', 0, 5, '/intervention', 'intervention/index', 'guide', 'C', '0', '0'),
('知识库', 0, 6, '/knowledge', 'knowledge/index', 'education', 'C', '0', '0'),
('AI助手', 0, 7, '/ai-agent', 'ai/index', 'cpu', 'C', '0', '0');
```

- [ ] **Step 2: 验证 SQL 语法**

```bash
# 可选：连接 MySQL 验证
# mysql -u root -p < sql/init.sql
```

- [ ] **Step 3: Commit**

```bash
git add sql/init.sql
git commit -m "feat: 创建完整数据库初始化脚本（26张表 + 初始数据）"
```

---

### Task 9: 创建 Docker Compose 配置

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `Dockerfile`

- [ ] **Step 1: docker-compose.yml**

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: health-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:-root}
      MYSQL_DATABASE: health_management
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: health-redis
    command: redis-server --appendonly yes
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      retries: 5

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: health-backend
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_PASSWORD: ${DB_PASSWORD:-root}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      AI_API_KEY: ${AI_API_KEY}
    ports:
      - "8080:8080"

  frontend:
    build:
      context: ./health-web
    container_name: health-frontend
    depends_on:
      - backend
    ports:
      - "80:80"

volumes:
  mysql_data:
  redis_data:
```

- [ ] **Step 2: .env.example**

```
DB_PASSWORD=root
AI_API_KEY=sk-your-deepseek-api-key
JWT_SECRET=change-me-in-production
```

- [ ] **Step 3: Dockerfile**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY health-common health-common
COPY health-system health-system
COPY health-framework health-framework
COPY health-generator health-generator
COPY health-quartz health-quartz
COPY health-admin health-admin
RUN mvn clean package -pl health-admin -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/health-admin/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml .env.example Dockerfile
git commit -m "feat: 创建 Docker Compose 部署配置"
```

---

### Task 10: 补充缺失的 Mapper 和 Service

**Files:**
- Create: `health-system/src/main/java/com/health/system/mapper/AiMessageMapper.java`
- Create: `health-system/src/main/java/com/health/system/service/IMemberService.java`
- Create: `health-system/src/main/java/com/health/system/service/impl/MemberServiceImpl.java`
- Create: `health-system/src/main/java/com/health/system/service/IAppointmentService.java`
- Create: `health-system/src/main/java/com/health/system/service/impl/AppointmentServiceImpl.java`
- Create: `health-system/src/main/java/com/health/system/service/IAssessmentService.java`
- Create: `health-system/src/main/java/com/health/system/service/impl/AssessmentServiceImpl.java`

- [ ] **Step 1: 补充 AiMessageMapper**

`health-system/src/main/java/com/health/system/mapper/AiMessageMapper.java`:
```java
package com.health.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.system.domain.AiMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}
```

- [ ] **Step 2: 编写 IMemberService 和实现**

`health-system/src/main/java/com/health/system/service/IMemberService.java`:
```java
package com.health.system.service;

import com.health.system.domain.Member;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IMemberService extends IService<Member> {
    Member getWithExamPlans(Long memberId);
}

// health-system/src/main/java/com/health/system/service/impl/MemberServiceImpl.java
package com.health.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.health.system.domain.Member;
import com.health.system.domain.ExamPlan;
import com.health.system.mapper.ExamPlanMapper;
import com.health.system.mapper.MemberMapper;
import com.health.system.service.IMemberService;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements IMemberService {

    private final ExamPlanMapper examPlanMapper;

    public MemberServiceImpl(ExamPlanMapper examPlanMapper) {
        this.examPlanMapper = examPlanMapper;
    }

    @Override
    public Member getWithExamPlans(Long memberId) {
        return baseMapper.selectById(memberId);
    }
}
```

- [ ] **Step 3: 编译 + Commit**

```bash
cd C:\Users\Dominion\health-management-system
mvn compile
git add health-system/src/main/java/com/health/system/mapper/AiMessageMapper.java health-system/src/main/java/com/health/system/service/
git commit -m "feat: 补充 AiMessageMapper 及业务 Service 接口与实现"
```

---

### Task 11: IDEA 配置支持

**Files:**
- Create: `.idea/runConfigurations/HealthApplication.run.xml`
- Create: `.gitignore`

- [ ] **Step 1: IDEA 启动配置**

`.idea/runConfigurations/HealthApplication.run.xml`:
```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="HealthApplication" type="SpringBootApplicationConfigurationType" factoryName="Spring Boot">
    <module name="health-admin" />
    <option name="SPRING_BOOT_MAIN_CLASS" value="com.health.web.HealthApplication" />
    <option name="VM_PARAMETERS" value="-DDB_PASSWORD=root" />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
```

- [ ] **Step 2: 更新 .gitignore**

确保 `.gitignore` 包含：
```
# Maven
target/
*.jar

# IDEA
.idea/workspace.xml
.idea/tasks.xml
.idea/usage.statistics.xml
.idea/dictionaries/
.idea/shelf/

# Environment
.env
*.log

# Node
node_modules/
dist/
```

- [ ] **Step 3: Commit**

```bash
git add .idea/runConfigurations/ .gitignore
git commit -m "chore: 添加 IDEA 启动配置和 gitignore"
```

---

### Task 12: IDEA 验证与编译

- [ ] **Step 1: 完整编译检查**

```bash
cd C:\Users\Dominion\health-management-system
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 打包检查**

```bash
mvn package -DskipTests
```
Expected: BUILD SUCCESS, health-admin/target/*.jar 存在

- [ ] **Step 3: 确认 IDEA 可识别项目**

```
1. File → Open → 选择 C:\Users\Dominion\health-management-system\pom.xml
2. 确认 6 个模块在 Project 视图中显示为模块
3. 确认 Maven 面板显示全部子模块
4. 运行 HealthApplication 启动配置，检查启动日志无错误
```

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "chore: 最终编译验证通过，IDEA 可开箱即用"
```

---

## 验证清单

- [ ] `mvn clean compile` — 全部模块编译通过
- [ ] `mvn package -DskipTests` — 成功打包 health-admin JAR
- [ ] IDEA File → Open 根 pom.xml — 识别 6 个子模块
- [ ] Docker `docker-compose up -d mysql redis` — MySQL + Redis 启动
- [ ] `curl http://localhost:8080/api/v1/system/user` — API 可访问
- [ ] `curl http://localhost:8080/api/v1/ai/chat?message=hello&conversationId=1` — AI SSE 流式返回
