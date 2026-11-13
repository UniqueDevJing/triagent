-- V6__intervention_extension.sql
-- 健康干预域扩展: 慢病管理、膳食日志、人群方案

CREATE TABLE IF NOT EXISTS chronic_disease_mgmt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    disease_type VARCHAR(50) NOT NULL COMMENT '高血压/糖尿病/冠心病/COPD',
    diagnosis_date DATE,
    medication JSON COMMENT '用药记录 [{name,dosage,frequency}]',
    target_indicators JSON COMMENT '目标指标 {SBP:140,FBG:6.1}',
    monitoring_frequency VARCHAR(20) DEFAULT 'MONTHLY',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='慢病管理表';

CREATE TABLE IF NOT EXISTS diet_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    meal_type VARCHAR(20) NOT NULL COMMENT 'BREAKFAST/LUNCH/DINNER/SNACK',
    food_items JSON COMMENT '[{name, amount, unit, calories}]',
    calories INT DEFAULT 0,
    recorded_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='膳食日志表';

CREATE TABLE IF NOT EXISTS crowd_programs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    target_group JSON COMMENT '目标人群 {age_min,age_max,gender,diseases}',
    description TEXT,
    content JSON COMMENT '方案内容 {diet,exercise,monitoring}',
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人群方案表';

INSERT INTO crowd_programs (name, target_group, description, content) VALUES
('高血压管理方案', JSON_OBJECT('diseases', JSON_ARRAY('高血压')), '针对高血压患者的健康管理方案，含饮食、运动、监测指导',
 JSON_OBJECT('diet', '低盐低脂饮食，每日盐摄入<6g，增加蔬果摄入', 'exercise', '每周≥5天中等强度有氧运动，每次30-60分钟', 'monitoring', '每周测量血压2-3次，目标<140/90mmHg')),
('糖尿病管理方案', JSON_OBJECT('diseases', JSON_ARRAY('糖尿病')), '针对糖尿病患者的血糖管理方案',
 JSON_OBJECT('diet', '控制碳水化合物摄入，定时定量进餐', 'exercise', '餐后1小时适度运动，每周≥150分钟', 'monitoring', '空腹血糖目标3.9-6.1mmol/L，餐后2h<7.8mmol/L')),
('健康体重管理方案', JSON_OBJECT('bmi_min', 24, 'bmi_max', 40), '针对超重/肥胖人群的体重管理方案',
 JSON_OBJECT('diet', '控制总热量摄入，均衡营养', 'exercise', '每周≥150分钟中等强度运动', 'monitoring', '每周称重1次，目标每月减重2-4kg'));

-- 扩展 intervention_plans 表 (兼容旧表; updated_at 已存在)
ALTER TABLE intervention_plans
    ADD COLUMN member_id BIGINT AFTER user_id,
    ADD COLUMN type VARCHAR(20) COMMENT 'CROWD/CHRONIC/DIET' AFTER member_id,
    ADD COLUMN description TEXT AFTER title,
    ADD COLUMN result JSON AFTER status;

-- 扩展 intervention_tasks 表 (updated_at 已存在, 无需添加)
