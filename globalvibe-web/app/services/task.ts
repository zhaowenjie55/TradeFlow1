import type {
  CandidateListResponse,
  CreateAnalysisTaskRequest,
  Phase1CreateTaskResponse,
  Phase2CreateTaskResponse,
  TaskHistoryResponse,
  TaskStatusResponse,
} from '~/types'
import { apiRequest } from './api'

export const createTask = (payload: CreateAnalysisTaskRequest) => {
  return apiRequest<Phase1CreateTaskResponse>('/api/analysis/tasks', {
    method: 'POST',
    body: payload,
  })
}

export const getTaskStatus = (taskId: string) => {
  return apiRequest<TaskStatusResponse>(`/api/analysis/tasks/${taskId}/status`)
}

export const getTaskHistory = () => {
  return apiRequest<TaskHistoryResponse>('/api/analysis/tasks/history')
}

export const getTaskCandidates = (taskId: string) => {
  return apiRequest<CandidateListResponse>(`/api/analysis/tasks/${taskId}/candidates`)
}

export const selectTaskCandidate = (taskId: string, productId: string) => {
  return apiRequest<Phase2CreateTaskResponse>(`/api/analysis/tasks/${taskId}/selection`, {
    method: 'POST',
    body: { productId },
  })
}
