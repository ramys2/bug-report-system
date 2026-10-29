ALTER TABLE user_account
    MODIFY password_hash VARCHAR(255) NULL,
    ADD google_subject VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD CONSTRAINT uq_user_account_google_subject UNIQUE (google_subject),
    ADD CONSTRAINT chk_user_account_login_method
        CHECK (password_hash IS NOT NULL OR google_subject IS NOT NULL);
