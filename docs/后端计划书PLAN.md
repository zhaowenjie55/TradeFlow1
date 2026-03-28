# TradeFlow Agent-First MVP 完整计划书（含 Excel 种子数据入库、PostgreSQL/pgvector、可解释匹配与风险决策）

## 1. Executive Summary
TradeFlow 本轮不是做一个“能跑通页面的搜索后端”，而是做一个可演示、可解释、可扩展的 AI Trade Decision Agent。前端交互保持现状不变，但后端第二阶段要升级为“Agent 先理解商品，再规划检索，再做召回、评分、风险判断和推荐”的决策链路。

这份完整计划把三件事合并为一条正式主线：
- 现有两阶段前端交互与外部 API 不变
- PostgreSQL + pgvector 作为正式主链路，`dev-memory` 作为本地演示 profile
- 你的 Excel 作为首批“种子样本库 + 标注基线 + 向量召回评估集”接入系统

TradeFlow 的核心不是找到最像的国内商品，而是：
- 在一对多候选里做决策
- 解释为什么匹配
- 说明哪里有风险
- 判断是否值得进入
- 给出带置信度的建议

## 2. Architecture Decisions
- 保留人工选品，不改前端两阶段交互。
  前端仍是：创建海外搜索任务 -> 展示海外候选 -> 用户手动选中 -> 进入二阶段分析。
- 向量能力首期只落国内侧。
  海外阶段仍然是实时搜索 + 历史快照兜底；国内阶段才做 live search + pgvector 历史召回 + 一对多映射。
- 匹配必须是多层机制。
  固定采用 `rule filter + vector recall + agent rerank + profit/risk decision`，禁止把候选直接扔给 LLM 选一个。
- 第二阶段必须有 `AnalysisPlan`。
  Agent 先理解商品并生成检索与判断计划，后续所有步骤都以它为输入。
- Excel 不是临时 demo 文件，而是长期种子数据源。
  它既提供快照数据，也提供人工匹配样本和负样本，能直接用于评估向量召回和 rerank 质量。
- 正式主链路采用 PostgreSQL + pgvector，开发保留 `dev-memory`。
  正式环境默认走数据库；本地 hackathon 演示允许不接库快速跑通。

## 3. End-to-End Flow
### Phase1：海外商品发现与展示
1. `POST /api/analysis/tasks`
2. 创建 Phase1 任务，状态 `CREATED -> QUEUED -> RUNNING`
3. `OverseasSearchService` 实时搜索海外商品
4. 若实时失败，则查 `overseas_product_snapshot` 历史快照兜底
5. `Phase1Agent` 对结果做候选筛选与推荐理由生成
6. 结果写入 `overseas_product_snapshot` 和 `candidate_snapshot`
7. 任务进入 `WAITING_USER_SELECTION`

### Phase2：Agent 决策、国内召回、匹配、分析
1. `POST /api/analysis/tasks/{taskId}/selection`
2. 创建 Phase2 任务，状态 `CREATED -> ANALYZING_SOURCE`
3. `ProductUnderstandingService` 基于海外商品生成 `AnalysisPlan`
4. `DomesticQueryPlanningService` 输出多层 query：
   - `canonicalQueries`
   - `commercialQueries`
   - `broadRecallQueries`
   - `negativeTerms`
5. `DomesticLiveSearchService` 用 `canonical + commercial` 做国内实时搜索
6. `DomesticVectorRecallService` 用 `AnalysisPlan.queryEmbeddingText` 在 `domestic_product_snapshot.embedding` 上做 pgvector TopK 召回
7. `CandidateMergeService` 合并 live search 与 vector recall 结果，做 exact dedup / near dedup / variant grouping
8. `CandidateRerankService` 对候选做 hard filter、分数计算和 Agent 重排序
9. `RiskAssessmentService` 产出结构化风险项
10. `ProfitAnalysisService` 计算成本、运费、利润、利润率
11. `ReportGenerationService` 生成结构化报告与 Markdown
12. 状态变更为 `REPORT_READY`

### 数据闭环
- 海外实时搜索成功结果落库
- 国内实时搜索成功结果落库
- 标准化画像落库
- embedding 落库
- 一对多映射落库
- 报告落库
- 步骤轨迹落库

