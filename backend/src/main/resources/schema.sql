CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS gv_product_embedding (
    embedding_id VARCHAR(128) PRIMARY KEY,
    platform VARCHAR(32) NOT NULL,
    external_item_id VARCHAR(128) NOT NULL,
    indexed_text TEXT NOT NULL,
    embedding vector(1024) NOT NULL,
    metadata_jsonb JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (platform, external_item_id)
);

CREATE INDEX IF NOT EXISTS idx_gv_product_embedding_platform
    ON gv_product_embedding (platform, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_gv_product_embedding_vector
    ON gv_product_embedding USING hnsw (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS product_detail_snapshot (
    product_id VARCHAR(128) PRIMARY KEY,
    platform VARCHAR(32) NOT NULL,
    title TEXT NOT NULL,
    price NUMERIC(12, 2),
    brand TEXT,
    image TEXT,
    link TEXT,
    description TEXT,
    gallery_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    attributes_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    sku_data_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_data_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS gv_analysis_task (
    task_id VARCHAR(64) PRIMARY KEY,
    parent_task_id VARCHAR(64),
    phase VARCHAR(32) NOT NULL,
    keyword TEXT NOT NULL,
    constraints_jsonb JSONB NOT NULL DEFAULT '[]'::jsonb,
    market VARCHAR(32),
    requested_limit INTEGER,
    target_profit_margin NUMERIC(12, 4),
    status VARCHAR(32) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    selected_candidate_id VARCHAR(128),
    selected_product_id VARCHAR(128),
    report_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gv_analysis_task_keyword_created_at
    ON gv_analysis_task (keyword, created_at DESC);

CREATE TABLE IF NOT EXISTS gv_analysis_task_log (
    task_id VARCHAR(64) NOT NULL,
    seq_no INTEGER NOT NULL,
    log_at TIMESTAMPTZ NOT NULL,
    stage VARCHAR(128) NOT NULL,
    level VARCHAR(16) NOT NULL,
    message TEXT NOT NULL,
    source VARCHAR(64),
    PRIMARY KEY (task_id, seq_no)
);

CREATE INDEX IF NOT EXISTS idx_gv_analysis_task_log_task_id
    ON gv_analysis_task_log (task_id, seq_no ASC);

CREATE TABLE IF NOT EXISTS gv_platform_item (
    platform VARCHAR(32) NOT NULL,
    external_item_id VARCHAR(128) NOT NULL,
    title TEXT NOT NULL,
    price NUMERIC(12, 2),
    rating DOUBLE PRECISION,
    reviews INTEGER,
    image TEXT,
    link TEXT,
    attributes_jsonb JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (platform, external_item_id)
);

CREATE INDEX IF NOT EXISTS idx_gv_platform_item_title_trgm
    ON gv_platform_item USING gin (LOWER(title) gin_trgm_ops);

CREATE TABLE IF NOT EXISTS gv_platform_item_snapshot (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    platform VARCHAR(32) NOT NULL,
    external_item_id VARCHAR(128) NOT NULL,
    query_text TEXT,
    title TEXT NOT NULL,
    price NUMERIC(12, 2),
    rating DOUBLE PRECISION,
    reviews INTEGER,
    image TEXT,
    link TEXT,
    raw_jsonb JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gv_platform_item_snapshot_lookup
    ON gv_platform_item_snapshot (platform, external_item_id, created_at DESC);

CREATE TABLE IF NOT EXISTS gv_search_run (
    search_run_id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    phase VARCHAR(32) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    query_text TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gv_search_run_query_text_created_at
    ON gv_search_run (platform, query_text, created_at DESC);

CREATE TABLE IF NOT EXISTS gv_search_run_result (
    search_run_id VARCHAR(64) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    external_item_id VARCHAR(128) NOT NULL,
    rank_no INTEGER NOT NULL,
    title TEXT NOT NULL,
    price NUMERIC(12, 2),
    image TEXT,
    link TEXT,
    raw_jsonb JSONB NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (search_run_id, platform, external_item_id)
);

CREATE INDEX IF NOT EXISTS idx_gv_search_run_result_rank
    ON gv_search_run_result (search_run_id, rank_no ASC);

CREATE TABLE IF NOT EXISTS gv_analysis_candidate (
    candidate_id VARCHAR(128) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    external_item_id VARCHAR(128) NOT NULL,
    title TEXT NOT NULL,
    price NUMERIC(12, 2),
    image TEXT,
    score NUMERIC(12, 2),
    risk_tag VARCHAR(64),
    recommendation_reason TEXT,
    suggest_second_phase BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'READY',
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gv_analysis_candidate_task_id
    ON gv_analysis_candidate (task_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_gv_analysis_candidate_external_item
    ON gv_analysis_candidate (external_item_id, created_at DESC);

CREATE TABLE IF NOT EXISTS gv_query_rewrite (
    rewrite_id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64),
    candidate_id VARCHAR(128),
    source_product_id VARCHAR(128),
    source_text TEXT NOT NULL,
    rewritten_text TEXT NOT NULL,
    keywords_jsonb JSONB NOT NULL DEFAULT '[]'::jsonb,
    gateway_source VARCHAR(64),
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gv_query_rewrite_source_text
    ON gv_query_rewrite (source_text, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_gv_query_rewrite_task_id
    ON gv_query_rewrite (task_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_gv_query_rewrite_candidate_id
    ON gv_query_rewrite (candidate_id, created_at DESC);

CREATE TABLE IF NOT EXISTS gv_candidate_match (
    match_id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64),
    candidate_id VARCHAR(128) NOT NULL,
    source_product_id VARCHAR(128),
    platform VARCHAR(32) NOT NULL,
    external_item_id VARCHAR(128) NOT NULL,
    title TEXT NOT NULL,
    price NUMERIC(12, 2),
    similarity_score NUMERIC(12, 4),
    match_source VARCHAR(64),
    search_keyword TEXT,
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_reason TEXT,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gv_candidate_match_candidate
    ON gv_candidate_match (candidate_id, similarity_score DESC);

CREATE INDEX IF NOT EXISTS idx_gv_candidate_match_task
    ON gv_candidate_match (task_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_gv_candidate_match_source_product
    ON gv_candidate_match (source_product_id, similarity_score DESC);

CREATE TABLE IF NOT EXISTS gv_analysis_report (
    report_id VARCHAR(128) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    estimated_profit NUMERIC(12, 2),
    estimated_margin NUMERIC(12, 4),
    report_markdown TEXT,
    report_jsonb JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
