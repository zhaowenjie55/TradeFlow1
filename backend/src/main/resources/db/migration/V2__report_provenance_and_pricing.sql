CREATE TABLE IF NOT EXISTS gv_pricing_config (
    version VARCHAR(32) PRIMARY KEY,
    fx_rate NUMERIC(12, 4) NOT NULL,
    logistics_rate NUMERIC(12, 4) NOT NULL,
    platform_fee_rate NUMERIC(12, 4) NOT NULL,
    exchange_loss_rate NUMERIC(12, 4) NOT NULL,
    fallback_sourcing_rate NUMERIC(12, 4) NOT NULL,
    default_domestic_shipping NUMERIC(12, 2) NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    effective_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO gv_pricing_config (
    version,
    fx_rate,
    logistics_rate,
    platform_fee_rate,
    exchange_loss_rate,
    fallback_sourcing_rate,
    default_domestic_shipping
) VALUES (
    'v0',
    7.20,
    0.12,
    0.15,
    0.03,
    0.45,
    6.00
)
ON CONFLICT (version) DO NOTHING;

ALTER TABLE gv_analysis_report
    ADD COLUMN IF NOT EXISTS rewrite_provider VARCHAR(64),
    ADD COLUMN IF NOT EXISTS rewrite_model VARCHAR(128),
    ADD COLUMN IF NOT EXISTS retrieval_source VARCHAR(64),
    ADD COLUMN IF NOT EXISTS detail_source VARCHAR(64),
    ADD COLUMN IF NOT EXISTS fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS fallback_reason TEXT,
    ADD COLUMN IF NOT EXISTS llm_provider VARCHAR(64),
    ADD COLUMN IF NOT EXISTS llm_model VARCHAR(128),
    ADD COLUMN IF NOT EXISTS quality_tier VARCHAR(64),
    ADD COLUMN IF NOT EXISTS pricing_config_version VARCHAR(32);

UPDATE gv_analysis_report
SET
    rewrite_provider = COALESCE(rewrite_provider, report_jsonb #>> '{analysisTrace,rewrite,provider}'),
    rewrite_model = COALESCE(rewrite_model, report_jsonb #>> '{auditData,rewriteModel}'),
    retrieval_source = COALESCE(retrieval_source, report_jsonb #>> '{analysisTrace,retrieval,matchSource}'),
    detail_source = COALESCE(detail_source, report_jsonb #>> '{domesticMatches,0,detailSource}'),
    llm_provider = COALESCE(llm_provider, report_jsonb #>> '{analysisTrace,llm,provider}'),
    llm_model = COALESCE(llm_model, report_jsonb #>> '{analysisTrace,llm,model}'),
    pricing_config_version = COALESCE(pricing_config_version, 'v0');

UPDATE gv_analysis_report
SET fallback_used = (
    COALESCE((report_jsonb #>> '{auditData,rewriteFallbackUsed}')::BOOLEAN, FALSE)
    OR COALESCE((report_jsonb #>> '{auditData,retrievalFallbackUsed}')::BOOLEAN, FALSE)
    OR COALESCE((report_jsonb #>> '{auditData,narrativeFallbackUsed}')::BOOLEAN, FALSE)
    OR COALESCE(retrieval_source, '') LIKE '%FALLBACK%'
    OR COALESCE(retrieval_source, '') LIKE '%CATALOG%'
    OR COALESCE(detail_source, '') LIKE '%SNAPSHOT%'
    OR COALESCE(detail_source, '') = 'SEARCH_RESULT_ONLY'
)
WHERE fallback_used = FALSE;

UPDATE gv_analysis_report
SET fallback_reason = NULLIF(CONCAT_WS(
        '; ',
        report_jsonb #>> '{auditData,rewriteFallbackReason}',
        report_jsonb #>> '{auditData,retrievalFallbackReason}',
        report_jsonb #>> '{auditData,narrativeFallbackReason}'
    ), '')
WHERE fallback_reason IS NULL;

UPDATE gv_analysis_report
SET quality_tier = CASE
    WHEN COALESCE((report_jsonb #>> '{auditData,narrativeFallbackUsed}')::BOOLEAN, FALSE) THEN 'LLM_FALLBACK_ASSISTED'
    WHEN COALESCE(retrieval_source, '') LIKE '%REALTIME%'
         AND COALESCE(detail_source, '') = 'DOMESTIC_REALTIME_DETAIL'
         AND NOT COALESCE((report_jsonb #>> '{auditData,rewriteFallbackUsed}')::BOOLEAN, FALSE)
         AND NOT COALESCE((report_jsonb #>> '{auditData,retrievalFallbackUsed}')::BOOLEAN, FALSE)
    THEN 'REALTIME_CONFIRMED'
    WHEN COALESCE(retrieval_source, '') LIKE '%REALTIME%' THEN 'REALTIME_HYBRID'
    ELSE 'SNAPSHOT_FALLBACK'
END
WHERE quality_tier IS NULL;

ALTER TABLE gv_analysis_report
    ALTER COLUMN pricing_config_version SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gv_analysis_report_pricing_config'
    ) THEN
        ALTER TABLE gv_analysis_report
            ADD CONSTRAINT fk_gv_analysis_report_pricing_config
            FOREIGN KEY (pricing_config_version)
            REFERENCES gv_pricing_config (version);
    END IF;
END
$$;