## 4. Agent Decision Layer
### AnalysisPlan
新增 `AnalysisPlan`，由 Phase2 开始时生成，并通过状态接口返回摘要。固定字段：
- `canonicalTitleZh`
- `normalizedCategory`
- `coreFunctions: string[]`
- `keySpecs: Record<string, string>`
- `material`
- `capacity`
- `bundleInfo`
- `targetUsers: string[]`
- `mustMatchFields: string[]`
- `preferredMatchFields: string[]`
- `expandableFields: string[]`
- `primaryQueries: string[]`
- `commercialQueries: string[]`
- `broadRecallQueries: string[]`
- `negativeTerms: string[]`
- `riskSensitivity: low | medium | high`
- `executionPlan: string[]`
- `reasoningSummary`
- `queryEmbeddingText`
- `normalizedOverseasDoc`

### 生成原则
- 输入是海外商品标题、价格、描述、图片、来源链接、类目、属性
- 输出必须结构化，不接受自由文本方案
- `reasoningSummary` 直接给前端 Thinking Log 展示
- `executionPlan` 直接映射到 pipeline step 的 `whyThisStep`

## 5. Retrieval and Matching Design
### Query Rewrite 四层
- `Canonical Query`
  - 目的：精确表达商品本体
  - 用于第一轮国内实时搜索
- `Commercial Query`
  - 目的：贴近 1688 标题风格，提升召回
  - 用于第二轮扩展搜索
- `Broad Recall Query`
  - 目的：扩大历史相似商品召回
  - 用于向量召回和历史兜底
- `Negative Terms`
  - 目的：排除品牌词、替换件、滤芯、配件、误导词、仿牌词
  - 用于 live search 过滤和 rerank 扣分

### Normalized Product Profile
海外和国内商品都先生成标准化画像，再做 embedding。字段：
- `profileId`
- `sourceType`
- `sourceId`
- `normalizedCategory`
- `coreFunctions`
- `material`
- `capacity`
- `bundleInfo`
- `targetUsers`
- `excludedSemantics`
- `normalizedAttributes`
- `normalizedDoc`
- `embeddingVersion`

### Embedding 策略
- 不直接用原始标题
- `normalizedDoc` 模板统一，去营销词、统一单位、保留核心功能和关键规格
- 海外 query embedding：
  - 来自 `AnalysisPlan.queryEmbeddingText`
- 国内 snapshot embedding：
  - 来自 `normalized_product_profile.normalizedDoc`
- 支持 `embeddingVersion` 升级后的 re-embedding

### 候选合并与变体归组
- Exact Dedup
  - 相同 `platformProductId`
  - 相同 `detailUrl`
- Near Dedup
  - 标题相似度高
  - 价格接近
  - 核心规格一致
- Variant Grouping
  - 同系列不同容量/材质/套装/SKU 保留为变体，不误删
- 输出分层
  - `primaryRecommendation`
  - `alternatives`
  - `variants`

## 6. Scoring and Risk Design
### Hard Filters
任一命中直接淘汰：
- 类目不一致
- 核心功能不一致
- 关键规格差异过大
- 高品牌/IP 风险
- 候选明显是配件/替换件，而不是完整商品

### Soft Penalties
- 材质不同：`-8`
- 容量不同：`-8`
- bundle 数量不同：`-6`
- 参数不完整：`-5`
- 运费不确定：`-4`
- 文案模糊：`-4`

### Scores
所有分数范围 `0-100`
- `ruleScore`
- `vectorScore`
- `agentScore`
- `profitScore`
- `finalScore`
- `confidenceScore`

### Final Score
`finalScore = 0.30 * ruleScore + 0.35 * vectorScore + 0.25 * agentScore + 0.10 * profitScore - softPenaltyTotal`

### Confidence Score
`confidenceScore = 0.45 * finalScore + 0.20 * sourceReliabilityScore + 0.15 * specCompletenessScore + 0.10 * candidateConsistencyScore - 0.10 * riskPenaltyScore`

### Source Reliability
- `LIVE_SEARCH = 95`
- `VECTOR_RECALL_RECENT = 82`
- `VECTOR_RECALL_HISTORICAL = 70`
- `HISTORICAL_FALLBACK_ONLY = 58`

### Recommendation Decision
- `RECOMMEND`
  - `finalScore >= 78`
  - `confidenceScore >= 72`
  - 无阻断型高风险
