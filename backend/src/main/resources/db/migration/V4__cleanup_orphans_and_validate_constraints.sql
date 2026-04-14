-- Clean up rows that cannot satisfy the relational model and then validate the
-- foreign keys introduced in V3. These rows are already unusable because their
-- parent record is missing.

DELETE FROM gv_search_run_result r
WHERE NOT EXISTS (
    SELECT 1
    FROM gv_search_run s
    WHERE s.search_run_id = r.search_run_id
);

DELETE FROM gv_search_run_result r
WHERE EXISTS (
    SELECT 1
    FROM gv_search_run s
    WHERE s.search_run_id = r.search_run_id
      AND NOT EXISTS (
          SELECT 1
          FROM gv_analysis_task t
          WHERE t.task_id = s.task_id
      )
);

DELETE FROM gv_query_rewrite q
WHERE (q.task_id IS NOT NULL AND NOT EXISTS (
           SELECT 1
           FROM gv_analysis_task t
           WHERE t.task_id = q.task_id
       ))
   OR (q.candidate_id IS NOT NULL AND NOT EXISTS (
           SELECT 1
           FROM gv_analysis_candidate c
           WHERE c.candidate_id = q.candidate_id
       ));

DELETE FROM gv_candidate_match m
WHERE (m.task_id IS NOT NULL AND NOT EXISTS (
           SELECT 1
           FROM gv_analysis_task t
           WHERE t.task_id = m.task_id
       ))
   OR (m.candidate_id IS NOT NULL AND NOT EXISTS (
           SELECT 1
           FROM gv_analysis_candidate c
           WHERE c.candidate_id = m.candidate_id
       ));

DELETE FROM gv_analysis_task_log l
WHERE NOT EXISTS (
    SELECT 1
    FROM gv_analysis_task t
    WHERE t.task_id = l.task_id
);

DELETE FROM gv_analysis_report r
WHERE NOT EXISTS (
    SELECT 1
    FROM gv_analysis_task t
    WHERE t.task_id = r.task_id
);

DELETE FROM gv_analysis_candidate c
WHERE NOT EXISTS (
    SELECT 1
    FROM gv_analysis_task t
    WHERE t.task_id = c.task_id
);

DELETE FROM gv_search_run s
WHERE NOT EXISTS (
    SELECT 1
    FROM gv_analysis_task t
    WHERE t.task_id = s.task_id
);

ALTER TABLE gv_analysis_task_log
    VALIDATE CONSTRAINT fk_gv_analysis_task_log_task;

ALTER TABLE gv_search_run
    VALIDATE CONSTRAINT fk_gv_search_run_task;

ALTER TABLE gv_search_run_result
    VALIDATE CONSTRAINT fk_gv_search_run_result_run;

ALTER TABLE gv_analysis_candidate
    VALIDATE CONSTRAINT fk_gv_analysis_candidate_task;

ALTER TABLE gv_query_rewrite
    VALIDATE CONSTRAINT fk_gv_query_rewrite_task;

ALTER TABLE gv_query_rewrite
    VALIDATE CONSTRAINT fk_gv_query_rewrite_candidate;

ALTER TABLE gv_candidate_match
    VALIDATE CONSTRAINT fk_gv_candidate_match_task;

ALTER TABLE gv_candidate_match
    VALIDATE CONSTRAINT fk_gv_candidate_match_candidate;

ALTER TABLE gv_analysis_report
    VALIDATE CONSTRAINT fk_gv_analysis_report_task;
