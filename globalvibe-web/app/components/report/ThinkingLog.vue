<script setup lang="ts">
const agentStore = useAgentStore()
const taskStore = useTaskStore()
const { t } = useAppI18n()
const logContainer = ref<HTMLElement | null>(null)

watch(
  () => agentStore.taskLogs.length,
  () => {
    nextTick(() => {
      if (logContainer.value) {
        logContainer.value.scrollTop = logContainer.value.scrollHeight
      }
    })
  }
)

const levelColors: Record<string, string> = {
  INFO: 'text-slate-600 dark:text-slate-300',
  WARN: 'text-amber-600 dark:text-amber-400',
  WARNING: 'text-amber-600 dark:text-amber-400',
  ERROR: 'text-red-600 dark:text-red-400',
}

const formatTime = (date: string) => new Date(date).toLocaleTimeString()
</script>

<template>
  <div
    ref="logContainer"
    class="globalvibe-scrollbar h-full overflow-y-auto px-4 py-3 font-mono text-sm text-slate-500 dark:text-slate-400"
    style="scroll-behavior: smooth"
  >
    <div v-if="agentStore.taskLogs.length === 0" class="flex h-full items-center justify-center">
      <div class="rounded-lg bg-slate-100 p-4 dark:bg-slate-900">
        <p class="text-slate-400 dark:text-slate-500">{{ t('logs.empty') }}</p>
      </div>
    </div>

    <div v-else class="space-y-1.5">
      <div
        v-for="(entry, index) in agentStore.taskLogs"
        :key="`${entry.timestamp}-${entry.stage}-${index}`"
        class="flex gap-3 rounded-lg bg-slate-100 px-3 py-2 dark:bg-slate-900"
      >
        <span class="flex-shrink-0 font-mono text-slate-400 dark:text-slate-500">{{ formatTime(entry.timestamp) }}</span>

        <div class="flex-1">
          <p class="mb-1 text-[10px] uppercase tracking-wide text-slate-400 dark:text-slate-500">{{ t(`stage.${entry.stage}`) }}</p>
          <span :class="['leading-relaxed', levelColors[entry.level] ?? 'text-slate-600 dark:text-slate-300']">
            {{ entry.message }}
          </span>
        </div>
      </div>

      <div v-if="taskStore.isPolling" class="flex gap-3 rounded-lg bg-slate-100 px-3 py-2 dark:bg-slate-900">
        <span class="flex-shrink-0 font-mono text-slate-400 dark:text-slate-500">{{ new Date().toLocaleTimeString() }}</span>
        <span class="text-slate-500 dark:text-slate-400">
          <span class="animate-pulse">...</span>
        </span>
      </div>
    </div>
  </div>
</template>
