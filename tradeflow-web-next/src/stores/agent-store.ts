"use client"

import type { TaskLogEntry } from "@/types"
import { create } from "zustand"

interface AgentState {
  taskLogs: TaskLogEntry[]
  pendingTaskLogs: TaskLogEntry[]
  isLogPlaybackRunning: boolean
  setTaskLogs: (entries: TaskLogEntry[]) => void
  clearTaskLogs: () => void
}

export const useAgentStore = create<AgentState>((set) => ({
  taskLogs: [],
  pendingTaskLogs: [],
  isLogPlaybackRunning: false,
  setTaskLogs: (entries) => set({ taskLogs: entries, pendingTaskLogs: [], isLogPlaybackRunning: false }),
  clearTaskLogs: () => set({ taskLogs: [], pendingTaskLogs: [], isLogPlaybackRunning: false }),
}))
