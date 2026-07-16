CREATE TABLE user_account
(
    id BIGINT NOT NULL AUTO_INCREMENT,

    username VARCHAR(32)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    nickname VARCHAR(32) NULL,

    avatar_url VARCHAR(500) NULL,

    status VARCHAR(16)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,

    auth_valid_after DATETIME(3) NOT NULL,

    last_login_time DATETIME(3) NULL,

    version INT NOT NULL,

    created_time DATETIME(3) NOT NULL,

    updated_time DATETIME(3) NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_user_account_username (username),

    CONSTRAINT chk_user_account_status
        CHECK(
            status IN ('NORMAL', 'DISABLED', 'DELETED')
        ),

    CONSTRAINT chk_user_account_version
        CHECK(
            version >= 0
        )
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;
