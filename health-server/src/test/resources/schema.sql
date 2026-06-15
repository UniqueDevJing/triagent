CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL,
    name VARCHAR(50),
    gender INT,
    age INT,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(200),
    emergency_contact VARCHAR(50),
    emergency_phone VARCHAR(20),
    blood_type VARCHAR(5),
    height DECIMAL(5,1),
    weight DECIMAL(5,1),
    medical_history TEXT,
    allergies VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS assessment_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200),
    description TEXT,
    category VARCHAR(50),
    questions TEXT,
    scoring_rules TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assessment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    template_id BIGINT,
    answers TEXT,
    total_score DECIMAL(5,2),
    risk_level VARCHAR(20),
    report_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS intervention_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    title VARCHAR(200),
    goal TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50),
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS intervention_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT,
    title VARCHAR(200),
    description TEXT,
    due_date DATE,
    status VARCHAR(50),
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    icon VARCHAR(50),
    sort_order INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    title VARCHAR(200),
    summary TEXT,
    content TEXT,
    author VARCHAR(50),
    view_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    session_id VARCHAR(100),
    role VARCHAR(20),
    content TEXT,
    feature_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS health_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    record_date DATE,
    type VARCHAR(50),
    metrics TEXT,
    report_url VARCHAR(500),
    doctor_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
