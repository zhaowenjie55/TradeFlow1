import type { CandidateSummary } from './product'
import type { ReportDetail } from './report'

export type TaskMode = 'MOCK' | 'REAL' | 'AUTO_FALLBACK'
export type TaskStatus =
  | 'CREATED'
  | 'QUEUED'
  | 'RUNNING'
  | 'WAITING_USER_SELECTION'
  | 'ANALYZING_SOURCE'
  | 'REPORT_READY'
  | 'FAILED'
  | 'FALLBACK_MOCK'
export type TaskPhase = 'PHASE1' | 'PHASE2'
export type TaskViewStatus = TaskStatus | 'IDLE'
export type TaskStage = string
export type PipelineStepStatus = 'completed' | 'current' | 'pending'

export interface TaskConstraintRequest {
  field: string
  operator: string
  value: string
}

export interface AnalysisFormValues {
  keyword: string
  market: string
  targetProfitMargin: number
  topN: number
}

export interface CreateAnalysisTaskRequest {
  keyword: string
  limit: number
  targetProfitMargin: number
  constraints: TaskConstraintRequest[]
  mode?: TaskMode
}

export interface Phase1CreateTaskResponse {
  taskId: string
  phase: 'PHASE1'
  status: TaskStatus
  mode: TaskMode
  createdAt: string
}

export interface Phase2CreateTaskResponse {
  taskId: string
  phase: 'PHASE2'
  status: TaskStatus
  mode: TaskMode
  phase1TaskId: string
  productId: string
  createdAt: string
}

export interface TaskLogEntry {
  timestamp: string
  stage: string
  level: string
  message: string
  source: string | null
}

export interface PipelineStep {
  key: string
  title: string
  status: PipelineStepStatus
}

export interface Phase1TaskStatusResponse {
  taskId: string
  phase: 'PHASE1'
  status: TaskStatus
  stage: TaskStage
  progress: number
  fallbackTriggered: boolean
  keyword: string
  market: string
  targetProfitMargin: number | null
  mode: TaskMode
  createdAt: string
  updatedAt: string
  logs: TaskLogEntry[]
  pipelineSteps: PipelineStep[]
  candidates: CandidateSummary[]
}

export interface Phase2TaskStatusResponse {
  taskId: string
  phase: 'PHASE2'
  status: TaskStatus
  stage: TaskStage
  progress: number
  fallbackTriggered: boolean
  phase1TaskId: string
  productId: string
  reportId: string | null
  mode: TaskMode
  createdAt: string
  updatedAt: string
  logs: TaskLogEntry[]
  pipelineSteps: PipelineStep[]
  report: ReportDetail | null
}

export type TaskStatusResponse = Phase1TaskStatusResponse | Phase2TaskStatusResponse

export interface TaskHistoryItem {
  taskId: string
  keyword: string
  market: string
  status: TaskStatus
  mode: TaskMode
  createdAt: string
}

export interface TaskHistoryResponse {
  items: TaskHistoryItem[]
}
