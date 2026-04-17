"use client"

import { useCallback, useRef } from "react"

import { getReportByTaskId } from "@/lib/api/report"
import { createTask, getTaskHistory, getTaskStatus, resumeTask, selectTaskCandidate } from "@/lib/api/task"
import { useAgentStore } from "@/stores/agent-store"
import { getFilteredCandidates, useProductsStore } from "@/stores/products-store"
import { useTaskStore } from "@/stores/task-store"
import type { AnalysisFormValues, ReportDetail, TaskConstraintRequest, TaskStatusResponse } from "@/types"

const POLL_INTERVAL = 1000
const VERIFICATION_RETRY_INTERVAL = 3000
const VERIFICATION_AUTO_RESUME_WINDOW_MILLIS = 30_000
const PHASE2_MAX_POLL_MILLIS = 90_000

function parseConstraints(): TaskConstraintRequest[] {
  return []
}

export function mapReports(reports: ReportDetail[]) {
  return reports.reduce<Record<string, ReportDetail>>((acc, report) => {
    acc[report.productId] = report
    return acc
  }, {})
}

function isTerminalStatus(snapshot: TaskStatusResponse) {
  if (
    snapshot.status === "FAILED" ||
    snapshot.status === "FALLBACK_MOCK" ||
    snapshot.status === "WAITING_1688_VERIFICATION"
  ) {
    return true
  }

  if (snapshot.phase === "PHASE1") {
    return snapshot.status === "WAITING_USER_SELECTION"
  }

  return snapshot.status === "REPORT_READY"
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) return error.message
  return fallback
}

