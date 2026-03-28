import { defineStore } from 'pinia'
import type { TaskLogEntry } from '~/types'

export const useAgentStore = defineStore('agent', () => {
  const taskLogs = ref<TaskLogEntry[]>([])

  const setTaskLogs = (entries: TaskLogEntry[]) => {
    taskLogs.value = entries
  }

  const clearTaskLogs = () => {
    taskLogs.value = []
  }

  return {
    taskLogs,
    setTaskLogs,
    clearTaskLogs,
  }
})
