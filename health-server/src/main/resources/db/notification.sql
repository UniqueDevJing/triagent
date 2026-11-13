CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(500),
    type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/HEALTH/REMINDER/ALERT',
    is_read TINYINT NOT NULL DEFAULT 0,
    related_id BIGINT COMMENT '关联业务ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';
