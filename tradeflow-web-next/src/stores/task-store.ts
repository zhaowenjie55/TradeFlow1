"use client"

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
} from "@/types"
import { create } from "zustand"

const defaultParams: AnalysisFormValues = {
  keyword: "",
  market: "AmazonUS",
  targetProfitMargin: 30,
  topN: 8,
}

interface TaskState {
  currentTaskId: string | null
  phase1TaskId: string | null
  currentTaskPhase: TaskPhase | null
  status: TaskViewStatus
  stage: TaskStage
  progress: number
  mode: TaskMode
  params: AnalysisFormValues
  lastUpdatedAt: string | null
  fallbackTriggered: boolean
  history: TaskHistoryItem[]
  pipelineSteps: PipelineStep[]
  currentReportId: string | null
  isPolling: boolean
  errorMessage: string
  setActivePhase1Task: (response: Phase1CreateTaskResponse, nextParams: AnalysisFormValues) => void
  setActivePhase2Task: (response: Phase2CreateTaskResponse) => void
  applyStatusSnapshot: (snapshot: TaskStatusResponse) => void
  setPolling: (value: boolean) => void
  setHistory: (items: TaskHistoryItem[]) => void
  setError: (message: string) => void
  resetTask: () => void
}

export const useTaskStore = create<TaskState>((set) => ({
  currentTaskId: null,
  phase1TaskId: null,
  currentTaskPhase: null,
  status: "IDLE",
  stage: "idle",
  progress: 0,
  mode: "AUTO_FALLBACK",
  params: { ...defaultParams },
  lastUpdatedAt: null,
  fallbackTriggered: false,
  history: [],
  pipelineSteps: [],
  currentReportId: null,
  isPolling: false,
  errorMessage: "",
  setActivePhase1Task: (response, nextParams) =>
    set({
      currentTaskId: response.taskId,
      phase1TaskId: response.taskId,
      currentTaskPhase: response.phase,
      status: response.status,
      mode: response.mode,
      params: { ...nextParams },
      stage: "phase1.create",
      progress: 0,
      lastUpdatedAt: response.createdAt,
      fallbackTriggered: false,
      pipelineSteps: [],
      currentReportId: null,
      errorMessage: "",
    }),
  setActivePhase2Task: (response) =>
    set({
      currentTaskId: response.taskId,
      phase1TaskId: response.phase1TaskId,
      currentTaskPhase: response.phase,
      status: response.status,
      mode: response.mode,
      stage: "phase2.create",
      progress: 0,
      lastUpdatedAt: response.createdAt,
      fallbackTriggered: false,
      currentReportId: null,
      errorMessage: "",
    }),
  applyStatusSnapshot: (snapshot) =>
    set((state) => ({
      currentTaskId: snapshot.taskId,
      currentTaskPhase: snapshot.phase,
      phase1TaskId: snapshot.phase === "PHASE1" ? snapshot.taskId : snapshot.phase1TaskId,
      status: snapshot.status,
      stage: snapshot.stage,
      progress: snapshot.progress,
      mode: snapshot.mode,
      lastUpdatedAt: snapshot.updatedAt,
      fallbackTriggered: snapshot.fallbackTriggered,
      pipelineSteps: snapshot.pipelineSteps,
      currentReportId: snapshot.phase === "PHASE2" ? snapshot.reportId : null,
      params:
        snapshot.phase === "PHASE1"
          ? {
              ...state.params,
              keyword: snapshot.keyword,
              targetProfitMargin:
                snapshot.targetProfitMargin !== null
                  ? Number((snapshot.targetProfitMargin * 100).toFixed(1))
                  : state.params.targetProfitMargin,
            }
          : state.params,
    })),
  setPolling: (value) => set({ isPolling: value }),
  setHistory: (items) => set({ history: items }),
  setError: (message) => set({ errorMessage: message }),
  resetTask: () =>
    set({
      currentTaskId: null,
      phase1TaskId: null,
      currentTaskPhase: null,
      status: "IDLE",
      stage: "idle",
      progress: 0,
      lastUpdatedAt: null,
      fallbackTriggered: false,
      pipelineSteps: [],
      currentReportId: null,
      errorMessage: "",
    }),
}))
