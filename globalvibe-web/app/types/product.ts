export interface CandidateSummary {
  productId: string
  title: string
  imageUrl: string | null
  market: string
  overseasPrice: number | null
  estimatedMargin: number | null
  riskTag: string | null
  recommendationReason: string | null
  suggestSecondPhase: boolean
}

export interface CandidateSnapshot extends CandidateSummary {
  taskId: string
  createdAt: string
}

export interface CandidateListResponse {
  taskId: string
  total: number
  items: CandidateSnapshot[]
}

export interface ProductDetail {
  productId: string
  platform: string
  title: string
  price: number | null
  image: string | null
  link: string | null
  rating: number | null
  reviews: number | null
  attributes: Record<string, unknown>
  brand: string | null
  description: string | null
  gallery: string[]
  skuData: Record<string, unknown>
  detailLoaded: boolean
}
