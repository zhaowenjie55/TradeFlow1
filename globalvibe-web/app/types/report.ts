export type RecommendationDecision = 'recommended' | 'cautious' | 'not_recommended'
export type RiskLevel = 'low' | 'medium' | 'high'

export interface ReportDownloadDocument {
  fileName: string
  mimeType: string
  content: string
}

export interface DomesticProductMatch {
  id: string
  platform: string
  platformProductId: string
  title: string
  price: number | null
  image: string | null
  similarityScore: number
  detailUrl: string | null
  searchUrl: string | null
  reason: string | null
  matchSource: string | null
  detailReady: boolean
  detailSource: string | null
  retrievalTerms: string[]
  scoreBreakdown: Record<string, number>
  evidence: string[]
}

export interface ReportSummary {
  insightKey: string
  insightParams?: Record<string, unknown>
}

export interface ReportCostBreakdown {
  sourcingCost: number | null
  domesticShippingCost: number | null
  logisticsCost: number | null
  platformFee: number | null
  exchangeRateCost: number | null
  totalCost: number | null
  targetSellingPrice: number | null
  estimatedProfit: number | null
}

export interface ReportRiskAssessment {
  score: number | null
  factors: string[]
  notes: string[] | null
}

export interface AnalysisTraceRewrite {
  sourceTitle: string
  rewrittenText: string
  keywords: string[]
  provider: string
}

export interface AnalysisTraceRetrieval {
  retrievalTerms: string[]
  matchSource: string
  scoreBreakdown: Record<string, number>
  evidence: string[]
}

export interface AnalysisTracePricing {
  currency: string
  usdToCnyRate: number
  formulaLines: string[]
  assumptions: string[]
}

export interface AnalysisTraceLlm {
  provider: string
  model: string
  generatedAt: string
}

export interface AnalysisTrace {
  rewrite: AnalysisTraceRewrite | null
  retrieval: AnalysisTraceRetrieval | null
  pricing: AnalysisTracePricing | null
  llm: AnalysisTraceLlm | null
}

export interface ReportProvenance {
  rewriteProvider: string | null
  rewriteModel: string | null
  retrievalSource: string | null
  detailSource: string | null
  fallbackUsed: boolean
  fallbackReason: string | null
  llmProvider: string | null
  llmModel: string | null
  qualityTier: string | null
  pricingConfigVersion: string | null
}

export interface ReportDetail {
  taskId: string
  reportId: string
  productId: string
  title: string
  market: string
  image: string | null
  decision: RecommendationDecision
  riskLevel: RiskLevel
  expectedMargin: number | null
  generatedAt: string
  summary: ReportSummary
  costBreakdown: ReportCostBreakdown
  riskAssessment: ReportRiskAssessment
  recommendations: string[]
  domesticMatches: DomesticProductMatch[]
  analysisTrace?: AnalysisTrace | null
  provenance?: ReportProvenance | null
  downloadDocument?: ReportDownloadDocument
}

export interface ReportListItem {
  taskId: string
  reportId: string
  productId: string
  title: string
  decision: RecommendationDecision
  margin: number | null
  riskLevel: RiskLevel
  generatedAt: string
  qualityTier?: string | null
  fallbackUsed?: boolean
  retrievalSource?: string | null
  detailSource?: string | null
}

export interface ReportListResponse {
  items: ReportListItem[]
}
