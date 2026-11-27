CREATE TABLE users (
    id              BIGSERIAL    PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    failed_attempts INT          NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email),
    -- emails are stored lowercased so the unique constraint is case-insensitive
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT ck_users_failed_attempts CHECK (failed_attempts >= 0)
);
