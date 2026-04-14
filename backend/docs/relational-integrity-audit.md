# Relational Integrity Audit

This backend now exposes a lightweight integrity audit endpoint:

- `GET /api/admin/integrity/relational`

It returns:

- orphan counts for the task/report/search/candidate relationships covered by `V3` and `V4`
- validation status for the foreign keys introduced during the Flyway rollout
- an `allClear` summary flag

Purpose:

- stop relying on one-off SQL shell checks when validating the FK migration rollout
- make orphan detection repeatable after imports, manual DB repair, or migration work
- provide a stable app-level check before validating or tightening constraints further

Current audited relationships:

- `gv_search_run_result.search_run_id -> gv_search_run.search_run_id`
- `gv_query_rewrite.task_id -> gv_analysis_task.task_id`
- `gv_query_rewrite.candidate_id -> gv_analysis_candidate.candidate_id`
- `gv_candidate_match.task_id -> gv_analysis_task.task_id`
- `gv_candidate_match.candidate_id -> gv_analysis_candidate.candidate_id`
- `gv_analysis_task_log.task_id -> gv_analysis_task.task_id`
- `gv_analysis_report.task_id -> gv_analysis_task.task_id`
- `gv_analysis_candidate.task_id -> gv_analysis_task.task_id`
- `gv_search_run.task_id -> gv_analysis_task.task_id`
- `gv_analysis_report.pricing_config_version -> gv_pricing_config.version`

This endpoint is intended as an internal operational surface. It does not mutate data.
