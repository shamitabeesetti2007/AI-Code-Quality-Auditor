
-- V3: Authentication schema hardening
-- PostgreSQL 16
-- Test on a fresh disposable database before applying anywhere persistent.

-- ============================================================
-- 1. Refresh-token lifecycle and replacement integrity
-- ============================================================

-- Unrevoked tokens must have no reason.
-- Revoked tokens must have a non-null approved reason.
ALTER TABLE refresh_token
    ADD CONSTRAINT ck_refresh_token_revocation_reason_allowed
    CHECK (
        (
            revoked_at IS NULL
            AND revocation_reason IS NULL
        )
        OR
        (
            revoked_at IS NOT NULL
            AND revocation_reason IS NOT NULL
            AND revocation_reason IN (
                'LOGOUT',
                'REUSE_DETECTED',
                'ACCOUNT_DISABLED'
            )
        )
    );

-- A token is either unconsumed/unreplaced or consumed/replaced.
-- Revocation remains independent of consumption.
ALTER TABLE refresh_token
    ADD CONSTRAINT ck_refresh_token_lifecycle
    CHECK (
        (used_at IS NULL AND replaced_by IS NULL)
        OR
        (used_at IS NOT NULL AND replaced_by IS NOT NULL)
    );

-- Ensure replacements belong to the same user and token family.
ALTER TABLE refresh_token
    ADD CONSTRAINT uq_refresh_token_identity_user_family
    UNIQUE (refresh_token_id, user_id, family_id);

ALTER TABLE refresh_token
    ADD CONSTRAINT fk_refresh_token_replacement_same_user_family
    FOREIGN KEY (replaced_by, user_id, family_id)
    REFERENCES refresh_token (
        refresh_token_id, user_id, family_id
    )
    ON DELETE RESTRICT;

-- A successor can have at most one predecessor.
CREATE UNIQUE INDEX uq_refresh_token_replaced_by
    ON refresh_token(replaced_by)
    WHERE replaced_by IS NOT NULL;

-- Once consumption is recorded, its timestamp and replacement
-- link cannot be rewritten.
CREATE OR REPLACE FUNCTION prevent_refresh_replacement_rewrite()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.replaced_by IS NOT NULL
       AND NEW.replaced_by IS DISTINCT FROM OLD.replaced_by THEN
        RAISE EXCEPTION
            'Refresh-token replacement link is immutable';
    END IF;

    IF OLD.used_at IS NOT NULL
       AND NEW.used_at IS DISTINCT FROM OLD.used_at THEN
        RAISE EXCEPTION
            'Consumed refresh-token timestamp is immutable';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_refresh_replacement_immutable
BEFORE UPDATE ON refresh_token
FOR EACH ROW
EXECUTE FUNCTION prevent_refresh_replacement_rewrite();


-- ============================================================
-- 2. Audit-log bootstrap integrity constraints
-- ============================================================

-- Bootstrap must identify its target, assign ADMIN, have no old
-- role, and provide a nonblank reason.
ALTER TABLE audit_log
    ADD CONSTRAINT ck_audit_bootstrap_integrity
    CHECK (
        event_type <> 'ADMIN_BOOTSTRAP'
        OR (
            actor_user_id IS NULL
            AND target_user_id IS NOT NULL
            AND new_role = 'ADMIN'
            AND old_role IS NULL
            AND NULLIF(BTRIM(metadata ->> 'reason'), '') IS NOT NULL
        )
    );

-- A missing actor is permitted only for ADMIN_BOOTSTRAP.
ALTER TABLE audit_log
    ADD CONSTRAINT ck_audit_null_actor_restricted
    CHECK (
        actor_user_id IS NOT NULL
        OR event_type = 'ADMIN_BOOTSTRAP'
    );


-- ============================================================
-- 3. Verify the actual target role for ADMIN_BOOTSTRAP
-- ============================================================

CREATE OR REPLACE FUNCTION enforce_admin_bootstrap_target_role()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    target_role VARCHAR(20);
BEGIN
    IF NEW.event_type = 'ADMIN_BOOTSTRAP' THEN
        -- Lock the target row while verifying its actual role.
        -- The service must set ADMIN before inserting the audit row,
        -- within the same transaction.
        SELECT role
        INTO target_role
        FROM app_user
        WHERE user_id = NEW.target_user_id
        FOR UPDATE;

        IF NOT FOUND THEN
            RAISE EXCEPTION
                'ADMIN_BOOTSTRAP target user does not exist';
        END IF;

        IF target_role <> 'ADMIN' THEN
            RAISE EXCEPTION
                'ADMIN_BOOTSTRAP target must have ADMIN role';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_audit_bootstrap_target_role
BEFORE INSERT ON audit_log
FOR EACH ROW
WHEN (NEW.event_type = 'ADMIN_BOOTSTRAP')
EXECUTE FUNCTION enforce_admin_bootstrap_target_role();


-- ============================================================
-- 4. Audit-log append-only defense in depth
-- ============================================================

CREATE OR REPLACE FUNCTION prevent_audit_log_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Audit log entries are append-only';
END;
$$;

CREATE TRIGGER trg_audit_log_append_only
BEFORE UPDATE OR DELETE ON audit_log
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_log_mutation();


-- ============================================================
-- 5. Keep app_user.updated_at current
-- ============================================================

CREATE OR REPLACE FUNCTION set_app_user_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_app_user_updated_at
BEFORE UPDATE ON app_user
FOR EACH ROW
EXECUTE FUNCTION set_app_user_updated_at();
