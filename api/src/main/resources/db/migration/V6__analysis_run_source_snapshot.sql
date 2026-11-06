-- Historical runs remain unattributed; never infer their source from the current submission.
ALTER TABLE analysis_run
    ADD COLUMN source_snapshot TEXT,
    ADD COLUMN source_hash CHAR(64);

ALTER TABLE analysis_run
    ADD CONSTRAINT ck_analysis_run_source_snapshot_pair
    CHECK (
        (
            source_snapshot IS NULL
            AND source_hash IS NULL
        )
        OR
        (
            source_snapshot IS NOT NULL
            AND BTRIM(source_snapshot) <> ''
            AND OCTET_LENGTH(source_snapshot) <= 100000
            AND source_hash ~ '^[0-9a-f]{64}$'
        )
    );

CREATE OR REPLACE FUNCTION prevent_analysis_run_source_snapshot_rewrite()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.source_snapshot IS DISTINCT FROM OLD.source_snapshot
       OR NEW.source_hash IS DISTINCT FROM OLD.source_hash THEN
        RAISE EXCEPTION 'Analysis-run source snapshot is immutable';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_analysis_run_source_snapshot_immutable
BEFORE UPDATE OF source_snapshot, source_hash ON analysis_run
FOR EACH ROW
EXECUTE FUNCTION prevent_analysis_run_source_snapshot_rewrite();
