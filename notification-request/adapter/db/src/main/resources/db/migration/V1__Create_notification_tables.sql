-- Idempotency key
CREATE TABLE IF NOT EXISTS idempotency_key (
    idempotency_key VARCHAR(36) PRIMARY KEY,
    operation_type VARCHAR(150) NOT NULL,
    data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- 알림 요청 메인 테이블
CREATE TABLE notification_request (
    request_id VARCHAR(36) PRIMARY KEY,
    requester_type VARCHAR(50) NOT NULL,
    requester_id VARCHAR(255) NOT NULL,
    notification_types VARCHAR(50) NOT NULL,
    content_id VARCHAR(50),
    template_info_id VARCHAR(50),
    memo TEXT,
    scheduled_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 인덱스 (status + notification_types 조합)
    INDEX idx_status_notification_types (status, notification_types)
);

-- 알림 콘텐츠 테이블
CREATE TABLE notification_request_content (
    content_id VARCHAR(50) PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL,
    title VARCHAR(255),
    body TEXT,
    image_url VARCHAR(255),
    redirect_url VARCHAR(255),

    -- 외래키 설정
    CONSTRAINT fk_content_request
        FOREIGN KEY (request_id)
        REFERENCES notification_request(request_id)
        ON DELETE CASCADE
);

-- 템플릿 정보 테이블
CREATE TABLE notification_request_template_info (
    template_info_id VARCHAR(50) PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL,
    template_id VARCHAR(255),
    parameters TEXT,

    CONSTRAINT fk_template_request
        FOREIGN KEY (request_id)
        REFERENCES notification_request(request_id)
        ON DELETE CASCADE
);

-- 수신자 정보 테이블
CREATE TABLE notification_request_recipient (
    recipient_id VARCHAR(36) PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL,
    recipient_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(36),
    email_address VARCHAR(255),
    phone_number VARCHAR(20),
    segment_name VARCHAR(255),
    device_token VARCHAR(255),

    CONSTRAINT fk_recipient_request
        FOREIGN KEY (request_id)
        REFERENCES notification_request(request_id)
        ON DELETE CASCADE
);

-- 발신자 정보 테이블
CREATE TABLE notification_request_sender (
    sender_id VARCHAR(36) PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    sender_name VARCHAR(255),
    sender_email VARCHAR(255),
    sender_phone VARCHAR(20),

    CONSTRAINT fk_sender_request
        FOREIGN KEY (request_id)
        REFERENCES notification_request(request_id)
        ON DELETE CASCADE
);


-- 알림 메시지 테이블
CREATE TABLE notification_message (
    message_id VARCHAR(36) PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(36),
    phone_number VARCHAR(20),
    email VARCHAR(255),
    device_token VARCHAR(255),
    language VARCHAR(10),
    sender_id VARCHAR(36),
    sender_phone_number VARCHAR(20),
    sender_email_address VARCHAR(255),
    sender_name VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    body TEXT,
    redirect_url VARCHAR(255),
    image_url VARCHAR(255),
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    scheduled_at TIMESTAMP,
    dispatched_at TIMESTAMP,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 외래키 설정
    CONSTRAINT fk_message_request
        FOREIGN KEY (request_id)
        REFERENCES notification_request(request_id)
        ON DELETE CASCADE,

    INDEX idx_user_id_notification_type (user_id, notification_type),
    INDEX idx_delivery_status (delivery_status)
);



-- Outbox 테이블 생성
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
);


-- Request Outbox 테이블 생성
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
);
