import type { MediaAnalysisResponse, VoiceQueryPreviewResponse, VoiceQueryResponse } from "@/types"
import { apiRequest, stringifyBody } from "@/lib/api/api"

function appendOptionalField(formData: FormData, field: string, value?: string | null) {
  if (value && value.trim()) {
    formData.append(field, value.trim())
  }
}

export async function submitVoiceQuery(file: File, options?: { task?: string; language?: string }) {
  const formData = new FormData()
  formData.append("file", file, file.name)
  formData.append("task", options?.task?.trim() || "transcribe")
  appendOptionalField(formData, "language", options?.language)
  return apiRequest<VoiceQueryResponse>("/api/voice-query", {
    method: "POST",
    body: formData,
  })
}

export async function previewVoiceQuery(file: File, options?: { task?: string; language?: string }) {
  const formData = new FormData()
  formData.append("file", file, file.name)
  formData.append("task", options?.task?.trim() || "transcribe")
  appendOptionalField(formData, "language", options?.language)
  return apiRequest<VoiceQueryPreviewResponse>("/api/voice-query/preview", {
    method: "POST",
    body: formData,
  })
}

export async function previewVoiceQueryText(payload: { transcript: string; translatedText?: string }) {
  return apiRequest<VoiceQueryPreviewResponse>("/api/voice-query/preview/text", {
    method: "POST",
    body: stringifyBody(payload),
  })
}

export async function submitMediaAnalyze(file: File, options?: { task?: string; language?: string }) {
  const formData = new FormData()
  formData.append("file", file, file.name)
  formData.append("task", options?.task?.trim() || "transcribe")
  appendOptionalField(formData, "language", options?.language)
  return apiRequest<MediaAnalysisResponse>("/api/media/analyze", {
    method: "POST",
    body: formData,
  })
}
