CREATE TABLE message_outbox (
    outbox_id VARCHAR(255) NOT NULL PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    processed_at DATETIME(6) NULL,
    retry_attempts INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    
    INDEX idx_status_next_retry_at (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE request_outbox (
    outbox_id VARCHAR(255) NOT NULL PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    processed_at DATETIME(6) NULL,
    retry_attempts INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    
    INDEX idx_status_next_retry_at (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



CREATE TABLE notification_message (
    message_id              CHAR(36) NOT NULL PRIMARY KEY,
    request_id              CHAR(36) NOT NULL,
    notification_type       VARCHAR(50) NOT NULL,

    -- 수신자 정보
    user_id                 CHAR(36) DEFAULT NULL,
    phone_number            VARCHAR(20) DEFAULT NULL,
    email                   VARCHAR(255) DEFAULT NULL,
    device_token            VARCHAR(255) DEFAULT NULL,

    -- 발신자 정보
    sender_phone_number     VARCHAR(20) DEFAULT NULL,
    sender_email_address    VARCHAR(255) DEFAULT NULL,
    sender_name             VARCHAR(100) DEFAULT NULL,

    -- 알림 내용
    title                   VARCHAR(255) DEFAULT NULL,
    body                    TEXT DEFAULT NULL,
    redirect_url            TEXT DEFAULT NULL,
    image_url               TEXT DEFAULT NULL,

    -- 상태 및 시간 정보
    delivery_status         VARCHAR(50) DEFAULT 'PENDING',
    scheduled_at            DATETIME(6) DEFAULT NULL,
    dispatched_at           DATETIME(6) DEFAULT NULL,
    failure_reason          TEXT DEFAULT NULL,
    created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_notification_type (notification_type),
    INDEX idx_user_id (user_id),
    INDEX idx_phone_number (phone_number),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;