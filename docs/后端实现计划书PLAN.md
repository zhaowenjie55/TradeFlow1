# TradeFlow 后端实现计划：AmazonUS -> 1688 强制兜底 + GLM + BGE-M3

## Summary
- 在现有两阶段任务骨架上完成一条稳定的 demo 后端链路：一阶段固定从 Amazon 历史搜索快照返回候选，二阶段通过 GLM 通用模型完成标题改写与报告叙事，通过 PostgreSQL pgvector + BGE-M3 完成 1688 语义召回与历史商品兜底，最终统一按人民币输出套利报告。
- Amazon、1688 的实时商品接口本轮不接入，全部由后端开关强制进入受控兜底。
- GLM API Key 不写入代码或配置文件，统一通过环境变量 `LLM_API_KEY` 注入；实现时默认使用你提供的 Key 对应环境，但仓库内不保留明文。

## Key Changes
### 1. 配置与公共接口
- 扩展 `app.integration.overseas`：
  - `enabled`
  - `force-fallback=true`
  - `search-endpoint`
  - `api-key`
- 扩展 `app.integration.domestic`：
  - `enabled`
  - `force-fallback=true`
  - `search-endpoint`
  - `detail-endpoint`
  - `api-key`
- 重构 `app.integration.llm`：
  - `enabled=true`
  - `force-simulated=false`
  - `chat-endpoint=https://open.bigmodel.cn/api/paas/v4/chat/completions`
  - `model=${LLM_MODEL:glm-5}`
  - `api-key=${LLM_API_KEY:}`
  - `temperature=${LLM_TEMPERATURE:0.2}`
- 新增 `app.pricing`：
  - `usd-to-cny-rate=7.20`
  - `cross-border-logistics-rate=0.12`
  - `platform-fee-rate=0.15`
  - `exchange-loss-rate=0.03`
  - `fallback-sourcing-rate=0.45`
- 重构 `app.vector`：
  - `provider=${VECTOR_PROVIDER:local-bge-m3}`
  - `model-path=${VECTOR_MODEL_PATH:}`
  - `tokenizer-path=${VECTOR_TOKENIZER_PATH:}`
  - `pooling-mode=${VECTOR_POOLING_MODE:CLS}`
  - `dimension=${VECTOR_DIMENSION:1024}`
  - `domestic-platform=TAOBAO`
  - `fixed-keyword=亚克力透明收纳架`
  - `bootstrap-on-startup=true`
- HTTP API 路径保持不变。
- 报告响应的 `costBreakdown` 新增 `domesticShippingCost`；现有成本、利润、售价字段从本轮开始统一按人民币解释。

### 2. 路由层与开关行为
- `RoutingOverseasMarketplaceGateway`：
  - `enabled=false` 或 `force-fallback=true` 时直接抛受控异常，不走 simulated 商品网关。
  - 由 `DiscoveryPhase1Workflow` 捕获异常并进入 `SearchHistoryFallbackService`。
- `RoutingDomesticMarketplaceGateway`：
  - `enabled=false` 或 `force-fallback=true` 时直接抛受控异常，不走 simulated 国内商品网关。
  - 商品搜索由 `DomesticMatchService` 回退到 `DomesticProductFallbackService.searchHistoricalProducts(...)`。
  - 详情读取由 `SourcingPhase2Workflow.resolveDomesticDetail()` 回退到 `product_detail_snapshot`。
- `RoutingLLMGateway`：
  - `enabled=true` 且 `force-simulated=false` 时调用真实 GLM。
  - `enabled=false` 或 `force-simulated=true` 时调用 `SimulatedLLMGateway`。
  - 真实 GLM 调用失败、空响应、非 JSON 响应时自动回退 `SimulatedLLMGateway`。

### 3. GLM 标题改写与报告叙事
- `HttpLLMGateway` 统一只使用一个 GLM Chat Completions 端点，不再依赖分离的 rewrite/analysis 专用接口。
- 标题改写调用：
  - system prompt 指定任务是“将 Amazon 英文标题改写为适合 1688 搜索的中文标题与扩展词”。
  - user 输入只传原始标题。
  - 模型必须输出严格 JSON：`rewritten_text`、`keywords`。
