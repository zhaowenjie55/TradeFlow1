export interface CandidateMatch {
  matchId: string
  taskId: string | null
  candidateId: string
  sourceProductId: string | null
  platform: string
  externalItemId: string
  title: string
  price: number | null
  image: string | null
  link: string | null
  similarityScore: number | null
  matchSource: string | null
  searchKeyword: string | null
  fallbackUsed: boolean
  fallbackReason: string | null
  reason: string | null
  createdAt: string
}

export interface CandidateMatchListResponse {
  items: CandidateMatch[]
}

export interface QueryRewrite {
  rewriteId: string
  taskId: string | null
  candidateId: string | null
  sourceProductId: string | null
  sourceText: string
  rewrittenText: string
  keywords: string[]
  gatewaySource: string | null
  fallbackUsed: boolean
  fallbackReason: string | null
  createdAt: string
}

export interface QueryRewriteListResponse {
  items: QueryRewrite[]
}

export interface AnalysisResult {
  taskId: string
  reportId: string
  sourceProductId: string
  sourceTitle: string
  benchmarkProductId: string | null
  benchmarkProductTitle: string | null
  benchmarkPlatform: string | null
  benchmarkPrice: number | null
  expectedMargin: number | null
  matchScore: number | null
  decision: string
  riskLevel: string
  summary: string | null
  createdAt: string
}
