CREATE DATABASE IF NOT EXISTS everypicfound
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE everypicfound;
DROP TABLE IF EXISTS image_asset;

CREATE TABLE image_asset(
    id BIGINT UNSIGNED NOT NULL COMMENT '主键、图片ID、同时对应向量库 vector_id、由雪花算法生成',
    file_name VARCHAR(255) NOT NULL COMMENT '系统生成文件名',
    original_file_name VARCHAR(255) NOT NULL COMMENT '用户原始文件名',
    file_hash CHAR(64) NOT NULL COMMENT '文件 SHA-256 hash，用于去重',
    file_size BIGINT UNSIGNED NOT NULL COMMENT '文件大小，单位 byte',
    mime_type VARCHAR(100) NOT NULL COMMENT 'MIME 类型',
    file_ext VARCHAR(20) NOT NULL COMMENT '文件扩展名',

    width INT UNSIGNED DEFAULT NULL COMMENT '图片宽度',
    height INT UNSIGNED DEFAULT NULL COMMENT '图片高度',

    storage_path VARCHAR(500) NOT NULL COMMENT '图片存储路径',
    thumbnail_path VARCHAR(500) DEFAULT NULL COMMENT '缩略图路径，MVP 可为空',

    image_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '图片状态：1 NORMAL，2 DELETED，3 INVALID',
    vector_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '向量状态：1 PENDING，2 PROCESSING，3 READY，4 FAILED',

    vector_updated_time DATETIME DEFAULT NULL COMMENT '向量完成更新时间',
    processing_started_time DATETIME DEFAULT NULL COMMENT '向量化开始时间',

    retry_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '向量化重试次数',
    fail_reason VARCHAR(1000) DEFAULT NULL COMMENT '最近一次失败原因',

    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',

    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),

    UNIQUE KEY uk_file_hash (file_hash),

    KEY idx_image_vector_status (image_status, vector_status),
    KEY idx_vector_status_processing_time (vector_status, processing_started_time),
    KEY idx_created_time (created_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    COMMENT = '图片资产表';