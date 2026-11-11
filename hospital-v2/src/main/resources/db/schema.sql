-- =====================================================================
--  hospital-v2 数据库 Schema（MySQL 8 / H2-MySQL 兼容）
--  共 23 张表，对应课程 7 层架构：接入 / RAG / 工具中心 /
--  多步 ReAct 分诊 / 二次核验落库 / 分诊命中率 / 存储与观测底座
-- =====================================================================

-- 1. 系统用户（管理员 / 医生 / 患者）
CREATE TABLE user_account (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(64)  NOT NULL,
    password     VARCHAR(128) NOT NULL,
    real_name    VARCHAR(32),
    role         VARCHAR(16)  NOT NULL COMMENT 'admin|doctor|patient',
    phone        VARCHAR(20),
    id_card      VARCHAR(32),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 科室
CREATE TABLE department (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(64)  NOT NULL,
    code         VARCHAR(32),
    description  VARCHAR(255),
    icon         VARCHAR(64),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 医生
CREATE TABLE doctor (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(32)  NOT NULL,
    department_id BIGINT        NOT NULL,
    title         VARCHAR(32)  COMMENT '主任医师/副主任医师/主治医师',
    specialty     VARCHAR(64),
    good_at       VARCHAR(255),
    user_id       BIGINT        COMMENT '开通的医生登录账号（user_account.id）',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_doctor_dept (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 医生排班（含每日号源，挂号乐观锁扣减）
CREATE TABLE doctor_schedule (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    doctor_id    BIGINT       NOT NULL,
    department_id BIGINT      NOT NULL,
    work_date    DATE         NOT NULL,
    period       VARCHAR(8)   NOT NULL COMMENT 'AM|PM',
    total_num    INT          NOT NULL DEFAULT 20,
    remain_num   INT          NOT NULL DEFAULT 20,
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1开放 0停诊',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sched (doctor_id, work_date, period),
    KEY idx_sched_date (work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 患者档案
CREATE TABLE patient_profile (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT,
    name            VARCHAR(32),
    gender          VARCHAR(4),
    age             INT,
    id_card         VARCHAR(32),
    allergic_history VARCHAR(255),
    past_history    VARCHAR(255),
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 症状标签
CREATE TABLE symptom_tag (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(64)  NOT NULL,
    department_id BIGINT,
    description   VARCHAR(255),
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 危急征象规则（命中即硬拦截转急诊）
CREATE TABLE red_flag_rule (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(64)  NOT NULL,
    keywords   VARCHAR(255) NOT NULL COMMENT '逗号分隔关键词',
    level      TINYINT      NOT NULL DEFAULT 1 COMMENT '1紧急 2高危 3注意',
    advice     VARCHAR(255),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 知识库（分类）
CREATE TABLE knowledge_base (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(64)  NOT NULL,
    description  VARCHAR(255),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 知识文档
CREATE TABLE knowledge_doc (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    base_id     BIGINT      NOT NULL,
    title       VARCHAR(128) NOT NULL,
    source_type VARCHAR(16)  COMMENT 'upload|text',
    status      VARCHAR(16) NOT NULL DEFAULT 'READY',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 知识片段（向量存 TEXT 列，JSON 数组）
CREATE TABLE knowledge_segment (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    doc_id         BIGINT      NOT NULL,
    base_id         BIGINT      NOT NULL,
    content        TEXT         NOT NULL,
    embedding      TEXT         COMMENT 'JSON 数组，维度=embedding-dim',
    vector_status  VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|READY',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_seg_base (base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. MCP 工具注册表（数据库驱动真开关）
CREATE TABLE mcp_tool (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tool_code    VARCHAR(64)  NOT NULL COMMENT '模型看到的工具名',
    name         VARCHAR(64)  NOT NULL,
    description  VARCHAR(255) NOT NULL,
    param_schema TEXT         COMMENT 'JSON 数组：[{name,type,required,desc}]',
    enabled      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用（关了模型真调不到）',
    sort         INT          NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tool_code (tool_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. 工具调用日志（成功/失败都记，区分 Agent 运行与人工测试）
CREATE TABLE mcp_tool_call_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    run_id      BIGINT,
    step_id     BIGINT,
    tool_code   VARCHAR(64)  NOT NULL,
    source      VARCHAR(16)  NOT NULL COMMENT 'agent|manual',
    input_json  TEXT,
    output_json TEXT,
    status      VARCHAR(16) NOT NULL COMMENT 'SUCCESS|FAILED',
    cost_ms     INT,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_log_run (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. Agent 运行记录（一次分诊 = 一个 run）
CREATE TABLE agent_run (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    session_id       BIGINT,
    patient_id        BIGINT,
    status           VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING|DONE|FAILED',
    final_department_id BIGINT,
    final_doctor_id  BIGINT,
    red_flag         TINYINT     NOT NULL DEFAULT 0,
    steps            INT         NOT NULL DEFAULT 0,
    cost_ms          INT,
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. Agent 步骤（逐轮：思考 / 调哪个工具 / 入参 / 返回 / 耗时，可回放）
CREATE TABLE agent_step (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    run_id       BIGINT       NOT NULL,
    step_index   INT          NOT NULL,
    thought      TEXT,
    tool_code    VARCHAR(64),
    input_json   TEXT,
    output_json  TEXT,
    cost_ms      INT,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_step_run (run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. 预问诊会话（多轮 SSE，分诊后锁定）
CREATE TABLE pre_triage_session (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    patient_id  BIGINT,
    status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE|LOCKED|DONE',
    locked      TINYINT     NOT NULL DEFAULT 0,
    summary     VARCHAR(512),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. 预问诊消息
CREATE TABLE pre_triage_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    session_id  BIGINT       NOT NULL,
    role        VARCHAR(16) NOT NULL COMMENT 'user|assistant',
    content     TEXT         NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_msg_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 17. 分诊建议（AI 输出经代码二次核验后落库）
CREATE TABLE triage_suggestion (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    run_id          BIGINT       NOT NULL,
    session_id      BIGINT,
    patient_id      BIGINT,
    department_id   BIGINT,
    doctor_id       BIGINT,
    confidence      DECIMAL(5,4) NOT NULL DEFAULT 0,
    red_flag        TINYINT      NOT NULL DEFAULT 0,
    reason          VARCHAR(512),
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|CONFIRMED',
    registered      TINYINT     NOT NULL DEFAULT 0 COMMENT '是否被拿去挂号（命中率统计用）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sug_dept (department_id),
    KEY idx_sug_reg (registered)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 18. 挂号（乐观锁扣减号源）
CREATE TABLE registration (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    suggestion_id  BIGINT,
    patient_id     BIGINT       NOT NULL,
    department_id  BIGINT       NOT NULL,
    doctor_id      BIGINT,
    schedule_id    BIGINT       NOT NULL,
    work_date      DATE         NOT NULL,
    period         VARCHAR(8)   NOT NULL,
    status         VARCHAR(16)  NOT NULL DEFAULT 'PAID' COMMENT 'PAID|DONE|CANCEL',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reg_patient (patient_id, work_date, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 19. 病历
CREATE TABLE medical_record (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    registration_id   BIGINT       NOT NULL,
    patient_id        BIGINT       NOT NULL,
    doctor_id         BIGINT       NOT NULL,
    chief_complaint   VARCHAR(512)  COMMENT '① 主诉',
    present_illness   TEXT          COMMENT '② 现病史',
    past_history      TEXT          COMMENT '③ 既往史',
    allergy_history   VARCHAR(512)  COMMENT '④ 过敏史',
    assistant_exam    TEXT          COMMENT '⑤ 辅助检查',
    diagnosis         VARCHAR(512)  COMMENT '⑥ 诊断',
    treatment         TEXT          COMMENT '⑦ 处理意见',
    status            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT 草稿可编辑|SUBMITTED 已提交锁定',
    create_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 20. AI 调用观测（每次对话/Embedding 调用都记）
CREATE TABLE ai_usage_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    use_case       VARCHAR(32)  NOT NULL COMMENT 'pretriage|triage|embedding|reason',
    model          VARCHAR(64),
    prompt_tokens  INT,
    completion_tokens INT,
    total_tokens   INT,
    cost_ms        INT,
    success        TINYINT      NOT NULL DEFAULT 1,
    error_msg      VARCHAR(1000),
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_usage_case (use_case)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 21. 症状→科室映射（RAG 兜底 + 二次核验参考）
CREATE TABLE symptom_department_map (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    symptom_keyword VARCHAR(64) NOT NULL,
    department_id   BIGINT      NOT NULL,
    weight          INT         NOT NULL DEFAULT 1,
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_symp (symptom_keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 22. AI 模型配置（管理端可配）
CREATE TABLE ai_model_config (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(64)  NOT NULL,
    provider   VARCHAR(32),
    base_url   VARCHAR(255),
    model      VARCHAR(64)  NOT NULL,
    api_key    VARCHAR(255)  COMMENT '不回显全文',
    purpose    VARCHAR(32)   COMMENT 'CHAT 对话|EMBEDDING 向量',
    prompt_template TEXT     COMMENT 'Prompt 模板（分诊/预问诊用）',
    enabled    TINYINT      NOT NULL DEFAULT 1,
    sort       INT          NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 23. 操作审计
CREATE TABLE operation_audit (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    operator   VARCHAR(32),
    action     VARCHAR(64)  NOT NULL,
    target     VARCHAR(64),
    detail     TEXT,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
