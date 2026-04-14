export interface AsrSegment {
  start: number
  end: number
  text: string
}

export interface AsrTranscriptionResult {
  success: boolean
  language: string | null
  duration: number
  text: string
  segments: AsrSegment[]
}

export interface TranscriptIntentResult {
  intent: string
  category: string | null
  market: string | null
  priceLevel: string | null
  sourcing: string | null
  keywords: string[]
  sellingPoints: string[]
  painPoints: string[]
  useCases: string[]
  targetAudience: string[]
  fallbackUsed: boolean
  provider: string | null
  model: string | null
  fallbackReason: string | null
  generatedAt: string | null
}

export interface VoiceSearchItem {
  platform: string | null
  externalItemId: string | null
  title: string | null
  price: string | null
  imageUrl: string | null
  productUrl: string | null
  rating: number | null
  reviewCount: number | null
}

export interface VoiceSearchResponse {
  success: boolean
  keyword: string
  page: number
  count: number
  items: VoiceSearchItem[]
}

export interface VoiceQueryResponse {
  transcript: AsrTranscriptionResult
  translatedText: string
  intent: TranscriptIntentResult
  normalizedKeyword: string
  searchResults: VoiceSearchResponse
}

export interface VoiceQueryPreviewResponse {
  transcript: AsrTranscriptionResult
  intent: TranscriptIntentResult
  translatedText: string
  normalizedKeyword: string
}

export interface MediaAnalysisResponse {
  transcript: AsrTranscriptionResult
  intent: TranscriptIntentResult
}
