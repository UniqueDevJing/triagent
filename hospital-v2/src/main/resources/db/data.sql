-- =====================================================================
--  hospital-v2 种子数据
--  说明：knowledge_segment.embedding 留空，应用启动时由 EmbeddingGateway 补齐
-- =====================================================================

-- 管理员 / 患者
INSERT INTO user_account (id, username, real_name, role, phone) VALUES
    (1, 'admin',  '系统管理员', 'admin',  '13800000000'),
    (2, 'patient','张小明',     'patient','13900000000');

-- 2. 科室
INSERT INTO department (id, name, code, description, icon) VALUES
    (1, '呼吸内科',   'resp', '咳嗽、哮喘、肺部感染', 'lungs'),
    (2, '心血管内科', 'card', '胸闷、心悸、高血压',   'heart'),
    (3, '消化内科',   'gastro','腹痛、腹泻、反酸',     'stomach'),
    (4, '神经内科',   'neuro','头痛、眩晕、肢体麻木', 'brain'),
    (5, '急诊科',     'emerg','危急重症绿色通道',     'cross'),
    (6, '骨科',       'ortho','骨折、关节痛、扭伤',   'bone'),
    (7, '儿科',       'pedia','儿童发热、咳嗽',       'child');

-- 3. 医生
INSERT INTO doctor (id, name, department_id, title, specialty, good_at) VALUES
    (1, '李建国', 1, '主任医师',     '慢性阻塞性肺病', '哮喘长期管理、肺部感染诊治'),
    (2, '王慧',   1, '副主任医师',   '支气管哮喘',     '咳嗽变异性哮喘、过敏性咳嗽'),
    (3, '陈志远', 2, '主任医师',     '冠心病',         '胸闷胸痛鉴别、PCI 术后管理'),
    (4, '赵敏',   2, '主治医师',     '高血压',         '难治性高血压、心律管理'),
    (5, '刘洋',   3, '副主任医师',   '胃肠疾病',       '幽门螺杆菌、炎症性肠病'),
    (6, '孙磊',   4, '主任医师',     '脑血管病',       '头痛眩晕、脑卒中二级预防'),
    (7, '周倩',   5, '主治医师',     '急危重症',       '胸痛中心绿色通道、休克抢救'),
    (8, '吴军',   6, '副主任医师',   '运动损伤',       '关节镜、骨折内固定'),
    (9, '郑甜',   7, '主治医师',     '小儿呼吸',       '儿童反复呼吸道感染');

-- 4. 排班（2026-11-12 ~ 11-14，AM/PM，号源充足）
INSERT INTO doctor_schedule (doctor_id, department_id, work_date, period, total_num, remain_num, status) VALUES
    (1,1,'2026-11-12','AM',20,20,1),(1,1,'2026-11-12','PM',20,20,1),
    (2,1,'2026-11-12','AM',20,20,1),(2,1,'2026-11-13','PM',20,20,1),
    (3,2,'2026-11-12','AM',20,20,1),(3,2,'2026-11-13','AM',20,20,1),
    (4,2,'2026-11-12','PM',20,20,1),(4,2,'2026-11-14','AM',20,20,1),
    (5,3,'2026-11-12','AM',20,20,1),(5,3,'2026-11-13','PM',20,20,1),
    (6,4,'2026-11-12','PM',20,20,1),(6,4,'2026-11-14','AM',20,20,1),
    (7,5,'2026-11-12','AM',30,30,1),(7,5,'2026-11-12','PM',30,30,1),
    (8,6,'2026-11-13','AM',20,20,1),(8,6,'2026-11-13','PM',20,20,1),
    (9,7,'2026-11-12','AM',20,20,1),(9,7,'2026-11-14','PM',20,20,1);

-- 5. 患者档案
INSERT INTO patient_profile (id, user_id, name, gender, age, allergic_history, past_history) VALUES
    (1, 2, '张小明', '男', 34, '青霉素过敏', '既往体健'),
    (2, NULL, '匿名患者', '女', 56, '', '高血压病史 5 年');

-- 6. 症状标签
INSERT INTO symptom_tag (id, name, department_id, description) VALUES
    (1, '咳嗽咳痰',   1, '呼吸道症状'),
    (2, '胸闷胸痛',   2, '心血管症状'),
    (3, '腹痛腹泻',   3, '消化道症状'),
    (4, '头痛眩晕',   4, '神经症状'),
    (5, '关节肿痛',   6, '运动系统症状'),
    (6, '小儿发热',   7, '儿科症状');

