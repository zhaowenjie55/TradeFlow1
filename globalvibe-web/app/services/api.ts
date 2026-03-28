import type { ApiEnvelope } from '~/types'

type FetchOptions<T> = Parameters<typeof $fetch<ApiEnvelope<T>>>[1]

const buildApiError = (message: string, code?: string | null, statusCode?: number) => {
  return Object.assign(new Error(message), {
    code: code ?? undefined,
    statusCode,
  })
}

export const apiRequest = async <T>(path: string, options?: FetchOptions<T>) => {
  const config = useRuntimeConfig()

  try {
    const response = await $fetch<ApiEnvelope<T>>(path, {
      baseURL: config.public.backendBaseUrl,
      ...options,
    })

    if (!response.success || response.data === null) {
      throw buildApiError(response.message ?? 'Request failed', response.errorCode)
    }

    return response.data
  } catch (error) {
    const normalized = error as {
      data?: ApiEnvelope<never>
      statusCode?: number
      message?: string
    }

    if (normalized?.data && normalized.data.success === false) {
      throw buildApiError(
        normalized.data.message ?? normalized.message ?? 'Request failed',
        normalized.data.errorCode,
        normalized.statusCode
      )
    }

    throw error
  }
}
