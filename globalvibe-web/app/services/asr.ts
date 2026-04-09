import { apiRequest } from '~/services/api'
import type { MediaAnalysisResponse, VoiceQueryPreviewResponse, VoiceQueryResponse } from '~/types'

const appendOptionalField = (formData: FormData, field: string, value?: string | null) => {
  if (value && value.trim()) {
    formData.append(field, value.trim())
  }
}

export const submitVoiceQuery = async (file: File, options?: { task?: string, language?: string }) => {
  const formData = new FormData()
  formData.append('file', file, file.name)
  formData.append('task', options?.task?.trim() || 'transcribe')
  appendOptionalField(formData, 'language', options?.language)
  return apiRequest<VoiceQueryResponse>('/api/voice-query', {
    method: 'POST',
    body: formData,
  })
}

export const previewVoiceQuery = async (file: File, options?: { task?: string, language?: string }) => {
  const formData = new FormData()
  formData.append('file', file, file.name)
  formData.append('task', options?.task?.trim() || 'transcribe')
  appendOptionalField(formData, 'language', options?.language)
  return apiRequest<VoiceQueryPreviewResponse>('/api/voice-query/preview', {
    method: 'POST',
    body: formData,
  })
}

export const previewVoiceQueryText = async (payload: { transcript: string, translatedText?: string }) => {
  return apiRequest<VoiceQueryPreviewResponse>('/api/voice-query/preview/text', {
    method: 'POST',
    body: payload,
  })
}

export const submitMediaAnalyze = async (file: File, options?: { task?: string, language?: string }) => {
  const formData = new FormData()
  formData.append('file', file, file.name)
  formData.append('task', options?.task?.trim() || 'transcribe')
  appendOptionalField(formData, 'language', options?.language)
  return apiRequest<MediaAnalysisResponse>('/api/media/analyze', {
    method: 'POST',
    body: formData,
  })
}
