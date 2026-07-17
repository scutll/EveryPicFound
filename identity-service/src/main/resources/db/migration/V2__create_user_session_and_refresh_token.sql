CREATE TABLE user_session
(
    session_id VARCHAR(64)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    user_id BIGINT NOT NULL,

    status VARCHAR(16)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    created_time DATETIME(3) NOT NULL,

    expires_time DATETIME(3) NOT NULL,

    revoked_time DATETIME(3) NULL,

    last_seen_time DATETIME(3) NULL,

    PRIMARY KEY (session_id),

    KEY idx_user_session_user_status (user_id, status),

    CONSTRAINT fk_user_session_user_account
        FOREIGN KEY (user_id)
        REFERENCES user_account (id),

    CONSTRAINT chk_user_session_status
        CHECK(
            status IN ('ACTIVE', 'REVOKED', 'EXPIRED')
        )
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_refresh_token
(
    token_hash CHAR(64)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    session_id VARCHAR(64)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    status VARCHAR(16)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    issued_time DATETIME(3) NOT NULL,

    expires_time DATETIME(3) NOT NULL,

    used_time DATETIME(3) NULL,

    revoked_time DATETIME(3) NULL,

    PRIMARY KEY (token_hash),

    KEY idx_user_refresh_token_session_status (session_id, status),

    CONSTRAINT fk_user_refresh_token_user_session
        FOREIGN KEY (session_id)
        REFERENCES user_session (session_id),

    CONSTRAINT chk_user_refresh_token_status
        CHECK(
            status IN ('ACTIVE', 'USED', 'REVOKED', 'EXPIRED')
        )
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;