export function useTaskRunner() {
  const pollTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const verificationWaitStartedAtRef = useRef<number | null>(null)

  const stopPolling = useCallback(() => {
    if (pollTimerRef.current) {
      clearTimeout(pollTimerRef.current)
      pollTimerRef.current = null
    }
    verificationWaitStartedAtRef.current = null
    useTaskStore.getState().setPolling(false)
  }, [])

  const applySnapshot = useCallback((snapshot: TaskStatusResponse) => {
    useTaskStore.getState().applyStatusSnapshot(snapshot)
    useAgentStore.getState().setTaskLogs(snapshot.logs)

    if (snapshot.phase === "PHASE1") {
      useProductsStore.getState().setCandidates(snapshot.candidates)
      useProductsStore.getState().setLoading(!isTerminalStatus(snapshot))
      useProductsStore.getState().setReportAnalyzing(false)
      return
    }

    useProductsStore.getState().selectProduct(snapshot.productId)
    if (snapshot.report) {
      useProductsStore.getState().setReport(snapshot.report)
    }
    useProductsStore.getState().setLoading(false)
    useProductsStore
      .getState()
      .setReportAnalyzing(snapshot.status !== "WAITING_1688_VERIFICATION" && !isTerminalStatus(snapshot))
  }, [])

  const pollTask = useCallback(
    async (taskId: string) => {
      useTaskStore.getState().setPolling(true)
      const startedAt = Date.now()

      const run = async (): Promise<void> => {
        try {
          const snapshot = await getTaskStatus(taskId)
          const isWaitingFor1688Verification =
            snapshot.phase === "PHASE2" && snapshot.status === "WAITING_1688_VERIFICATION"

          if (isWaitingFor1688Verification) {
            const now = Date.now()
            verificationWaitStartedAtRef.current ??= now
            const waitingMillis = now - verificationWaitStartedAtRef.current

            if (waitingMillis < VERIFICATION_AUTO_RESUME_WINDOW_MILLIS) {
              useAgentStore.getState().setTaskLogs(snapshot.logs)
              useProductsStore.getState().setLoading(false)
              useProductsStore.getState().setReportAnalyzing(true)

              if (useTaskStore.getState().isPolling) {
                pollTimerRef.current = setTimeout(() => {
                  void run()
                }, VERIFICATION_RETRY_INTERVAL)
              }
              return
            }
          } else {
            verificationWaitStartedAtRef.current = null
          }

          applySnapshot(snapshot)

          if (snapshot.phase === "PHASE2" && Date.now() - startedAt > PHASE2_MAX_POLL_MILLIS) {
            stopPolling()
            useProductsStore.getState().setLoading(false)
            useProductsStore.getState().setReportAnalyzing(false)
            useTaskStore.getState().setError("国内货源分析超时，请重试或切换其他商品。")
            return
          }

          if (snapshot.phase === "PHASE2" && snapshot.status === "REPORT_READY" && !snapshot.report) {
            const report = await getReportByTaskId(snapshot.taskId)
            useProductsStore.getState().selectProduct(report.productId)
            useProductsStore.getState().setReport(report)
            useProductsStore.getState().setReportAnalyzing(false)
          }

          if (isTerminalStatus(snapshot)) {
            stopPolling()
            return
          }

          if (useTaskStore.getState().isPolling) {
            pollTimerRef.current = setTimeout(() => {
              void run()
            }, POLL_INTERVAL)
          }
        } catch (error) {
          stopPolling()
          useProductsStore.getState().setLoading(false)
          useProductsStore.getState().setReportAnalyzing(false)
          useTaskStore.getState().setError(resolveErrorMessage(error, "Failed to fetch task status"))
        }
      }

      await run()
    },
    [applySnapshot, stopPolling],
  )

  const refreshHistory = useCallback(async () => {
    const response = await getTaskHistory()
    useTaskStore.getState().setHistory(response.items)
  }, [])

  const startTask = useCallback(
    async (params: AnalysisFormValues) => {
      stopPolling()
      useTaskStore.getState().resetTask()
      useTaskStore.getState().setError("")
      useAgentStore.getState().clearTaskLogs()
      useProductsStore.getState().reset()
      useProductsStore.getState().setLoading(true)

      try {
        const response = await createTask({
          keyword: params.keyword.trim(),
          limit: params.topN,
          targetProfitMargin: Number((params.targetProfitMargin / 100).toFixed(4)),
          constraints: parseConstraints(),
        })

        useTaskStore.getState().setActivePhase1Task(response, params)
        await pollTask(response.taskId)
        await refreshHistory()
      } catch (error) {
        stopPolling()
        useProductsStore.getState().setLoading(false)
        useTaskStore.getState().setError(resolveErrorMessage(error, "Failed to create task"))
        throw error
      }
    },
    [pollTask, refreshHistory, stopPolling],
  )

  const analyzeProduct = useCallback(
    async (productId: string) => {
      const taskState = useTaskStore.getState()
      const productState = useProductsStore.getState()
      if (!taskState.phase1TaskId || productState.isAnalyzingReport) return

      const candidate = productState.candidates.find((item) => item.productId === productId)
      if (!candidate) return

      stopPolling()
      useTaskStore.getState().setError("")
      useAgentStore.getState().clearTaskLogs()
      useProductsStore.getState().selectProduct(productId)
      useProductsStore.getState().setReportAnalyzing(true)

      try {
        const response = await selectTaskCandidate(taskState.phase1TaskId, productId)
        useTaskStore.getState().setActivePhase2Task(response)
        await pollTask(response.taskId)
        await refreshHistory()
      } catch (error) {
        stopPolling()
        useProductsStore.getState().setReportAnalyzing(false)
        useTaskStore
          .getState()
          .setError(resolveErrorMessage(error, "Failed to start phase 2 analysis"))
        throw error
      }
    },
    [pollTask, refreshHistory, stopPolling],
  )

  const resumePhase2Task = useCallback(async () => {
    const taskState = useTaskStore.getState()
    if (!taskState.currentTaskId || taskState.currentTaskPhase !== "PHASE2") return
    stopPolling()
    useTaskStore.getState().setError("")
    useProductsStore.getState().setReportAnalyzing(true)

    try {
      const response = await resumeTask(taskState.currentTaskId)
      useTaskStore.getState().setActivePhase2Task(response)
      await pollTask(response.taskId)
    } catch (error) {
      stopPolling()
      useProductsStore.getState().setReportAnalyzing(false)
      useTaskStore.getState().setError(resolveErrorMessage(error, "Failed to resume phase 2 analysis"))
      throw error
    }
  }, [pollTask, stopPolling])

  const filteredCandidates = getFilteredCandidates(useProductsStore.getState())

  return {
    startTask,
    analyzeProduct,
    resumePhase2Task,
    stopPolling,
    refreshHistory,
    filteredCandidates,
  }
}
