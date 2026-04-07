<script setup lang="ts">
import type { LogFilterMode } from '~/types'
import { getLiveStageMessage, humanizeTaskLog, isKeyTaskLog, resolveTaskLogCategory } from '~/utils/presentation'

const agentStore = useAgentStore()
const taskStore = useTaskStore()
const { t } = useAppI18n()
const logContainer = ref<HTMLElement | null>(null)
const liveTimestamp = ref('')
const displayedLengths = reactive<Record<string, number>>({})
const typingStates = reactive<Record<string, boolean>>({})
const animationTimers = new Map<string, ReturnType<typeof setInterval>>()
const autoFollow = ref(true)
const filterMode = ref<LogFilterMode>('all')

const entryKey = (entry: { timestamp: string, stage: string }, index: number) => `${entry.timestamp}-${entry.stage}-${index}`

const scrollToBottom = (behavior: ScrollBehavior = 'smooth') => {
  if (!autoFollow.value || !logContainer.value) return
  logContainer.value.scrollTo({
    top: logContainer.value.scrollHeight,
    behavior,
  })
}

const syncAutoFollow = () => {
  if (!logContainer.value) return
  const remaining = logContainer.value.scrollHeight - logContainer.value.scrollTop - logContainer.value.clientHeight
  autoFollow.value = remaining < 56
}

const animateMessage = (key: string, content: string) => {
  const existingTimer = animationTimers.get(key)
  if (existingTimer) clearInterval(existingTimer)

  displayedLengths[key] = 0
  typingStates[key] = true
  const timer = setInterval(() => {
    displayedLengths[key] = Math.min(content.length, displayedLengths[key] + 1)
    scrollToBottom('smooth')

    if (displayedLengths[key] >= content.length) {
      clearInterval(timer)
      animationTimers.delete(key)
      typingStates[key] = false
    }
  }, 18)

  animationTimers.set(key, timer)
}

const levelColors: Record<string, string> = {
  INFO: 'text-slate-600 dark:text-slate-300',
  WARN: 'text-amber-600 dark:text-amber-400',
  WARNING: 'text-amber-600 dark:text-amber-400',
  ERROR: 'text-red-600 dark:text-red-400',
}

const formatTime = (date: string) => new Date(date).toLocaleTimeString()

const getEntryView = (entry: (typeof agentStore.taskLogs)[number]) => humanizeTaskLog(entry)
const getEntryCategory = (entry: (typeof agentStore.taskLogs)[number]) => resolveTaskLogCategory(entry)

const visibleLogs = computed(() => {
  if (filterMode.value === 'key') {
    return agentStore.taskLogs.filter(isKeyTaskLog)
  }
  return agentStore.taskLogs
})

const getDisplayedMessage = (entry: (typeof agentStore.taskLogs)[number], index: number) => {
  const key = entryKey(entry, index)
  const content = getEntryView(entry).message
  const length = displayedLengths[key] ?? 0
  return content.slice(0, length)
}

onMounted(() => {
  liveTimestamp.value = new Date().toLocaleTimeString()
})

onUnmounted(() => {
  animationTimers.forEach(timer => clearInterval(timer))
  animationTimers.clear()
})

watch(
  () => visibleLogs.value.map((entry, index) => ({
    key: entryKey(entry, index),
    message: humanizeTaskLog(entry).message,
  })),
  (entries) => {
    if (!entries.length) {
      return
    }

    entries.forEach(entry => {
      if (!(entry.key in displayedLengths)) {
        animateMessage(entry.key, entry.message)
      }
    })

    nextTick(() => scrollToBottom('smooth'))
  },
  { deep: true },
)

watch(
  () => taskStore.isPolling,
  (isPolling) => {
    if (isPolling) {
      liveTimestamp.value = new Date().toLocaleTimeString()
      nextTick(() => scrollToBottom('smooth'))
    }
  }
)
</script>

