# TradeFlow 前端现状分析与后端衔接基线

## Summary
- 当前前端已经不是静态原型，而是一个完成度较高的 Nuxt 4 + Pinia 工作台，主流程是“创建分析任务 -> 轮询状态 -> 展示候选商品 -> 用户选品 -> 二阶段分析 -> 展示报告”，核心编排在 [useTaskRunner.ts](D:/Project/WebProject/TradeFlow/globalvibe-web/app/composables/useTaskRunner.ts)。
- 目前**有真实接口调用逻辑**，而且接口契约已经基本成型；项目本身也能成功 `npm run build`，说明前端结构和类型是通的。
- 当前仓库里真正实现的本地接口只有 `/api/settings/demo-config`；分析任务和报告相关接口都还是前端先约定、后端待实现的状态。

## Current Flow
- 首页左侧参数表单提交后，会调用 `POST /api/analysis/tasks` 创建一阶段任务；提交内容是 `keyword / limit / targetProfitMargin / constraints / mode`。这里有一个现状缺口：UI 里有 `market`，但创建任务时**没有发给后端**。
- 创建成功后，前端每 1 秒轮询 `GET /api/analysis/tasks/{taskId}/status`。一阶段状态响应里，前端期望拿到 `progress / stage / pipelineSteps / logs / candidates / fallbackTriggered`。
- 当一阶段状态变成 `WAITING_USER_SELECTION`，前端停止轮询并展示候选商品。用户点击商品后，调用 `POST /api/analysis/tasks/{phase1TaskId}/selection`，只提交 `productId`。
- 二阶段开始后继续轮询状态；当状态变成 `REPORT_READY` 时，如果状态响应里已经带了 `report`，前端直接展示；如果没带，则额外调用 `GET /api/report/{taskId}` 补拉报告。
- 独立页面还会额外请求：
  - `GET /api/analysis/tasks/history`
  - `GET /api/report/list`
  - `GET /api/report/by-report/{reportId}`
- 以上这些远端请求都通过 [api.ts](D:/Project/WebProject/TradeFlow/globalvibe-web/app/services/api.ts) 统一封装，并依赖 `NUXT_PUBLIC_BACKEND_BASE_URL`；只有设置页和参数表单里的 demo 配置走本地 Nuxt 接口 `/api/settings/demo-config`。

## Important Interface Notes
- 当前前端默认所有远端接口都返回统一 envelope：`{ success, data, errorCode, message }`。如果 `success=false` 或 `data=null`，前端会直接报错。
- `getTaskCandidates(taskId)` 服务虽然写了，但页面实际上**没有单独调用**；候选列表目前依赖状态接口直接返回。这意味着你后端重设计时，v1 可以先不做单独的 candidates 查询接口。
- 报告展示组件 [ArbitrageReport.vue](D:/Project/WebProject/TradeFlow/globalvibe-web/app/components/report/ArbitrageReport.vue) 当前依赖的报告结构比较重，包含成本拆解、风险评估、推荐结论、国内匹配商品、下载文档等字段。
- 报告摘要不是纯文本，而是 `summary.insightKey + insightParams` 这种“翻译键驱动”结构；`riskAssessment.factors` 也是键值式枚举。这更像演示期契约，后端正式化时建议改成直接返回可展示文本。
- `CandidateMatch / QueryRewrite / AnalysisResult` 这些类型已经在前端定义，但当前 UI 没有消费，可以视为后续 LangChain4j 中间产物或审计数据的预留模型。

## Recommended Backend Direction
- 既然你准备自己用 Spring Boot + LangChain4j 重设计，建议保留“异步任务 + 轮询 + 两阶段分析”这个产品流程，因为它天然适合 LLM、检索、比价、报告生成这类长耗时任务。
- 可以不强行兼容当前路径命名，后端建议统一资源模型；但前端真正离不开的是这些能力：任务创建、状态轮询、候选返回、用户选品、报告查询、历史查询。
- 第一轮最值得先定死的不是技术细节，而是 3 个契约：任务状态枚举、状态查询 DTO、报告 DTO。只要这三块稳定，前后端联调会非常顺。

## Test Plan
- 提交任务后，前端能进入轮询，并正确显示步骤、日志、进度。
- 一阶段返回候选商品后，前端能停止一阶段轮询并允许用户选品。
- 选品后能进入二阶段，并在 `REPORT_READY` 时正确展示报告。
- 历史页、报告列表页、报告详情页都能独立加载。
- 异常场景覆盖：任务失败、空候选、空报告、超时、`success=false`、以及 mock/fallback 状态。

## Assumptions
- 后端由你后续独立用 Spring Boot + LangChain4j 实现，不放在当前 Nuxt `server/api` 里。
- 允许重设计接口，不要求严格兼容现有前端路径和字段。
- 默认建议后端继续采用“两阶段异步任务”流程；如果你后面想简化成同步接口，再一起反推前端改动会更稳。
