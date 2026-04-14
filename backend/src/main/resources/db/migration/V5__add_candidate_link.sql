-- Add overseas product link to candidate snapshot so the report can surface a direct URL.
ALTER TABLE gv_analysis_candidate
    ADD COLUMN IF NOT EXISTS link TEXT;
