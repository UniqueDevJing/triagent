-- V7__knowledge_extension.sql
-- 知识库扩展: 宣教内容、宣教词、运动项目库、疾病库、健康食谱库

-- 1. 宣教内容表
CREATE TABLE IF NOT EXISTS education_contents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content TEXT,
    media_url VARCHAR(500) COMMENT '媒体资源URL',
    type VARCHAR(20) DEFAULT 'ARTICLE' COMMENT 'ARTICLE/VIDEO/INFOGRAPHIC',
    author VARCHAR(50),
    view_count INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES knowledge_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宣教内容表';

INSERT INTO education_contents (category_id, title, summary, content, type, author) VALUES
(1, '高血压患者日常饮食指南', '高血压患者的低盐低脂饮食原则与食谱推荐',
 '<h3>低盐饮食原则</h3><p>每日食盐摄入量应控制在6g以下，避免腌制食品、加工肉制品等高盐食物。</p><h3>推荐食物</h3><ul><li>新鲜蔬菜水果</li><li>全谷物</li><li>低脂乳制品</li><li>鱼类</li></ul><h3>避免食物</h3><ul><li>咸菜、泡菜</li><li>腊肉、香肠</li><li>方便面</li><li>酱油、味精过量使用</li></ul>',
 'ARTICLE', '营养科'),
(1, '糖尿病血糖监测指南', '糖尿病患者的血糖监测方法与频率指导',
 '<h3>监测频率</h3><p>根据病情和治疗方案，血糖监测频率有所不同。</p><h3>监测时间点</h3><ul><li>空腹血糖：清晨空腹状态下测量</li><li>餐后2小时血糖：从第一口饭开始计时</li><li>睡前血糖：临睡前测量</li></ul><h3>目标值</h3><ul><li>空腹血糖：3.9-6.1 mmol/L</li><li>餐后2小时：<7.8 mmol/L</li><li>糖化血红蛋白：<7.0%</li></ul>',
 'ARTICLE', '内分泌科'),
(1, '合理运动促进健康', '适合全人群的运动方案与运动处方',
 '<h3>运动原则</h3><p>循序渐进、量力而行、持之以恒。</p><h3>推荐运动量</h3><ul><li>每周至少150分钟中等强度有氧运动</li><li>每周2-3次力量训练</li><li>每天进行柔韧性练习</li></ul>',
 'ARTICLE', '运动医学科');

