-- Money is always a BIGINT count of paise, never a float: 0.1 has no exact binary
-- representation, so float sums drift; integers are exact.

CREATE TABLE expenses (
    id           BIGSERIAL    PRIMARY KEY,
    group_id     BIGINT       NOT NULL REFERENCES expense_groups (id) ON DELETE CASCADE,
    paid_by      BIGINT       NOT NULL REFERENCES users (id),
    description  VARCHAR(200) NOT NULL,
    amount_paise BIGINT       NOT NULL,
    split_type   VARCHAR(10)  NOT NULL,
    expense_date DATE         NOT NULL,
    created_by   BIGINT       NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_expenses_amount_positive CHECK (amount_paise > 0),
    CONSTRAINT ck_expenses_split_type CHECK (split_type IN ('EQUAL', 'EXACT'))
);

-- Serves the paginated "newest first" listing per group.
CREATE INDEX idx_expenses_group_id_expense_date ON expenses (group_id, expense_date);

CREATE TABLE expense_shares (
    expense_id  BIGINT NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users (id),
    share_paise BIGINT NOT NULL,
    PRIMARY KEY (expense_id, user_id),
    CONSTRAINT ck_expense_shares_share_non_negative CHECK (share_paise >= 0)
);

CREATE TABLE settlements (
    id           BIGSERIAL   PRIMARY KEY,
    group_id     BIGINT      NOT NULL REFERENCES expense_groups (id) ON DELETE CASCADE,
    from_user    BIGINT      NOT NULL REFERENCES users (id),
    to_user      BIGINT      NOT NULL REFERENCES users (id),
    amount_paise BIGINT      NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_settlements_amount_positive CHECK (amount_paise > 0),
    CONSTRAINT ck_settlements_different_users CHECK (from_user <> to_user)
);

CREATE INDEX idx_settlements_group_id ON settlements (group_id);
