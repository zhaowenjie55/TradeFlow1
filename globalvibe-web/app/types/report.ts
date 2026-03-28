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
}

export interface ReportListResponse {
  items: ReportListItem[]
}
