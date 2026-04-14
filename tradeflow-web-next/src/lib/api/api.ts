import type { ApiEnvelope } from "@/types"

const BACKEND_BASE_URL =
  process.env.NEXT_PUBLIC_BACKEND_BASE_URL?.trim() || "http://127.0.0.1:8081"

export interface ApiError extends Error {
  code?: string
  statusCode?: number
}

function buildApiError(message: string, code?: string | null, statusCode?: number): ApiError {
  const error = new Error(message) as ApiError
  error.code = code ?? undefined
  error.statusCode = statusCode
  return error
}

function resolveUrl(path: string) {
  if (/^https?:\/\//.test(path)) return path
  return `${BACKEND_BASE_URL}${path}`
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(resolveUrl(path), {
    ...init,
    headers: {
      ...(init?.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
      ...(init?.headers ?? {}),
    },
    cache: "no-store",
  })

  let payload: ApiEnvelope<T> | null = null
  try {
    payload = (await response.json()) as ApiEnvelope<T>
  } catch {
    if (!response.ok) {
      throw buildApiError(`Request failed with status ${response.status}`, null, response.status)
    }
    throw buildApiError("Invalid API response", null, response.status)
  }

  if (!response.ok || !payload.success || payload.data === null) {
    throw buildApiError(
      payload.message ?? `Request failed with status ${response.status}`,
      payload.errorCode,
      response.status,
    )
  }

  return payload.data
}

export function stringifyBody<T>(payload: T) {
  return JSON.stringify(payload)
}
