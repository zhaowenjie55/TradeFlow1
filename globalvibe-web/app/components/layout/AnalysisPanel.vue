<script setup lang="ts">
import ArbitrageReport from '~/components/report/ArbitrageReport.vue'
import ThinkingLog from '~/components/report/ThinkingLog.vue'

const { t } = useAppI18n()
const taskStore = useTaskStore()
const containerRef = ref<HTMLElement | null>(null)
const topHeight = ref(34)

const startResize = (event: MouseEvent) => {
  event.preventDefault()

  const handleMouseMove = (moveEvent: MouseEvent) => {
    const container = containerRef.value
    if (!container) return

    const rect = container.getBoundingClientRect()
    const next = ((moveEvent.clientY - rect.top) / rect.height) * 100
    topHeight.value = Math.min(60, Math.max(22, next))
  }

  const handleMouseUp = () => {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  document.body.style.cursor = 'row-resize'
  document.body.style.userSelect = 'none'
}
</script>

<template>
  <div ref="containerRef" class="flex h-full min-h-0 flex-col">
    <div class="group min-h-0 overflow-hidden" :style="{ flexBasis: `${topHeight}%` }">
      <div class="flex h-11 items-center gap-2.5 border-b border-slate-200 bg-slate-50 px-4 dark:border-slate-800 dark:bg-slate-900">
        <div class="flex items-center gap-2">
          <UIcon name="i-heroicons-bolt" class="h-4 w-4 text-slate-500 dark:text-slate-400" />
          <span class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{{ t('logs.title') }}</span>
        </div>
        <span v-if="taskStore.isPolling" class="ml-auto flex h-2 w-2">
          <span class="absolute inline-flex h-2 w-2 animate-ping rounded-full bg-blue-400 opacity-75 dark:bg-slate-400" />
          <span class="relative inline-flex h-2 w-2 rounded-full bg-blue-500 dark:bg-slate-500" />
        </span>
      </div>
      <ThinkingLog />
    </div>

    <button
      type="button"
      aria-label="Resize analysis panels"
      class="group flex h-4 shrink-0 cursor-row-resize items-center justify-center bg-white/70 transition-colors hover:bg-slate-100 dark:bg-slate-900/70 dark:hover:bg-slate-800"
      @mousedown="startResize"
    >
      <span class="h-1 w-14 rounded-full bg-slate-300 transition-colors group-hover:bg-slate-400 dark:bg-slate-700 dark:group-hover:bg-slate-500" />
    </button>

    <div class="group min-h-0 flex-1 overflow-hidden">
      <div class="flex h-11 items-center gap-2.5 border-b border-slate-200 bg-slate-50 px-4 dark:border-slate-800 dark:bg-slate-900">
        <div class="flex items-center gap-2">
          <UIcon name="i-heroicons-document-chart-bar" class="h-4 w-4 text-slate-500 dark:text-slate-400" />
          <span class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">{{ t('report.title') }}</span>
        </div>
      </div>
      <ArbitrageReport />
    </div>
  </div>
</template>
