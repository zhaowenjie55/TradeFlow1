import { getReportByTaskId } from '~/services/report'
import { createTask, getTaskHistory, getTaskStatus, resumeTask, selectTaskCandidate } from '~/services/task'
import type {
  AnalysisFormValues,
  ReportDetail,
  TaskConstraintRequest,
  TaskStatusResponse,
} from '~/types'

const POLL_INTERVAL = 1000
const PHASE2_MAX_POLL_MILLIS = 90_000
let activePollTimer: ReturnType<typeof setTimeout> | null = null

const parseConstraints = (): TaskConstraintRequest[] => []

const mapReports = (reports: ReportDetail[]) => {
  return reports.reduce<Record<string, ReportDetail>>((acc, report) => {
    acc[report.productId] = report
    return acc
  }, {})
}

const isTerminalStatus = (snapshot: TaskStatusResponse) => {
  if (snapshot.status === 'FAILED' || snapshot.status === 'FALLBACK_MOCK' || snapshot.status === 'WAITING_1688_VERIFICATION') {
    return true
  }

  if (snapshot.phase === 'PHASE1') {
    return snapshot.status === 'WAITING_USER_SELECTION'
  }

  return snapshot.status === 'REPORT_READY'
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

export const useTaskRunner = () => {
  const taskStore = useTaskStore()
  const agentStore = useAgentStore()
  const productsStore = useProductsStore()

  const stopPolling = () => {
    if (activePollTimer) {
      clearTimeout(activePollTimer)
      activePollTimer = null
    }
    taskStore.setPolling(false)
  }

  const applySnapshot = (snapshot: TaskStatusResponse) => {
    taskStore.applyStatusSnapshot(snapshot)
    agentStore.setTaskLogs(snapshot.logs)

    if (snapshot.phase === 'PHASE1') {
      productsStore.setCandidates(snapshot.candidates)
      productsStore.setLoading(!isTerminalStatus(snapshot))
      productsStore.setReportAnalyzing(false)
      return
    }

    productsStore.selectProduct(snapshot.productId)

    if (snapshot.report) {
      productsStore.setReport(snapshot.report)
    }

    productsStore.setLoading(false)
    productsStore.setReportAnalyzing(snapshot.status !== 'WAITING_1688_VERIFICATION' && !isTerminalStatus(snapshot))
  }

  const pollTask = async (taskId: string) => {
    taskStore.setPolling(true)
    const startedAt = Date.now()

    const run = async () => {
      try {
        const snapshot = await getTaskStatus(taskId)
        applySnapshot(snapshot)

        if (snapshot.phase === 'PHASE2' && Date.now() - startedAt > PHASE2_MAX_POLL_MILLIS) {
          stopPolling()
          productsStore.setLoading(false)
          productsStore.setReportAnalyzing(false)
          taskStore.setError('国内货源分析超时，请重试或切换其他商品。')
          return
        }

        if (snapshot.phase === 'PHASE2' && snapshot.status === 'REPORT_READY' && !snapshot.report) {
          const report = await getReportByTaskId(snapshot.taskId)
          productsStore.selectProduct(report.productId)
          productsStore.setReport(report)
          productsStore.setReportAnalyzing(false)
        }

        if (isTerminalStatus(snapshot)) {
          stopPolling()
          return
        }

        if (taskStore.isPolling) {
          activePollTimer = setTimeout(run, POLL_INTERVAL)
        }
      } catch (error) {
        stopPolling()
        productsStore.setLoading(false)
        productsStore.setReportAnalyzing(false)
        taskStore.setError(resolveErrorMessage(error, 'Failed to fetch task status'))
      }
    }

    await run()
  }

  const startTask = async (params: AnalysisFormValues) => {
    stopPolling()
    taskStore.resetTask()
    taskStore.setError('')
    agentStore.clearTaskLogs()
    productsStore.reset()
    productsStore.setLoading(true)

    try {
      const response = await createTask({
        keyword: params.keyword.trim(),
        limit: params.topN,
        targetProfitMargin: Number((params.targetProfitMargin / 100).toFixed(4)),
        constraints: parseConstraints(),
      })

      taskStore.setActivePhase1Task(response, params)

      await pollTask(response.taskId)
      await refreshHistory()
    } catch (error) {
      stopPolling()
      productsStore.setLoading(false)
      taskStore.setError(resolveErrorMessage(error, 'Failed to create task'))
      throw error
    }
  }

  const analyzeProduct = async (productId: string) => {
    if (!taskStore.phase1TaskId || productsStore.isAnalyzingReport) return

    const candidate = productsStore.candidates.find(item => item.productId === productId)
    if (!candidate) return

    stopPolling()
    taskStore.setError('')
    agentStore.clearTaskLogs()
    productsStore.selectProduct(productId)
    productsStore.setReportAnalyzing(true)

    try {
      const response = await selectTaskCandidate(taskStore.phase1TaskId, productId)
      taskStore.setActivePhase2Task(response)

      await pollTask(response.taskId)
      await refreshHistory()
    } catch (error) {
      stopPolling()
      productsStore.setReportAnalyzing(false)
      taskStore.setError(resolveErrorMessage(error, 'Failed to start phase 2 analysis'))
      throw error
    }
  }

  const resumePhase2Task = async () => {
    if (!taskStore.currentTaskId || taskStore.currentTaskPhase !== 'PHASE2') return

    stopPolling()
    taskStore.setError('')
    productsStore.setReportAnalyzing(true)

    try {
      const response = await resumeTask(taskStore.currentTaskId)
      taskStore.setActivePhase2Task(response)
      await pollTask(response.taskId)
    } catch (error) {
      stopPolling()
      productsStore.setReportAnalyzing(false)
      taskStore.setError(resolveErrorMessage(error, 'Failed to resume phase 2 analysis'))
      throw error
    }
  }

  const refreshHistory = async () => {
    const response = await getTaskHistory()
    taskStore.setHistory(response.items)
  }

  return {
    startTask,
    analyzeProduct,
    resumePhase2Task,
    stopPolling,
    refreshHistory,
    mapReports,
  }
}
