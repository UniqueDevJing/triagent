-- V4__appointments.sql
-- 预约管理域: 检测项目组、检测项、套餐、套餐明细、预约

CREATE TABLE IF NOT EXISTS exam_item_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测项目组';

INSERT INTO exam_item_categories (name, sort_order) VALUES
('一般检查', 1), ('血液检查', 2), ('尿液检查', 3),
('影像检查', 4), ('心电图', 5), ('超声检查', 6), ('其他', 7);

CREATE TABLE IF NOT EXISTS exam_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    reference_range VARCHAR(200),
    unit VARCHAR(20),
    category_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES exam_item_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测项';

INSERT INTO exam_items (name, description, reference_range, unit, category_id) VALUES
('身高', '测量身高', '100-200', 'cm', 1),
('体重', '测量体重', '30-150', 'kg', 1),
('血压(收缩压)', '收缩压测量', '90-140', 'mmHg', 1),
('血压(舒张压)', '舒张压测量', '60-90', 'mmHg', 1),
('白细胞计数', '白细胞计数', '3.5-9.5', '10^9/L', 2),
('红细胞计数', '红细胞计数', '3.8-5.8', '10^12/L', 2),
('血红蛋白', '血红蛋白浓度', '115-175', 'g/L', 2),
('血小板计数', '血小板计数', '125-350', '10^9/L', 2),
('空腹血糖', '空腹血糖', '3.9-6.1', 'mmol/L', 2),
('总胆固醇', '总胆固醇', '2.8-5.2', 'mmol/L', 2),
('甘油三酯', '甘油三酯', '0.56-1.7', 'mmol/L', 2),
('尿蛋白', '尿蛋白定性', '阴性', '', 3),
('尿糖', '尿糖定性', '阴性', '', 3),
('胸部X光', '胸部正位片', '未见异常', '', 4),
('心电图', '静息心电图', '正常心电图', '', 5),
('腹部超声', '肝胆脾胰超声', '未见异常', '', 6);

CREATE TABLE IF NOT EXISTS packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    icon VARCHAR(50),
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐表';

INSERT INTO packages (name, description, price, icon) VALUES
('基础体检套餐', '包含一般检查、血常规、尿常规、心电图', 299.00, 'Document'),
('标准体检套餐', '基础套餐+血脂血糖+腹部超声', 599.00, 'DocumentChecked'),
('高端体检套餐', '标准套餐+胸部X光+肿瘤标志物筛查', 1299.00, 'Star'),
('入职体检套餐', '快速入职体检必备项目', 199.00, 'User');

CREATE TABLE IF NOT EXISTS package_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    exam_item_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE CASCADE,
    FOREIGN KEY (exam_item_id) REFERENCES exam_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐项目明细';

-- 基础套餐: 一般检查+血常规+尿常规+心电图
INSERT INTO package_items (package_id, exam_item_id, sort_order) VALUES
(1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 4, 4),
(1, 5, 5), (1, 6, 6), (1, 7, 7), (1, 8, 8),
(1, 12, 9), (1, 13, 10), (1, 15, 11);

-- 标准套餐: 基础套餐 + 血脂血糖 + 腹部超声
INSERT INTO package_items (package_id, exam_item_id, sort_order) VALUES
(2, 1, 1), (2, 2, 2), (2, 3, 3), (2, 4, 4),
(2, 5, 5), (2, 6, 6), (2, 7, 7), (2, 8, 8),
(2, 9, 9), (2, 10, 10), (2, 11, 11),
(2, 12, 12), (2, 13, 13), (2, 15, 14), (2, 16, 15);

-- 高端套餐: 标准套餐 + 胸部X光
INSERT INTO package_items (package_id, exam_item_id, sort_order) VALUES
(3, 1, 1), (3, 2, 2), (3, 3, 3), (3, 4, 4),
(3, 5, 5), (3, 6, 6), (3, 7, 7), (3, 8, 8),
(3, 9, 9), (3, 10, 10), (3, 11, 11),
(3, 12, 12), (3, 13, 13), (3, 14, 14), (3, 15, 15), (3, 16, 16);

-- 入职套餐: 一般检查+血常规+尿常规+心电图
INSERT INTO package_items (package_id, exam_item_id, sort_order) VALUES
(4, 1, 1), (4, 2, 2), (4, 3, 3), (4, 4, 4),
(4, 5, 5), (4, 7, 6), (4, 12, 7), (4, 15, 8);

CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    package_id BIGINT,
    appointment_date DATE NOT NULL,
    time_slot VARCHAR(20) DEFAULT 'MORNING',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/DONE/CANCELLED',
    notes VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (package_id) REFERENCES packages(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';
