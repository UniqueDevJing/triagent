-- V5__assessment_extension.sql
-- 健康评估域扩展: 评估指标、中医体质、心理评测，扩展现有评估表

-- 1. 评估指标表
CREATE TABLE IF NOT EXISTS assessment_indicators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50),
    unit VARCHAR(20),
    reference_min DECIMAL(10,2),
    reference_max DECIMAL(10,2),
    category VARCHAR(20) COMMENT 'BLOOD/URINE/IMAGING/PHYSICAL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估指标表';

INSERT INTO assessment_indicators (name, code, unit, reference_min, reference_max, category) VALUES
('收缩压', 'SBP', 'mmHg', 90.00, 140.00, 'PHYSICAL'),
('舒张压', 'DBP', 'mmHg', 60.00, 90.00, 'PHYSICAL'),
('空腹血糖', 'FBG', 'mmol/L', 3.90, 6.10, 'BLOOD'),
('总胆固醇', 'TC', 'mmol/L', 2.80, 5.20, 'BLOOD'),
('甘油三酯', 'TG', 'mmol/L', 0.56, 1.70, 'BLOOD'),
('高密度脂蛋白', 'HDL-C', 'mmol/L', 1.03, 1.55, 'BLOOD'),
('低密度脂蛋白', 'LDL-C', 'mmol/L', 0.00, 3.36, 'BLOOD'),
('尿酸', 'UA', 'umol/L', 155.00, 428.00, 'BLOOD'),
('体重指数', 'BMI', 'kg/m²', 18.50, 23.90, 'PHYSICAL'),
('腰围', 'WC', 'cm', 0.00, 90.00, 'PHYSICAL'),
('血红蛋白', 'HB', 'g/L', 115.00, 175.00, 'BLOOD'),
('尿蛋白', 'PRO', '', 0.00, 0.00, 'URINE'),
('胸片', 'CXR', '', 0.00, 0.00, 'IMAGING');

-- 2. 中医体质表
CREATE TABLE IF NOT EXISTS tcm_constitutions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    features TEXT COMMENT '特征描述',
    advice TEXT COMMENT '调养建议',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中医体质表';

INSERT INTO tcm_constitutions (name, description, features, advice) VALUES
('平和质', '阴阳气血调和，体态适中，面色红润，精力充沛', '体形匀称健壮、面色肤色润泽、头发稠密有光泽、目光有神、鼻色明润、嗅觉通利、唇色红润、精力充沛、耐受寒热、睡眠良好、二便正常', '饮食有节、劳逸结合、坚持锻炼'),
('气虚质', '元气不足，疲乏、气短、自汗等气虚表现', '肌肉松软不实、平素语音低弱、气短懒言、容易疲乏、精神不振、易出汗、舌淡红、舌边有齿痕', '补气养气、宜食益气健脾食物'),
('阳虚质', '阳气不足，以畏寒怕冷、手足不温等虚寒表现为主', '肌肉松软不实、平素畏冷、手足不温、喜热饮食、精神不振、舌淡胖嫩', '温阳补气、宜食温阳食物'),
('阴虚质', '阴液亏少，以口燥咽干、手足心热等虚热表现为主', '体形偏瘦、手足心热、口燥咽干、鼻微干、喜冷饮、大便干燥、舌红少津', '滋阴降火、宜食甘凉滋润食物'),
('痰湿质', '痰湿凝聚，以形体肥胖、腹部肥满、口黏苔腻等痰湿表现为主', '体形肥胖、腹部肥满松软、面部皮肤油脂较多、多汗且黏、胸闷、痰多、口黏腻或甜', '健脾利湿、宜食清淡食物'),
('湿热质', '湿热内蕴，以面垢油光、口苦、苔黄腻等湿热表现为主', '形体中等或偏瘦、面垢油光、易生痤疮、口苦口干、身重困倦、大便黏滞不畅或燥结', '清热利湿、宜食清热食物'),
('血瘀质', '血行不畅，以肤色晦暗、舌质紫暗等血瘀表现为主', '肤色晦暗、色素沉着、容易出现瘀斑、口唇黯淡、舌暗或有瘀点', '活血化瘀、宜食活血食物'),
('气郁质', '气机郁滞，以神情抑郁、忧虑脆弱等气郁表现为主', '形体瘦者为多、神情抑郁、情感脆弱、烦闷不乐、舌淡红、苔薄白', '疏肝理气、宜食行气解郁食物'),
('特禀质', '先天失常，以生理缺陷、过敏反应等为主', '过敏体质者常见哮喘、风疹、咽痒、鼻塞、喷嚏等', '益气固表、避免过敏原');

