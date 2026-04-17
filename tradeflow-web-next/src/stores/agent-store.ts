"use client"

import {
  SEARCH_PHASE_CONFIG,
  type SearchPhase,
} from "@/components/agent/search-processing-workflow"
import type { TaskLogEntry } from "@/types"
import { create } from "zustand"

export interface ReasoningLogEntry {
  id: string
  phase: SearchPhase
  message: string
  timestamp: string
}

interface AgentState {
  taskLogs: TaskLogEntry[]
  pendingTaskLogs: TaskLogEntry[]
  isLogPlaybackRunning: boolean
  searchExperienceActive: boolean
  searchExperiencePhase: SearchPhase | null
  visibleCandidateCount: number
  stagedReasoningLogs: ReasoningLogEntry[]
  setTaskLogs: (entries: TaskLogEntry[]) => void
  clearTaskLogs: () => void
  startSearchExperience: () => void
  setSearchExperiencePhase: (phase: SearchPhase) => void
  setVisibleCandidateCount: (count: number) => void
  finishSearchExperience: () => void
  resetSearchExperience: () => void
}

export const useAgentStore = create<AgentState>((set) => ({
  taskLogs: [],
  pendingTaskLogs: [],
  isLogPlaybackRunning: false,
  searchExperienceActive: false,
  searchExperiencePhase: null,
  visibleCandidateCount: 0,
  stagedReasoningLogs: [],
  setTaskLogs: (entries) => set({ taskLogs: entries, pendingTaskLogs: [], isLogPlaybackRunning: false }),
  clearTaskLogs: () => set({ taskLogs: [], pendingTaskLogs: [], isLogPlaybackRunning: false }),
  startSearchExperience: () =>
    set(() => {
      const phase: SearchPhase = "query_parsed"
      return {
        searchExperienceActive: true,
        searchExperiencePhase: phase,
        visibleCandidateCount: 0,
        stagedReasoningLogs: [createReasoningLog(phase)],
      }
    }),
  setSearchExperiencePhase: (phase) =>
    set((state) => {
      const alreadyLogged = state.stagedReasoningLogs.some((entry) => entry.phase === phase)
      return {
        searchExperiencePhase: phase,
        stagedReasoningLogs: alreadyLogged
          ? state.stagedReasoningLogs
          : [...state.stagedReasoningLogs, createReasoningLog(phase)],
      }
    }),
  setVisibleCandidateCount: (count) => set({ visibleCandidateCount: count }),
  finishSearchExperience: () =>
    set((state) => ({
      searchExperienceActive: false,
      searchExperiencePhase: "complete",
      stagedReasoningLogs: state.stagedReasoningLogs.some((entry) => entry.phase === "complete")
        ? state.stagedReasoningLogs
        : [...state.stagedReasoningLogs, createReasoningLog("complete")],
    })),
  resetSearchExperience: () =>
    set({
      searchExperienceActive: false,
      searchExperiencePhase: null,
      visibleCandidateCount: 0,
      stagedReasoningLogs: [],
    }),
}))

function createReasoningLog(phase: SearchPhase): ReasoningLogEntry {
  return {
    id: `${phase}-${Date.now()}`,
    phase,
    message: SEARCH_PHASE_CONFIG[phase].reasoning,
    timestamp: new Date().toISOString(),
  }
}