<template>
  <div
    ref="logContainer"
    class="tradeflow-scrollbar h-full overflow-y-auto px-3 py-2.5 text-sm text-slate-500 dark:text-slate-400"
    style="scroll-behavior: smooth"
    @scroll="syncAutoFollow"
  >
    <div class="sticky top-0 z-10 mb-2.5 rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)]/95 px-3 py-2.5 backdrop-blur">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="inline-flex rounded-2xl border border-[var(--tf-border)] bg-white/70 p-1 text-sm dark:bg-slate-950/70">
          <button
            type="button"
            class="rounded-xl px-3 py-2 transition"
            :class="filterMode === 'all' ? 'bg-[var(--tf-accent-soft)] text-[var(--tf-text)]' : 'text-slate-500 dark:text-slate-400'"
            @click="filterMode = 'all'"
          >
            {{ t('logs.filterAll') }}
          </button>
          <button
            type="button"
            class="rounded-xl px-3 py-2 transition"
            :class="filterMode === 'key' ? 'bg-[var(--tf-accent-soft)] text-[var(--tf-text)]' : 'text-slate-500 dark:text-slate-400'"
            @click="filterMode = 'key'"
          >
            {{ t('logs.filterKey') }}
          </button>
        </div>

        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-xl border border-[var(--tf-border)] bg-white/70 px-3 py-1.5 text-sm font-medium text-slate-600 transition hover:bg-white dark:bg-slate-950/70 dark:text-slate-300"
          @click="autoFollow = !autoFollow"
        >
          <span class="h-2 w-2 rounded-full" :class="autoFollow ? 'bg-emerald-500' : 'bg-slate-400'" />
          {{ autoFollow ? t('logs.autoFollowOn') : t('logs.autoFollowOff') }}
        </button>
      </div>
    </div>

    <div v-if="visibleLogs.length === 0" class="flex h-full items-center justify-center">
      <div class="rounded-lg bg-slate-100 p-4 dark:bg-slate-900">
        <p class="text-slate-400 dark:text-slate-500">{{ t('logs.empty') }}</p>
      </div>
    </div>

    <div v-else class="space-y-1.5">
      <div
        v-for="(entry, index) in visibleLogs"
        :key="`${entry.timestamp}-${entry.stage}-${index}`"
        class="tradeflow-log-entry flex gap-3 rounded-2xl px-3 py-2.5"
        :class="getEntryCategory(entry) === 'alert'
          ? 'border border-amber-200 bg-amber-50/90 dark:border-amber-900/50 dark:bg-amber-950/20'
          : getEntryCategory(entry) === 'system'
            ? 'border border-[var(--tf-border)] bg-[var(--tf-bg-soft)]'
            : 'border border-blue-100 bg-blue-50/65 dark:border-blue-900/40 dark:bg-blue-950/20'"
      >
        <span class="flex-shrink-0 font-mono text-slate-400 dark:text-slate-500">{{ formatTime(entry.timestamp) }}</span>

        <div class="flex-1">
          <p class="mb-1 text-[12px] font-semibold tracking-wide text-slate-400 dark:text-slate-500">
            {{ getEntryView(entry).stageLabel }}
          </p>
          <span :class="['leading-6', levelColors[entry.level] ?? 'text-slate-600 dark:text-slate-300']">
            {{ getDisplayedMessage(entry, index) }}
            <span
              v-if="typingStates[entryKey(entry, index)]"
              class="ml-0.5 inline-block animate-pulse text-blue-500 dark:text-blue-300"
            >
              |
            </span>
          </span>
        </div>
      </div>

      <div v-if="taskStore.isPolling" class="flex gap-3 rounded-2xl border border-blue-100 bg-blue-50/70 px-3 py-2.5 dark:border-slate-800 dark:bg-slate-900">
        <span class="flex-shrink-0 font-mono text-slate-400 dark:text-slate-500">{{ liveTimestamp }}</span>
        <div class="text-slate-500 dark:text-slate-400">
          <p class="text-[11px] font-semibold tracking-wide text-slate-400 dark:text-slate-500">{{ t('logs.running') }}</p>
          <span class="leading-6">
            {{ getLiveStageMessage(taskStore.stage) }}<span class="animate-pulse">...</span>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>