- 报告叙事调用：
  - system prompt 指定任务是“根据结构化套利上下文生成摘要、建议、风险说明”。
  - user 输入传结构化上下文：商品标题、改写词、国内候选标题、结论、风险等级、人民币成本与利润等。
  - 模型必须输出严格 JSON：`summary_text`、`recommendations`、`risk_notes`。
- 响应解析策略固定：
  - 从 `choices[0].message.content` 取文本。
  - 去掉 Markdown 代码块包裹。
  - 解析为 JSON。
  - 若 JSON 缺关键字段或字段为空，则抛异常进入 simulated fallback。
- `QueryRewriteService` 继续负责写 `gv_query_rewrite`，并在保存前保证 `keywords_jsonb` 一定包含 `亚克力透明收纳架`。
- 报告叙事中 `provider` 固定记录为 `GLM_CHAT`；fallback 时记录 `SIMULATED_LLM`。

### 4. BGE-M3 本地向量化与 pgvector
- 向量模型改为本地 BGE-M3，不再使用 `BgeSmallZhV15EmbeddingModelFactory`。
- `LangChain4jVectorConfig` 改为加载本地 ONNX 模型与 tokenizer：
  - 从 `VECTOR_MODEL_PATH` 读取 `model.onnx`
  - 从 `VECTOR_TOKENIZER_PATH` 读取 tokenizer 文件
  - pooling 固定为 `CLS`
- 若模型路径为空或文件不存在，应用启动直接失败，并给出明确错误：“BGE-M3 model/tokenizer not configured”。
- `schema.sql` 中 `gv_product_embedding.embedding` 改为 `vector(1024)`。
- `ProductEmbeddingRepository` 继续使用现有表与 HNSW 索引，不新增表。
- `DomesticVectorIndexBootstrapper` 保持启动时重建索引：
  - 从国内商品池读取 1688 商品
  - 生成 embedding
  - 写入 `gv_product_embedding`
- `DomesticVectorSearchService` 检索顺序固定：
  - 对每个搜索词先向量召回
  - 再尝试实时国内商品网关
  - 最后走历史商品池兜底
- BGE-M3 查询文本与索引文本策略固定：
  - 索引文本：`商品标题 + 固定关键词 + 属性键值对`
  - 查询文本：`改写主词或扩展词 + 固定关键词`
  - 不加特定检索前缀

### 5. 一阶段：Amazon 历史快照发现
- 保持 `POST /api/analysis/tasks`、`GET /status`、`GET /candidates` 不变。
- `AnalysisTaskController` 继续固定市场为 `AMAZON`。
- `Phase1TaskApplicationService` / `Phase1TaskProcessor` 维持现有异步结构，状态流转固定：
  - `CREATED -> RUNNING -> WAITING_USER_SELECTION`
- `DiscoveryPhase1Workflow` 保持“先调 `syncAmazonProducts`，失败后 fallback”的结构，但默认配置下会稳定进入 `SearchHistoryFallbackService.findLatestAmazonProducts()`。
- 读取来源固定为：
  - `gv_search_run`
  - `gv_search_run_result`
- 候选仍写入 `gv_analysis_candidate`。
- 每次运行都新建一条 `gv_search_run`，并把本次实际展示结果写入新的 `gv_search_run_result`。
- 新 run 的 `status=FALLBACK`、`fallback_used=true`；种子 search run 不覆盖。

### 6. 二阶段：1688 改写、召回、匹配、详情
- 保持 `POST /api/analysis/tasks/{taskId}/selection` 不变。
- `Phase2TaskApplicationService` 校验候选归属后创建 PHASE2 子任务。
- `Phase2TaskProcessor` 状态流转固定：
  - `RUNNING -> ANALYZING_SOURCE -> REPORT_READY`
- `DomesticMatchService` 的匹配顺序固定为：
  - 对 `rewritten_text`
  - 对固定词 `亚克力透明收纳架`
  - 对去重后的扩展词
- 每个搜索词执行固定流程：
  - 先 `DomesticVectorSearchService.search()`
  - 再 `ProductCatalogSyncService.syncDomesticKeywordProducts(term)`
  - 失败或空结果时 `DomesticProductFallbackService.searchHistoricalProducts(MarketplaceType.TAOBAO, term, limit)`
- 合并候选后统一排序，评分固定由四部分组成：
  - 标题词重叠得分
  - 改写词覆盖得分
  - 价格合理性得分
  - 向量召回命中加权分
