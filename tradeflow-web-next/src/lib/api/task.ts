import type {
  CandidateListResponse,
  CreateAnalysisTaskRequest,
  Phase1CreateTaskResponse,
  Phase2CreateTaskResponse,
  TaskHistoryResponse,
  TaskStatusResponse,
} from "@/types"
import { apiRequest, stringifyBody } from "@/lib/api/api"

export function createTask(payload: CreateAnalysisTaskRequest) {
  return apiRequest<Phase1CreateTaskResponse>("/api/analysis/tasks", {
    method: "POST",
    body: stringifyBody(payload),
  })
}

export function getTaskStatus(taskId: string) {
  return apiRequest<TaskStatusResponse>(`/api/analysis/tasks/${taskId}/status`)
}

export function getTaskHistory() {
  return apiRequest<TaskHistoryResponse>("/api/analysis/tasks/history")
}

export function getTaskCandidates(taskId: string) {
  return apiRequest<CandidateListResponse>(`/api/analysis/tasks/${taskId}/candidates`)
}

export function selectTaskCandidate(taskId: string, productId: string) {
  return apiRequest<Phase2CreateTaskResponse>(`/api/analysis/tasks/${taskId}/selection`, {
    method: "POST",
    body: stringifyBody({ productId }),
  })
}

export function resumeTask(taskId: string) {
  return apiRequest<Phase2CreateTaskResponse>(`/api/analysis/tasks/${taskId}/resume`, {
    method: "POST",
  })
}