- `CAUTIOUS`
  - `finalScore >= 60`
  - 无 hard reject
  - 或利润不错但风险偏高
- `NOT_RECOMMENDED`
  - hard reject
  - 或 `finalScore < 60`
  - 或风险不可接受

### Risk Taxonomy
每个风险项固定字段：
- `riskType`
- `riskLevel`
- `evidence`
- `suggestion`
- `blocking`

固定风险大类：
- `IP_BRAND_RISK`
- `SPEC_MISMATCH_RISK`
- `COMPLIANCE_RISK`
- `QUALITY_UNCERTAINTY_RISK`
- `SUPPLY_STABILITY_RISK`

## 7. Data Model and Storage
### 外部 API 保持不变
- `POST /api/analysis/tasks`
- `GET /api/analysis/tasks/{taskId}/status`
- `POST /api/analysis/tasks/{taskId}/selection`
- `GET /api/analysis/tasks/history`
- `GET /api/report/{taskId}`
- `GET /api/report/list`
- `GET /api/report/by-report/{reportId}`

### 业务表
- `analysis_task`
- `analysis_task_step`
- `overseas_product_snapshot`
- `domestic_product_snapshot`
- `normalized_product_profile`
- `product_mapping`
- `analysis_report`
- `source_offer_variant`

### Excel 导入基线表
- `import_batch`
- `import_row_raw`
- `import_row_parse_result`

### Excel 到业务模型的核心映射
#### 海外侧
- `国外商品搜索关键词` -> `search_keyword`
- `标题` -> `title`
- `价格（美元）` -> `price_amount + currency=USD`
- `产品信息` -> `description_raw`
- `图片地址` -> `image_url`
- `商品源地址` -> `source_url`

#### 国内侧
- `国内搜索对应商品` -> `manual_search_label`
- `标题` -> `title`
- `价格（人民币）`
  - 单值 -> `price_amount + currency=CNY`
  - 多规格 -> `price_text_raw + source_offer_variant`
- `产品信息` -> `description_raw`
- `图片地址` -> `image_url`
- `商品源地址` -> `source_url`
- `运费（人民币）` -> `shipping_amount + currency=CNY`

### Excel 导入解析规则
- 若 `A-F` 有值，开启新的海外商品组
- 若该行只有 `J-O` 有值，则挂到上一条海外商品下
- 若为 `不进行匹配`，生成 `mapping_label=NO_MATCH`
- 空白分隔行不进业务表
- 读取时必须按单元格坐标补空位，不能按“存在的单元格顺序”直接拼列
- `Sheet1` 作为典型“一对多映射样本”
- `表2` 作为宠物饮水机 demo 主样本

### 种子映射标签
`product_mapping` 增加：
- `mapping_source = EXCEL_SEED | AGENT_GENERATED`
- `mapping_label = CONFIRMED_MATCH | POSSIBLE_MATCH | NO_MATCH`
- `annotation_confidence`
- `annotation_note`

### 索引
- `analysis_task(status, updated_at desc)`
- `analysis_task_step(task_id, started_at)`
- `overseas_product_snapshot(search_keyword, captured_at desc)`
- `domestic_product_snapshot(platform, captured_at desc)`
- `product_mapping(task_id, final_score desc)`
- `normalized_product_profile(source_type, source_id)`
- `ivfflat` on `domestic_product_snapshot.embedding`

## 8. Observability and Task Status
### Step Trace
`pipelineSteps[]` 扩展为结构化 step trace，每个 step 固定字段：
- `key`
- `title`
- `status`
- `startedAt`
- `endedAt`
- `inputSummary`
- `outputSummary`
- `whyThisStep`
- `fallbackUsed`
- `errorMessage`

### 必须覆盖的关键步骤
- `rewrite_overseas_product`
- `plan_domestic_queries`
- `search_domestic_live`
- `recall_domestic_vector`
- `merge_and_dedup_candidates`
- `group_variants`
- `rerank_candidates`
- `analyze_profitability`
- `assess_risks`
- `generate_report`

### 返回策略
- `logs[]` 给 Thinking Log 面板
- `pipelineSteps[]` 给结构化步骤展示
- `Phase2TaskStatusResponse` 新增 `analysisPlan`
- 失败或 fallback 时必须保留原因与影响说明

