-- V3__members.sql
CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    gender TINYINT DEFAULT 0 COMMENT '0未知 1男 2女',
    age INT DEFAULT 0,
    id_card VARCHAR(18),
    phone VARCHAR(20),
    emergency_contact VARCHAR(50),
    emergency_phone VARCHAR(20),
    blood_type VARCHAR(5),
    height DECIMAL(5,2),
    weight DECIMAL(5,2),
    medical_history JSON,
    allergies JSON,
    member_level VARCHAR(20) DEFAULT 'NORMAL',
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

CREATE TABLE IF NOT EXISTS physical_exam_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    start_date DATE,
    end_date DATE,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检计划表';