-- 2. 宣教词表
CREATE TABLE IF NOT EXISTS education_words (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term VARCHAR(100) NOT NULL COMMENT '术语',
    definition TEXT NOT NULL COMMENT '定义解释',
    category VARCHAR(50) COMMENT '分类',
    example VARCHAR(500) COMMENT '示例/用法',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宣教词表';

INSERT INTO education_words (term, definition, category, example) VALUES
('BMI', '身体质量指数(Body Mass Index)，计算公式：体重(kg)/身高(m)²，用于评估体重是否在健康范围', '基本指标', 'BMI=24.5 属于超重范围（24.0-27.9）'),
('血压', '血液在血管内流动时对血管壁产生的侧压力，分为收缩压和舒张压', '基本指标', '理想血压为收缩压<120mmHg且舒张压<80mmHg'),
('空腹血糖', '禁食8-12小时后测量的血糖值，反映基础胰岛素分泌功能', '代谢指标', '空腹血糖正常范围3.9-6.1mmol/L'),
('糖化血红蛋白', '反映过去2-3个月平均血糖水平的指标，是糖尿病管理的金标准', '代谢指标', 'HbA1c<7.0%为糖尿病控制良好'),
('代谢综合征', '一组以肥胖、高血糖、血脂异常、高血压等多种代谢异常聚集为特征的临床症候群', '疾病概念', '具备以下三项即可诊断：腹型肥胖+高血糖+高血压+高甘油三酯+低HDL-C'),
('碳水化合物', '由碳、氢、氧组成的有机化合物，是人体最主要的能量来源，每克提供4千卡能量', '营养', '主食如米饭、面条富含碳水化合物'),
('有氧运动', '在氧气充分供应的情况下进行的、持续时间较长的节律性运动', '运动', '快走、慢跑、游泳、骑自行车等'),
('膳食纤维', '植物性食物中不能被人体消化吸收的多糖类物质，有助于肠道健康和控制血糖', '营养', '全谷物、豆类、蔬菜、水果富含膳食纤维');

-- 3. 运动项目库
CREATE TABLE IF NOT EXISTS exercise_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL COMMENT 'AEROBIC/STRENGTH/FLEXIBILITY/BALANCE',
    description TEXT,
    calories_per_hour INT DEFAULT 0 COMMENT '每小时消耗热量(kcal)',
    intensity VARCHAR(10) DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH',
    suitable_for VARCHAR(500) COMMENT '适用人群',
    precautions VARCHAR(500) COMMENT '注意事项',
    image_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动项目库';

INSERT INTO exercise_library (name, category, description, calories_per_hour, intensity, suitable_for, precautions) VALUES
('快走', 'AEROBIC', '以较快速度步行，心率达到最大心率的60-70%', 300, 'MEDIUM', '所有人群，尤其适合中老年及体重较大者', '穿着舒适运动鞋，选择平坦路面'),
('慢跑', 'AEROBIC', '以舒适速度慢跑，心率达到最大心率的65-80%', 500, 'MEDIUM', '有一定运动基础的人群', '跑前热身，注意膝盖保护，避免硬地面'),
('游泳', 'AEROBIC', '全身性有氧运动，对关节压力小', 600, 'HIGH', '所有人群，尤其适合关节不适及超重者', '注意安全，避免空腹游泳'),
('骑自行车', 'AEROBIC', '骑行运动，可室内外进行', 400, 'MEDIUM', '全年龄段人群', '注意交通安全，调整合适座椅高度'),
('太极拳', 'BALANCE', '中国传统武术，动作缓慢柔和，强调呼吸与动作配合', 250, 'LOW', '中老年人及慢性病患者', '衣着宽松，选择空气流通处练习'),
('瑜伽', 'FLEXIBILITY', '通过体式练习改善柔韧性和平衡能力', 200, 'LOW', '全人群，适合压力大、柔韧性差者', '量力而行，不强行完成高难度体式'),
('深蹲', 'STRENGTH', '下肢力量训练基础动作', 350, 'MEDIUM', '下肢力量训练者', '保持背部直立，膝盖不超过脚尖'),
('平板支撑', 'STRENGTH', '核心肌群训练动作', 200, 'LOW', '需要增强核心力量者', '保持身体平直，腹部收紧'),
('弹力带训练', 'STRENGTH', '使用弹力带进行抗阻训练', 250, 'MEDIUM', '各年龄段，康复期训练者', '选择合适阻力的弹力带'),
('拉伸运动', 'FLEXIBILITY', '各部位肌肉拉伸放松', 150, 'LOW', '所有人群，运动前后必备', '缓慢进行，有牵拉感即可，避免弹震');

-- 4. 疾病库
CREATE TABLE IF NOT EXISTS disease_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) COMMENT '疾病分类',
    description TEXT,
    symptoms TEXT COMMENT '症状',
    causes TEXT COMMENT '病因',
    treatment TEXT COMMENT '治疗原则',
    prevention TEXT COMMENT '预防措施',
    risk_factors VARCHAR(500) COMMENT '危险因素',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='疾病库';