## 9. MVP Scope and Non-goals
### Hackathon MVP 必做
- 海外实时搜索
- 用户手动选中
- Agent 结构化理解
- 国内实时搜索
- 国内 pgvector 召回
- 合并去重
- 1 主推荐 + 若干备选 + 变体组
- 利润、风险、解释、置信度
- Markdown 报告
- Excel 种子数据导入
- 任务步骤可观测

### 非目标
- 海外向量搜索主链路
- 以图搜图
- 多模态图片 embedding
- 自动选中海外商品
- 用户长期偏好学习
- 精确税费系统
- 真实物流 API 精算
- 全类目泛化
- SKU 级完整商品树建模

## 10. Implementation Plan
### 迭代 1：导入与数据基线
- 实现 `WorkbookImportService`
- 实现坐标补空位的 `ExcelRowParser`
- 完成 `import_batch / import_row_raw / import_row_parse_result`
- 导入 `Sheet1` 与 `表2`
- 生成 `overseas_product_snapshot / domestic_product_snapshot / source_offer_variant / product_mapping`

### 迭代 2：标准化与向量基线
- 实现 `SeedDataNormalizationService`
- 生成 `normalized_product_profile`
- 实现 `EmbeddingBackfillService`
- 完成 `domestic_product_snapshot.embedding` 回填

### 迭代 3：Agent 决策层
- 新增 `AnalysisOrchestrator`
- 新增：
  - `ProductUnderstandingService`
  - `DomesticQueryPlanningService`
- 让 Phase2 从“直接选结果”切到“先生成 AnalysisPlan”

### 迭代 4：检索与召回层
- 新增：
  - `OverseasSearchService`
  - `DomesticLiveSearchService`
  - `DomesticVectorRecallService`
  - `DomesticVectorSearchRepository`
- 正式 profile 走 PostgreSQL/pgvector
- `dev-memory` 保留 demo 运行能力

### 迭代 5：匹配、评分、风险
- 新增：
  - `CandidateMergeService`
  - `CandidateRerankService`
  - `RiskAssessmentService`
  - `ProfitAnalysisService`
- 落地 hard filters、soft penalties、finalScore、confidenceScore

### 迭代 6：报告与可观测性
- 新增 `ReportGenerationService`
- 完成 Markdown 输出
- 完成 `analysis_task_step`
- 扩展 `pipelineSteps[]` 与 `logs[]`
- 保持 controller 层不大改，只升级 service 与 repository 层

## 11. Test Plan
- Excel 导入：
  - `Sheet1` 中仅填 `J-O` 的国内候选行会正确继承上一条海外商品
  - `表2` 中只填海外侧的行不会误生成空的国内记录
  - `不进行匹配` 行会被标记为 `NO_MATCH`
  - 多规格价格文本会正确拆出 `source_offer_variant`
- 向量召回：
  - 导入后的宠物饮水机国内样本能完成 embedding 回填
  - 已知人工匹配候选能在 vector topK 中命中
- 二阶段决策：
  - 选中海外商品后能返回 `AnalysisPlan`
  - 能看到完整 step trace
  - live search 失败但历史向量召回命中时仍能完成分析
- 评分与风险：
  - 配件/滤芯/替换件被 hard reject
  - 容量、材质、bundle 差异进入 soft penalties
  - 风险项能出现在最终报告中
- 报告：
  - `GET /api/report/{taskId}` 返回主推荐、备选、风险、利润、置信度和 Markdown
- 兼容性：
  - 当前前端页面结构和外部 API 不需要重做

## 12. Assumptions
- Excel 作为首批长期种子数据源保留，不是一次性迁移文件。
- 宠物饮水机样本是默认 demo 主类目。
- 正式主链路采用 PostgreSQL + pgvector，开发保留 `dev-memory`。
- 本轮优先做“国内侧向量召回 + Agent 决策”，海外侧仍以实时搜索为主。
- 变体价格与规格本轮拆到 `source_offer_variant`，不做完整 SKU 交易模型。
- 报告必须体现“利润不等于推荐”，推荐决策由匹配质量、风险、利润、来源可信度共同决定。

## 13. Open Questions
- 海外实时搜索首批接入哪个 provider。
- 国内实时搜索首批是否只做 1688。
- 品牌/IP 风险词库首批是否由人工给一版黑白名单。
- embedding 正式 provider 选择哪一家。
- 运费和平台费 MVP 先按固定规则还是类目规则。
