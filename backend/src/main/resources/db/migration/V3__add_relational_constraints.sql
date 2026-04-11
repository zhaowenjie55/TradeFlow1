DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gv_analysis_task_log_task'
    ) THEN
        ALTER TABLE gv_analysis_task_log
            ADD CONSTRAINT fk_gv_analysis_task_log_task
            FOREIGN KEY (task_id)
            REFERENCES gv_analysis_task(task_id)
            ON DELETE RESTRICT
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gv_search_run_task'
    ) THEN
        ALTER TABLE gv_search_run
            ADD CONSTRAINT fk_gv_search_run_task
            FOREIGN KEY (task_id)
            REFERENCES gv_analysis_task(task_id)
            ON DELETE RESTRICT
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gv_search_run_result_run'
    ) THEN
        ALTER TABLE gv_search_run_result
            ADD CONSTRAINT fk_gv_search_run_result_run
            FOREIGN KEY (search_run_id)
            REFERENCES gv_search_run(search_run_id)
            ON DELETE RESTRICT
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gv_analysis_candidate_task'
    ) THEN
        ALTER TABLE gv_analysis_candidate
            ADD CONSTRAINT fk_gv_analysis_candidate_task
            FOREIGN KEY (task_id)
            REFERENCES gv_analysis_task(task_id)
            ON DELETE RESTRICT
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gv_query_rewrite_task'
    ) THEN
        ALTER TABLE gv_query_rewrite
            ADD CONSTRAINT fk_gv_query_rewrite_task
            FOREIGN KEY (task_id)
            REFERENCES gv_analysis_task(task_id)
            ON DELETE RESTRICT
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gv_query_rewrite_candidate'
    ) THEN
        ALTER TABLE gv_query_rewrite
            ADD CONSTRAINT fk_gv_query_rewrite_candidate
            FOREIGN KEY (candidate_id)
            REFERENCES gv_analysis_candidate(candidate_id)
            ON DELETE RESTRICT
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gv_candidate_match_task'
    ) THEN
        ALTER TABLE gv_candidate_match
            ADD CONSTRAINT fk_gv_candidate_match_task
            FOREIGN KEY (task_id)
            REFERENCES gv_analysis_task(task_id)
            ON DELETE RESTRICT
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gv_candidate_match_candidate'
    ) THEN
        ALTER TABLE gv_candidate_match
            ADD CONSTRAINT fk_gv_candidate_match_candidate
            FOREIGN KEY (candidate_id)
            REFERENCES gv_analysis_candidate(candidate_id)
            ON DELETE RESTRICT
            NOT VALID;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gv_analysis_report_task'
    ) THEN
        ALTER TABLE gv_analysis_report
            ADD CONSTRAINT fk_gv_analysis_report_task
            FOREIGN KEY (task_id)
            REFERENCES gv_analysis_task(task_id)
            ON DELETE RESTRICT
            NOT VALID;
    END IF;
END $$;
