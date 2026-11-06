CREATE TABLE code_submission (
    submission_id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    language VARCHAR(30) NOT NULL,
    origin_type VARCHAR(30) NOT NULL,
    provenance_verified BOOLEAN NOT NULL DEFAULT FALSE,
    source_code TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    logical_loc INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_submission_title_nonblank
        CHECK (BTRIM(title) <> ''),
    CONSTRAINT ck_submission_language
        CHECK (language = 'JAVA'),
    CONSTRAINT ck_submission_origin
        CHECK (origin_type IN (
            'HUMAN', 'AI_GENERATED', 'AI_ASSISTED', 'AI_REMEDIATED'
        )),
    CONSTRAINT ck_submission_provenance
        CHECK (
            NOT provenance_verified
            OR origin_type IN ('AI_GENERATED', 'AI_REMEDIATED')
        ),
    CONSTRAINT ck_submission_source_nonblank
        CHECK (BTRIM(source_code) <> ''),
    CONSTRAINT ck_submission_source_size
        CHECK (OCTET_LENGTH(source_code) <= 100000),
    CONSTRAINT ck_submission_content_hash
        CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_submission_logical_loc
        CHECK (logical_loc >= 0),
    CONSTRAINT ck_submission_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT uq_code_submission_id_owner
        UNIQUE (submission_id, owner_user_id)
);

CREATE INDEX idx_code_submission_owner_created
    ON code_submission(owner_user_id, created_at DESC);

CREATE TABLE analysis_run (
    run_id UUID PRIMARY KEY,
    submission_id UUID NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    analyzer_version VARCHAR(80),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_summary TEXT,
    score NUMERIC,
    grade VARCHAR(5),

    CONSTRAINT ck_analysis_run_status
        CHECK (status IN (
            'QUEUED', 'PARSING', 'ANALYZING', 'SCORING',
            'COMPLETED', 'PARTIAL', 'PARSE_FAILED', 'FAILED'
        )),
    CONSTRAINT ck_analysis_run_lifecycle
        CHECK (
            completed_at IS NULL
            OR (
                started_at IS NOT NULL
                AND completed_at >= started_at
                AND status IN (
                    'COMPLETED', 'PARTIAL', 'PARSE_FAILED', 'FAILED'
                )
            )
        ),
    CONSTRAINT ck_analysis_run_score
        CHECK (score IS NULL OR score BETWEEN 0 AND 100),
    CONSTRAINT fk_analysis_run_submission_owner
        FOREIGN KEY (submission_id, owner_user_id)
        REFERENCES code_submission(submission_id, owner_user_id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_analysis_run_one_active_per_submission
    ON analysis_run(submission_id)
    WHERE status IN ('QUEUED', 'PARSING', 'ANALYZING', 'SCORING');

CREATE INDEX idx_analysis_run_owner_created
    ON analysis_run(owner_user_id, created_at DESC);
CREATE INDEX idx_analysis_run_submission_created
    ON analysis_run(submission_id, created_at DESC);
CREATE INDEX idx_analysis_run_created
    ON analysis_run(created_at DESC);

CREATE TABLE analysis_metric (
    metric_id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES analysis_run(run_id) ON DELETE CASCADE,
    dimension VARCHAR(40) NOT NULL,
    metric_key VARCHAR(80) NOT NULL,
    numeric_value NUMERIC NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,

    CONSTRAINT uq_analysis_metric_run_dimension_key
        UNIQUE (run_id, dimension, metric_key),
    CONSTRAINT ck_analysis_metric_dimension
        CHECK (BTRIM(dimension) <> ''),
    CONSTRAINT ck_analysis_metric_key
        CHECK (BTRIM(metric_key) <> ''),
    CONSTRAINT ck_analysis_metric_metadata_object
        CHECK (JSONB_TYPEOF(metadata) = 'object')
);

CREATE TABLE analysis_finding (
    finding_id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES analysis_run(run_id) ON DELETE CASCADE,
    rule_id VARCHAR(80) NOT NULL,
    fingerprint CHAR(64) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    description TEXT,
    file_path VARCHAR(500),
    start_line INTEGER,
    start_column INTEGER,
    end_line INTEGER,
    end_column INTEGER,
    cwe_id VARCHAR(20),
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,

    CONSTRAINT ck_analysis_finding_rule
        CHECK (BTRIM(rule_id) <> ''),
    CONSTRAINT ck_analysis_finding_fingerprint
        CHECK (fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_analysis_finding_severity
        CHECK (severity IN ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_analysis_finding_message
        CHECK (BTRIM(message) <> ''),
    CONSTRAINT ck_analysis_finding_lines
        CHECK (
            (start_line IS NULL OR start_line > 0)
            AND (end_line IS NULL OR end_line > 0)
            AND (start_line IS NULL OR end_line IS NULL OR end_line >= start_line)
        ),
    CONSTRAINT ck_analysis_finding_columns
        CHECK (
            (start_column IS NULL OR start_column > 0)
            AND (end_column IS NULL OR end_column > 0)
        ),
    CONSTRAINT ck_analysis_finding_metadata_object
        CHECK (JSONB_TYPEOF(metadata) = 'object')
);

CREATE INDEX idx_analysis_finding_run_fingerprint
    ON analysis_finding(run_id, fingerprint);
