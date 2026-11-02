CREATE INDEX idx_analysis_run_submission_owner_created_run
    ON analysis_run (
        submission_id,
        owner_user_id,
        created_at DESC,
        run_id DESC
    );
