CREATE TABLE password_reset_tokens (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- SHA-256 of the token, hex encoded. The raw token only exists in the email.
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_password_reset_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);

-- Posted by a daily job, split equally among the group's members at posting time.
CREATE TABLE recurring_expenses (
    id            BIGSERIAL    PRIMARY KEY,
    group_id      BIGINT       NOT NULL REFERENCES expense_groups (id) ON DELETE CASCADE,
    paid_by       BIGINT       NOT NULL REFERENCES users (id),
    description   VARCHAR(200) NOT NULL,
    amount_paise  BIGINT       NOT NULL,
    -- 1 to 28 so every month has the day; no "31st in February" edge cases.
    day_of_month  INT          NOT NULL,
    next_run_date DATE         NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by    BIGINT       NOT NULL REFERENCES users (id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_recurring_expenses_amount_positive CHECK (amount_paise > 0),
    CONSTRAINT ck_recurring_expenses_day_of_month CHECK (day_of_month BETWEEN 1 AND 28)
);

-- The job only ever looks for active rows that are due.
CREATE INDEX idx_recurring_expenses_due ON recurring_expenses (next_run_date) WHERE active;
CREATE INDEX idx_recurring_expenses_group_id ON recurring_expenses (group_id);