INSERT INTO disease_library (name, category, description, symptoms, causes, treatment, prevention, risk_factors) VALUES
('高血压', '心血管疾病', '以体循环动脉血压持续升高为主要特征的心血管综合征', '头晕、头痛、颈项板紧、疲劳、心悸，也可出现视力模糊、鼻出血', '遗传因素、高盐饮食、肥胖、精神紧张、缺乏运动、饮酒过量', '生活方式干预+药物治疗，目标血压<140/90mmHg', '限盐、减重、规律运动、戒烟限酒、保持心理平衡', '家族史、年龄>45岁、肥胖、高盐饮食、吸烟、饮酒'),
('2型糖尿病', '代谢疾病', '以胰岛素抵抗和/或胰岛素分泌不足为特征的慢性代谢性疾病', '多饮、多食、多尿、体重减轻、乏力、视力模糊', '遗传因素、肥胖、不健康饮食、缺乏运动', '饮食控制、规律运动、口服降糖药或胰岛素、血糖监测', '保持健康体重、均衡饮食、规律运动、定期体检', '家族史、超重肥胖、年龄>40岁、缺乏运动、妊娠糖尿病史'),
('冠心病', '心血管疾病', '冠状动脉粥样硬化导致血管狭窄或阻塞，引起心肌缺血缺氧的心脏病', '胸痛（心绞痛）、胸闷、心悸、气短、乏力', '动脉粥样硬化、高血压、高血脂、糖尿病、吸烟', '药物治疗、介入治疗（支架）、冠脉搭桥手术', '控制血压血脂血糖、戒烟、健康饮食、规律运动', '高血压、高血脂、糖尿病、吸烟、肥胖、家族史、年龄增长'),
('慢性阻塞性肺疾病', '呼吸系统疾病', '一组以持续性气流受限为特征的慢性呼吸系统疾病', '慢性咳嗽、咳痰、气短、呼吸困难、喘息', '长期吸烟、空气污染、职业粉尘和化学物质', '戒烟、支气管扩张剂、吸入糖皮质激素、氧疗', '戒烟、避免空气污染暴露、接种流感/肺炎疫苗', '吸烟、职业暴露、室内外空气污染、α1-抗胰蛋白酶缺乏'),
('高脂血症', '代谢疾病', '血脂水平升高的代谢性疾病，是动脉粥样硬化的重要危险因素', '多数无明显症状，常在体检发现', '遗传因素、不健康饮食、肥胖、缺乏运动、某些疾病继发', '饮食控制+运动+降脂药物', '合理饮食、规律运动、保持健康体重、定期查血脂', '高脂饮食、肥胖、缺乏运动、家族史'),
('骨质疏松症', '骨代谢疾病', '以骨量低、骨组织微结构损坏导致骨脆性增加、易发生骨折为特征的全身性骨病', '骨痛、身高缩短、驼背、易骨折', '年龄增长、雌激素减少、钙和维生素D缺乏', '钙剂+维生素D补充、抗骨质疏松药物、运动、防跌倒', '充足钙和维生素D摄入、规律运动、戒烟限酒', '绝经后女性、年龄>65岁、家族史、低体重、长期使用糖皮质激素');

-- 5. 健康食谱库
CREATE TABLE IF NOT EXISTS health_recipes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL COMMENT 'LOW_SALT/LOW_SUGAR/LOW_FAT/HIGH_PROTEIN/VEGETARIAN/GENERAL',
    ingredients TEXT COMMENT '食材清单JSON [{name,amount}]',
    steps TEXT COMMENT '制作步骤JSON [{step,description}]',
    calories INT DEFAULT 0 COMMENT '总热量(kcal)',
    nutrition_info JSON COMMENT '营养成分 {protein,fat,carbs,fiber}',
    suitable_for VARCHAR(200) COMMENT '适用人群/疾病',
    cooking_time INT DEFAULT 30 COMMENT '烹饪时间(分钟)',
    difficulty VARCHAR(10) DEFAULT 'EASY' COMMENT 'EASY/MEDIUM/HARD',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康食谱库';

