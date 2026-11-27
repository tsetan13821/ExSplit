-- "groups" is an SQL keyword, hence expense_groups.
CREATE TABLE expense_groups (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_by BIGINT       NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE group_members (
    group_id  BIGINT      NOT NULL REFERENCES expense_groups (id) ON DELETE CASCADE,
    user_id   BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role      VARCHAR(10) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT ck_group_members_role CHECK (role IN ('OWNER', 'MEMBER'))
);

-- The primary key covers lookups by group; this covers "which groups am I in".
CREATE INDEX idx_group_members_user_id ON group_members (user_id);

CREATE TABLE group_invites (
    id         BIGSERIAL   PRIMARY KEY,
    group_id   BIGINT      NOT NULL REFERENCES expense_groups (id) ON DELETE CASCADE,
    -- SHA-256 of the invite token, hex encoded. The raw token only exists in the link.
    token_hash VARCHAR(64) NOT NULL,
    created_by BIGINT      NOT NULL REFERENCES users (id),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    used_by    BIGINT      REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_group_invites_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_group_invites_used CHECK ((used_at IS NULL) = (used_by IS NULL))
);

CREATE INDEX idx_group_invites_group_id ON group_invites (group_id);
