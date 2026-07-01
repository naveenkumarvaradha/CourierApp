CREATE TABLE password_reset_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reset_token UNIQUE (token)
);

CREATE INDEX idx_prt_token   ON password_reset_tokens (token);
CREATE INDEX idx_prt_user    ON password_reset_tokens (user_id);
CREATE INDEX idx_prt_expires ON password_reset_tokens (expires_at);