INSERT INTO health_recipes (name, category, ingredients, steps, calories, nutrition_info, suitable_for, cooking_time, difficulty) VALUES
('清蒸鲈鱼', 'LOW_FAT', '[{"name":"鲈鱼","amount":"1条(约500g)"},{"name":"姜","amount":"3片"},{"name":"葱","amount":"2根"},{"name":"蒸鱼豉油","amount":"15ml"},{"name":"料酒","amount":"10ml"}]',
 '[{"step":1,"description":"鲈鱼洗净，两面划刀"},{"step":2,"description":"盘中铺姜片，放上鱼，淋料酒"},{"step":3,"description":"水开后上锅蒸8-10分钟"},{"step":4,"description":"倒掉蒸出的汁水，撒葱丝，淋蒸鱼豉油"},{"step":5,"description":"热油浇在葱丝上即可"}]',
 280, '{"protein":42,"fat":12,"carbs":3,"fiber":0}', '高血脂、高血压、减重人群', 20, 'EASY'),
('番茄菌菇汤', 'LOW_SALT', '[{"name":"番茄","amount":"2个"},{"name":"金针菇","amount":"100g"},{"name":"香菇","amount":"3朵"},{"name":"鸡蛋","amount":"1个"},{"name":"葱花","amount":"少许"}]',
 '[{"step":1,"description":"番茄切块，菌菇洗净"},{"step":2,"description":"热锅少油，炒软番茄出汁"},{"step":3,"description":"加入适量水，放入菌菇煮5分钟"},{"step":4,"description":"淋入蛋液搅拌"},{"step":5,"description":"撒葱花出锅（少盐调味）"}]',
 150, '{"protein":10,"fat":6,"carbs":14,"fiber":4}', '高血压、糖尿病患者', 15, 'EASY'),
('糙米蔬菜鸡肉饭', 'LOW_SUGAR', '[{"name":"糙米","amount":"100g"},{"name":"鸡胸肉","amount":"100g"},{"name":"西兰花","amount":"100g"},{"name":"胡萝卜","amount":"50g"},{"name":"橄榄油","amount":"5ml"}]',
 '[{"step":1,"description":"糙米提前浸泡30分钟，正常煮熟"},{"step":2,"description":"鸡胸肉切丁，用少许盐、胡椒腌制"},{"step":3,"description":"西兰花掰小朵焯水，胡萝卜切片"},{"step":4,"description":"少油炒熟鸡丁，加入蔬菜翻炒"}]',
 420, '{"protein":30,"fat":8,"carbs":55,"fiber":6}', '糖尿病患者、减重人群', 40, 'EASY'),
('五彩豆腐', 'VEGETARIAN', '[{"name":"嫩豆腐","amount":"300g"},{"name":"青椒","amount":"1个"},{"name":"红椒","amount":"1个"},{"name":"玉米粒","amount":"50g"},{"name":"木耳","amount":"50g"}]',
 '[{"step":1,"description":"豆腐切小块焯水"},{"step":2,"description":"青红椒切丁，木耳泡发切丝"},{"step":3,"description":"少油炒香蔬菜粒"},{"step":4,"description":"加入豆腐轻轻翻炒，调味勾薄芡"}]',
 220, '{"protein":18,"fat":10,"carbs":18,"fiber":5}', '素食者、高血脂、高血压人群', 20, 'EASY'),
('山药排骨汤', 'GENERAL', '[{"name":"排骨","amount":"300g"},{"name":"山药","amount":"200g"},{"name":"枸杞","amount":"10g"},{"name":"姜片","amount":"3片"},{"name":"料酒","amount":"10ml"}]',
 '[{"step":1,"description":"排骨焯水去血沫"},{"step":2,"description":"排骨、姜片、料酒入锅，加水煮沸转小火炖40分钟"},{"step":3,"description":"山药去皮切段加入，继续炖20分钟"},{"step":4,"description":"加枸杞煮5分钟，少许盐调味"}]',
 380, '{"protein":28,"fat":18,"carbs":20,"fiber":3}', '一般人群、体质虚弱者', 70, 'MEDIUM');

-- 6. 扩展 knowledge_categories 表
ALTER TABLE knowledge_categories
    ADD COLUMN type VARCHAR(20) DEFAULT 'ARTICLE' COMMENT 'ARTICLE/EXERCISE/DISEASE/RECIPE/EDUCATION' AFTER name,
    ADD COLUMN description VARCHAR(200) AFTER type;
