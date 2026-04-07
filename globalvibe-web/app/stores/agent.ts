import { defineStore } from 'pinia'
import type { TaskLogEntry } from '~/types'

export const useAgentStore = defineStore('agent', () => {
  const taskLogs = ref<TaskLogEntry[]>([])
  const pendingTaskLogs = ref<TaskLogEntry[]>([])
  const seenLogKeys = ref<string[]>([])
  const isLogPlaybackRunning = ref(false)
  let playbackTimer: ReturnType<typeof setTimeout> | null = null

  const createLogKey = (entry: TaskLogEntry) => {
    return `${entry.timestamp}-${entry.stage}-${entry.level}-${entry.message}`
  }

  const clearPlaybackTimer = () => {
    if (!playbackTimer) return
    clearTimeout(playbackTimer)
    playbackTimer = null
  }

  const resolvePlaybackDelay = (entry: TaskLogEntry) => {
    const normalizedLength = entry.message.trim().length
    return Math.min(2200, Math.max(950, 420 + normalizedLength * 26))
  }

  const playPendingLogs = () => {
    if (isLogPlaybackRunning.value || pendingTaskLogs.value.length === 0) return

    isLogPlaybackRunning.value = true

    const runNext = () => {
      const next = pendingTaskLogs.value.shift()
      if (!next) {
        isLogPlaybackRunning.value = false
        clearPlaybackTimer()
        return
      }

      taskLogs.value = [...taskLogs.value, next]
      playbackTimer = setTimeout(runNext, resolvePlaybackDelay(next))
    }

    runNext()
  }

  const setTaskLogs = (entries: TaskLogEntry[]) => {
    const unseenEntries = entries.filter((entry) => {
      const key = createLogKey(entry)
      if (seenLogKeys.value.includes(key)) return false
      seenLogKeys.value = [...seenLogKeys.value, key]
      return true
    })

    if (!unseenEntries.length) return

    pendingTaskLogs.value = [...pendingTaskLogs.value, ...unseenEntries]
    playPendingLogs()
  }

  const clearTaskLogs = () => {
    clearPlaybackTimer()
    taskLogs.value = []
    pendingTaskLogs.value = []
    seenLogKeys.value = []
    isLogPlaybackRunning.value = false
  }

  return {
    taskLogs,
    pendingTaskLogs,
    isLogPlaybackRunning,
    setTaskLogs,
    clearTaskLogs,
  }
})
