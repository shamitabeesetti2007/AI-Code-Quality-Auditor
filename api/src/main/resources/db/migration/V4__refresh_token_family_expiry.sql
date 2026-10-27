-- V4: Enforce one absolute expiry per refresh-token family.
-- Existing families use the earliest issued token as their
-- original issuance anchor.

ALTER TABLE refresh_token
    ADD COLUMN family_expires_at TIMESTAMPTZ;

WITH family_expiries AS (
    SELECT
        family_id,
        GREATEST(
            MIN(issued_at) + INTERVAL '30 days',
            MAX(expires_at)
        ) AS family_expiry
    FROM refresh_token
    GROUP BY family_id
)
UPDATE refresh_token AS token
SET family_expires_at = family_expiries.family_expiry
FROM family_expiries
WHERE token.family_id = family_expiries.family_id;

ALTER TABLE refresh_token
    ALTER COLUMN family_expires_at SET NOT NULL;

ALTER TABLE refresh_token
    ADD CONSTRAINT ck_refresh_token_family_expiry
    CHECK (
        family_expires_at > issued_at
        AND expires_at <= family_expires_at
    );

CREATE INDEX idx_refresh_token_family_expiry
    ON refresh_token(family_expires_at);