- 排序结果写入 `gv_candidate_match`。
- Top1 用于详情补齐与成本分析，Top3 用于报告展示。
- `SourcingPhase2Workflow.resolveDomesticDetail()` 保持当前结构，但默认回退读取 `product_detail_snapshot`。
- 内部平台枚举继续保留 `MarketplaceType.TAOBAO` 作为 1688 兼容标识，本轮不做全量重命名。

### 7. 人民币统一测算与 Markdown 报告
- `ProductAnalysisService` 改为先读取 Amazon 原始美元价格：
  - 优先从 `candidate.overseasPrice`
  - 同时在 `raw_data_json` 中保留 `priceAmountUsd`
- 换算流程固定：
  - `amazonPriceRmb = amazonPriceUsd * usdToCnyRate`
- 国内采购与运费固定规则：
  - `sourcingCost = 1688 Top1 price`
  - `domesticShippingCost = 解析 shippingText`
  - `shippingText` 优先从 `raw_data_json`，其次 `attributes_jsonb`
  - 解析失败时 `domesticShippingCost = 0`
- 其他成本固定公式：
  - `logisticsCost = amazonPriceRmb * cross-border-logistics-rate`
  - `platformFee = amazonPriceRmb * platform-fee-rate`
  - `exchangeRateCost = amazonPriceRmb * exchange-loss-rate`
  - `totalCost = sourcingCost + domesticShippingCost + logisticsCost + platformFee + exchangeRateCost`
  - `estimatedProfit = amazonPriceRmb - totalCost`
  - `expectedMargin = estimatedProfit / amazonPriceRmb * 100`
- 报告输出固定为人民币语义：
  - Markdown 金额使用 `¥`
  - `report_jsonb` 中保留原始美元值、汇率、shippingText 作为审计字段
- `ReportAggregateService` 继续保存 Markdown 与 JSON 聚合，不新增表。

## Test Plan
- 启动测试：
  - `tradeflow` 数据库存在且安装了 `pgvector` 时，应用可启动。
  - `schema.sql` 与 `data.sql` 自动执行成功。
  - `gv_product_embedding` 可被重建。
- GLM 测试：
  - 模拟 GLM 正常返回 JSON，标题改写可写入 `gv_query_rewrite`。
  - 模拟 GLM 返回代码块 JSON、空 JSON、非 JSON 时，自动 fallback 到 `SimulatedLLMGateway`。
- BGE-M3 测试：
  - 模型路径和 tokenizer 路径存在时，向量索引构建成功。
  - 路径缺失时，启动失败并给出明确错误。
  - `gv_product_embedding.embedding` 维度为 1024。
- 一阶段集成：
  - 提交 `Acrylic Desktop Organizer`、`limit=9` 后，任务进入 `WAITING_USER_SELECTION`。
  - `gv_search_run` 新增一条 `FALLBACK` 记录。
  - `gv_analysis_candidate` 生成 9 条候选。
- 二阶段集成：
  - 选择 `amz-acrylic-01` 后，`gv_query_rewrite.keywords_jsonb` 包含 `亚克力透明收纳架`。
  - `gv_candidate_match` 写入候选匹配记录。
  - Top1 能从 `product_detail_snapshot` 补齐详情。
  - `gv_analysis_report` 成功生成，Markdown 中金额使用 `¥`。
- 计价测试：
  - 固定输入验证 `USD -> CNY`、国内运费解析、总成本、利润率公式。
  - 无 `shippingText` 时 `domesticShippingCost=0`。
- 回归测试：
  - 历史任务、候选列表、报告列表接口不回归。
  - 当未来 `force-fallback=false` 且启用真实商品接口时，原实时路径仍可工作。

## Assumptions
- GLM 默认模型使用 `glm-5`，但实现必须允许通过 `LLM_MODEL` 环境变量覆盖为其他通用模型。
- 你提供的 GLM API Key 只通过 `LLM_API_KEY` 环境变量注入，不进入仓库、日志或种子数据。
- BGE-M3 模型与 tokenizer 将由你后续提供到本地；当前实现按“本地 ONNX 路径可配置”落地。
- 本轮只做后端，不处理前端金额符号与页面文案收口。
- 内部 `TAOBAO` 平台枚举继续作为 1688 兼容标识使用，不做全量重命名。
