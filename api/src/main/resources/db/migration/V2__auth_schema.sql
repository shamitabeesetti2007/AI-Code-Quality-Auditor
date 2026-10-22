
CREATE TABLE app_user (
    user_id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_user_email_normalized
        CHECK (email = LOWER(BTRIM(email))),
    CONSTRAINT ck_user_role
        CHECK (role IN ('USER', 'RESEARCHER', 'ADMIN')),
    CONSTRAINT ck_user_account_status
        CHECK (account_status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE refresh_token (
    refresh_token_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(user_id) ON DELETE RESTRICT,
    family_id UUID NOT NULL,
    token_hash BYTEA NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_token(refresh_token_id)
        ON DELETE RESTRICT,
    revocation_reason VARCHAR(40),

    CONSTRAINT ck_refresh_token_hash_length
        CHECK (OCTET_LENGTH(token_hash) = 32),
    CONSTRAINT ck_refresh_token_expiry
        CHECK (expires_at > issued_at),
    CONSTRAINT ck_refresh_token_replacement
        CHECK (
            replaced_by IS NULL
            OR replaced_by <> refresh_token_id
        ),
    CONSTRAINT ck_refresh_token_revocation_reason
        CHECK (
            revocation_reason IS NULL
            OR revoked_at IS NOT NULL
        )
);

CREATE INDEX idx_refresh_token_user
    ON refresh_token(user_id);

CREATE INDEX idx_refresh_token_family
    ON refresh_token(family_id);

CREATE TABLE audit_log (
    audit_id UUID PRIMARY KEY,
    actor_user_id UUID REFERENCES app_user(user_id)
        ON DELETE RESTRICT,
    target_user_id UUID REFERENCES app_user(user_id)
        ON DELETE RESTRICT,
    event_type VARCHAR(60) NOT NULL,
    old_role VARCHAR(20),
    new_role VARCHAR(20),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,

    CONSTRAINT ck_audit_old_role
        CHECK (
            old_role IS NULL
            OR old_role IN ('USER', 'RESEARCHER', 'ADMIN')
        ),
    CONSTRAINT ck_audit_new_role
        CHECK (
            new_role IS NULL
            OR new_role IN ('USER', 'RESEARCHER', 'ADMIN')
        ),
    CONSTRAINT ck_audit_role_change
        CHECK (
            event_type <> 'ROLE_CHANGED'
            OR (
                actor_user_id IS NOT NULL
                AND target_user_id IS NOT NULL
                AND old_role IS NOT NULL
                AND new_role IS NOT NULL
                AND old_role <> new_role
            )
        )
);

CREATE INDEX idx_audit_log_target_time
    ON audit_log(target_user_id, occurred_at DESC);

CREATE UNIQUE INDEX uq_audit_single_admin_bootstrap
    ON audit_log(event_type)
    WHERE event_type = 'ADMIN_BOOTSTRAP';