-- 7. 危急征象规则（命中即硬转急诊）
INSERT INTO red_flag_rule (id, name, keywords, level, advice) VALUES
    (1, '急性胸痛',   '胸痛,压榨感,放射痛,大汗', 1, '立即转急诊科，启动胸痛中心绿色通道'),
    (2, '呼吸困难',   '呼吸困难,喘憋,紫绀,窒息感', 1, '立即转急诊科，评估气道与氧合'),
    (3, '卒中征象',   '偏瘫,口角歪斜,言语不清,意识丧失', 1, '立即转急诊科，启动卒中绿色通道'),
    (4, '剧烈腹痛',   '剧烈腹痛,板状腹,便血', 2, '优先排查急腹症，建议急诊就诊'),
    (5, '高热惊厥',   '高热惊厥,抽搐,意识模糊', 2, '儿科急诊，防误吸与脑损伤');

-- 11. MCP 工具注册表（数据库驱动真开关，6 个真实业务工具）
INSERT INTO mcp_tool (tool_code, name, description, param_schema, enabled, sort) VALUES
    ('department_query', '科室查询', '按名称或症状关键词查询科室信息',
        '[{"name":"keyword","type":"string","required":false,"desc":"科室名或症状关键词"}]', 1, 10),
    ('doctor_query', '医生查询', '按科室/职称查询医生及其擅长',
        '[{"name":"department_id","type":"integer","required":false,"desc":"科室ID"},{"name":"title","type":"string","required":false,"desc":"职称"}]', 1, 20),
    ('schedule_query', '排班余号查询', '查询某医生/科室指定日期与时段的可约号源',
        '[{"name":"doctor_id","type":"integer","required":true,"desc":"医生ID"},{"name":"work_date","type":"string","required":true,"desc":"日期 YYYY-MM-DD"},{"name":"period","type":"string","required":false,"desc":"AM或PM"}]', 1, 30),
    ('patient_profile_query', '患者档案读取', '读取患者既往史、过敏史、基础信息',
        '[{"name":"patient_id","type":"integer","required":true,"desc":"患者ID"}]', 1, 40),
    ('symptom_tag_search', '症状标签检索', '根据症状关键词检索标准症状标签与归属科室',
        '[{"name":"keyword","type":"string","required":true,"desc":"症状关键词"}]', 1, 50),
    ('knowledge_retrieve', '医学知识检索', '从医学知识库检索相关片段辅助分诊',
        '[{"name":"query","type":"string","required":true,"desc":"检索问句"},{"name":"top_k","type":"integer","required":false,"desc":"召回条数"}]', 1, 60);

-- 21. 症状→科室映射（RAG 兜底 + 二次核验参考）
INSERT INTO symptom_department_map (symptom_keyword, department_id, weight) VALUES
    ('咳嗽', 1, 3), ('咳痰', 1, 2), ('哮喘', 1, 3),
    ('胸闷', 2, 3), ('胸痛', 2, 3), ('心悸', 2, 2), ('高血压', 2, 2),
    ('腹痛', 3, 3), ('腹泻', 3, 2), ('反酸', 3, 2),
    ('头痛', 4, 3), ('眩晕', 4, 2), ('麻木', 4, 2),
    ('骨折', 6, 3), ('关节痛', 6, 2),
    ('发热', 7, 2), ('小儿', 7, 3);

-- 22. AI 模型配置
INSERT INTO ai_model_config (id, name, provider, base_url, model, enabled, sort) VALUES
    (1, 'DeepSeek-V4-Flash', 'dashscope', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'deepseek-v4-flash', 1, 10),
    (2, 'Qwen3-Coder-Flash', 'dashscope', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'qwen3-coder-flash', 1, 20),
    (3, 'Text-Embedding-V3', 'dashscope', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'text-embedding-v3', 1, 30);

-- 8/9/10. 知识库 + 文档 + 片段
INSERT INTO knowledge_base (id, name, description) VALUES
    (1, '呼吸系统疾病库', '咳嗽、哮喘、慢阻肺等常见呼吸疾病诊疗要点');
INSERT INTO knowledge_doc (id, base_id, title, source_type, status) VALUES
    (1, 1, '支气管哮喘诊疗要点', 'text', 'READY');
INSERT INTO knowledge_segment (id, doc_id, base_id, content, vector_status) VALUES
    (1, 1, 1, '支气管哮喘典型表现为反复发作的喘息、气促、胸闷或咳嗽，夜间和清晨加重，常伴哮鸣音。', 'PENDING'),
    (2, 1, 1, '咳嗽变异性哮喘以慢性咳嗽为唯一或主要症状，无典型喘息，支气管激发试验阳性可确诊。', 'PENDING'),
    (3, 1, 1, '哮喘急性发作时需立即使用短效β2受体激动剂（如沙丁胺醇）吸入，严重者需急诊吸氧。', 'PENDING'),
    (4, 1, 1, '慢阻肺（COPD）多见于长期吸烟者，表现为慢性咳嗽、咳痰和活动后呼吸困难。', 'PENDING');
