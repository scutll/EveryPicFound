ALTER TABLE user_account
    MODIFY username VARCHAR(80)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL;