-- 3. 心理评测量表
CREATE TABLE IF NOT EXISTS psychology_assessments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    questions JSON COMMENT '问卷题目 [{question, options: [{text, score}]}]',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心理评测量表';

INSERT INTO psychology_assessments (name, description, questions) VALUES
('SCL-90 症状自评量表', '用于评估个体心理健康状况的自评量表，包含90个项目，涵盖感觉、情感、思维、行为等多个方面',
 JSON_OBJECT('items', JSON_ARRAY(
   JSON_OBJECT('id', 1, 'question', '头痛', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有', 'score', 0), JSON_OBJECT('text', '轻度', 'score', 1), JSON_OBJECT('text', '中度', 'score', 2), JSON_OBJECT('text', '偏重', 'score', 3), JSON_OBJECT('text', '严重', 'score', 4))),
   JSON_OBJECT('id', 2, 'question', '神经过敏，心中不踏实', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有', 'score', 0), JSON_OBJECT('text', '轻度', 'score', 1), JSON_OBJECT('text', '中度', 'score', 2), JSON_OBJECT('text', '偏重', 'score', 3), JSON_OBJECT('text', '严重', 'score', 4))),
   JSON_OBJECT('id', 3, 'question', '头脑中有不必要的想法或字句盘旋', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有', 'score', 0), JSON_OBJECT('text', '轻度', 'score', 1), JSON_OBJECT('text', '中度', 'score', 2), JSON_OBJECT('text', '偏重', 'score', 3), JSON_OBJECT('text', '严重', 'score', 4))),
   JSON_OBJECT('id', 4, 'question', '感到孤独', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有', 'score', 0), JSON_OBJECT('text', '轻度', 'score', 1), JSON_OBJECT('text', '中度', 'score', 2), JSON_OBJECT('text', '偏重', 'score', 3), JSON_OBJECT('text', '严重', 'score', 4))),
   JSON_OBJECT('id', 5, 'question', '感到大多数人都不可信任', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有', 'score', 0), JSON_OBJECT('text', '轻度', 'score', 1), JSON_OBJECT('text', '中度', 'score', 2), JSON_OBJECT('text', '偏重', 'score', 3), JSON_OBJECT('text', '严重', 'score', 4)))
 ))),
('焦虑自评量表（SAS）', '评估焦虑症状严重程度的自评工具，包含20个条目',
 JSON_OBJECT('items', JSON_ARRAY(
   JSON_OBJECT('id', 1, 'question', '我觉得比平时容易紧张和着急', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有或很少时间', 'score', 1), JSON_OBJECT('text', '少部分时间', 'score', 2), JSON_OBJECT('text', '相当多时间', 'score', 3), JSON_OBJECT('text', '绝大部分或全部时间', 'score', 4))),
   JSON_OBJECT('id', 2, 'question', '我无缘无故地感到害怕', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有或很少时间', 'score', 1), JSON_OBJECT('text', '少部分时间', 'score', 2), JSON_OBJECT('text', '相当多时间', 'score', 3), JSON_OBJECT('text', '绝大部分或全部时间', 'score', 4))),
   JSON_OBJECT('id', 3, 'question', '我容易心里烦乱或觉得惊恐', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有或很少时间', 'score', 1), JSON_OBJECT('text', '少部分时间', 'score', 2), JSON_OBJECT('text', '相当多时间', 'score', 3), JSON_OBJECT('text', '绝大部分或全部时间', 'score', 4))),
   JSON_OBJECT('id', 4, 'question', '我觉得我可能将要发疯', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有或很少时间', 'score', 1), JSON_OBJECT('text', '少部分时间', 'score', 2), JSON_OBJECT('text', '相当多时间', 'score', 3), JSON_OBJECT('text', '绝大部分或全部时间', 'score', 4))),
   JSON_OBJECT('id', 5, 'question', '我觉得一切都很好，也不会发生什么不幸', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有或很少时间', 'score', 4), JSON_OBJECT('text', '少部分时间', 'score', 3), JSON_OBJECT('text', '相当多时间', 'score', 2), JSON_OBJECT('text', '绝大部分或全部时间', 'score', 1)))
 ))),
('抑郁自评量表（SDS）', '评估抑郁症状严重程度的自评工具',
 JSON_OBJECT('items', JSON_ARRAY(
   JSON_OBJECT('id', 1, 'question', '我觉得闷闷不乐，情绪低沉', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有或很少时间', 'score', 1), JSON_OBJECT('text', '少部分时间', 'score', 2), JSON_OBJECT('text', '相当多时间', 'score', 3), JSON_OBJECT('text', '绝大部分或全部时间', 'score', 4))),
   JSON_OBJECT('id', 2, 'question', '我觉得一天之中早晨最好', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有或很少时间', 'score', 4), JSON_OBJECT('text', '少部分时间', 'score', 3), JSON_OBJECT('text', '相当多时间', 'score', 2), JSON_OBJECT('text', '绝大部分或全部时间', 'score', 1))),
   JSON_OBJECT('id', 3, 'question', '我一阵阵哭出来或觉得想哭', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有或很少时间', 'score', 1), JSON_OBJECT('text', '少部分时间', 'score', 2), JSON_OBJECT('text', '相当多时间', 'score', 3), JSON_OBJECT('text', '绝大部分或全部时间', 'score', 4))),
   JSON_OBJECT('id', 4, 'question', '我晚上睡眠不好', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有或很少时间', 'score', 1), JSON_OBJECT('text', '少部分时间', 'score', 2), JSON_OBJECT('text', '相当多时间', 'score', 3), JSON_OBJECT('text', '绝大部分或全部时间', 'score', 4))),
   JSON_OBJECT('id', 5, 'question', '我吃得跟平常一样多', 'options', JSON_ARRAY(JSON_OBJECT('text', '没有或很少时间', 'score', 4), JSON_OBJECT('text', '少部分时间', 'score', 3), JSON_OBJECT('text', '相当多时间', 'score', 2), JSON_OBJECT('text', '绝大部分或全部时间', 'score', 1)))
 )));

-- 4. 扩展 assessment_templates 表
ALTER TABLE assessment_templates
    ADD COLUMN type VARCHAR(20) COMMENT 'RISK/TCM/PSYCHOLOGY' AFTER category,
    ADD COLUMN indicator_ids JSON COMMENT '关联评估指标' AFTER type,
    ADD COLUMN tcm_type_ids JSON COMMENT '关联中医体质类型' AFTER indicator_ids,
    ADD COLUMN psychology_ids JSON COMMENT '关联心理评测' AFTER tcm_type_ids,
    ADD COLUMN status TINYINT DEFAULT 1 AFTER psychology_ids;

-- 5. 扩展 assessment_records 表 (兼容旧 user_id)
ALTER TABLE assessment_records
    ADD COLUMN member_id BIGINT AFTER user_id,
    ADD COLUMN assessor_id BIGINT AFTER template_id,
    ADD COLUMN type VARCHAR(20) COMMENT 'RISK/TCM/PSYCHOLOGY' AFTER assessor_id,
    ADD COLUMN conclusion TEXT AFTER risk_level,
    ADD COLUMN suggestion TEXT AFTER conclusion,
    ADD COLUMN detailed_data JSON AFTER suggestion,
    ADD COLUMN assessed_at DATETIME AFTER detailed_data,
    ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;
