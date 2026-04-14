export interface ApiEnvelope<T> {
  success: boolean
  data: T | null
  errorCode: string | null
  message: string | null
}

export interface DemoConfigResponse {
  defaultLocale: string
  locales: string[]
  markets: string[]
}
