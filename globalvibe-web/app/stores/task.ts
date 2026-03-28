import { defineStore } from 'pinia'
import type {
  AnalysisFormValues,
  Phase1CreateTaskResponse,
  Phase2CreateTaskResponse,
  PipelineStep,
  TaskHistoryItem,
  TaskMode,
  TaskPhase,
  TaskStage,
  TaskStatusResponse,
  TaskViewStatus,
} from '~/types'

const defaultParams: AnalysisFormValues = {
  keyword: '',
  market: 'AmazonUS',
  targetProfitMargin: 30,
  topN: 8,
}

export const useTaskStore = defineStore('task', () => {
  const currentTaskId = ref<string | null>(null)
  const phase1TaskId = ref<string | null>(null)
  const currentTaskPhase = ref<TaskPhase | null>(null)
  const status = ref<TaskViewStatus>('IDLE')
  const stage = ref<TaskStage>('idle')
  const progress = ref(0)
  const mode = ref<TaskMode>('AUTO_FALLBACK')
  const params = ref<AnalysisFormValues>({ ...defaultParams })
  const lastUpdatedAt = ref<string | null>(null)
  const fallbackTriggered = ref(false)
  const history = ref<TaskHistoryItem[]>([])
  const pipelineSteps = ref<PipelineStep[]>([])
  const currentReportId = ref<string | null>(null)
  const isPolling = ref(false)
  const errorMessage = ref('')

  const setActivePhase1Task = (response: Phase1CreateTaskResponse, nextParams: AnalysisFormValues) => {
    currentTaskId.value = response.taskId
    phase1TaskId.value = response.taskId
    currentTaskPhase.value = response.phase
    status.value = response.status
    mode.value = response.mode
    params.value = { ...nextParams }
    stage.value = 'phase1.create'
    progress.value = 0
    lastUpdatedAt.value = response.createdAt
    fallbackTriggered.value = false
    pipelineSteps.value = []
    currentReportId.value = null
    errorMessage.value = ''
  }

  const setActivePhase2Task = (response: Phase2CreateTaskResponse) => {
    currentTaskId.value = response.taskId
    phase1TaskId.value = response.phase1TaskId
    currentTaskPhase.value = response.phase
    status.value = response.status
    mode.value = response.mode
    stage.value = 'phase2.create'
    progress.value = 0
    lastUpdatedAt.value = response.createdAt
    fallbackTriggered.value = false
    currentReportId.value = null
    errorMessage.value = ''
  }

  const applyStatusSnapshot = (snapshot: TaskStatusResponse) => {
    currentTaskId.value = snapshot.taskId
    currentTaskPhase.value = snapshot.phase
    phase1TaskId.value = snapshot.phase === 'PHASE1' ? snapshot.taskId : snapshot.phase1TaskId
    status.value = snapshot.status
    stage.value = snapshot.stage
    progress.value = snapshot.progress
    mode.value = snapshot.mode
    lastUpdatedAt.value = snapshot.updatedAt
    fallbackTriggered.value = snapshot.fallbackTriggered
    pipelineSteps.value = snapshot.pipelineSteps
    currentReportId.value = snapshot.phase === 'PHASE2' ? snapshot.reportId : null

    if (snapshot.phase === 'PHASE1') {
      params.value = {
        ...params.value,
        keyword: snapshot.keyword,
        targetProfitMargin: snapshot.targetProfitMargin !== null
          ? Number((snapshot.targetProfitMargin * 100).toFixed(1))
          : params.value.targetProfitMargin,
      }
    }
  }

  const setPolling = (value: boolean) => {
    isPolling.value = value
  }

  const setHistory = (items: TaskHistoryItem[]) => {
    history.value = items
  }

  const setError = (message: string) => {
    errorMessage.value = message
  }

  const resetTask = () => {
    currentTaskId.value = null
    phase1TaskId.value = null
    currentTaskPhase.value = null
    status.value = 'IDLE'
    stage.value = 'idle'
    progress.value = 0
    lastUpdatedAt.value = null
    fallbackTriggered.value = false
    pipelineSteps.value = []
    currentReportId.value = null
    errorMessage.value = ''
  }

  return {
    currentTaskId,
    phase1TaskId,
    currentTaskPhase,
    status,
    stage,
    progress,
    mode,
    params,
    lastUpdatedAt,
    fallbackTriggered,
    history,
    pipelineSteps,
    currentReportId,
    isPolling,
    errorMessage,
    setActivePhase1Task,
    setActivePhase2Task,
    applyStatusSnapshot,
    setPolling,
    setHistory,
    setError,
    resetTask,
  }
})
