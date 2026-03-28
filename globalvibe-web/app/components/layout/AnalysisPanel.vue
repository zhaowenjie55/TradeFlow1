<script setup lang="ts">
import ArbitrageReport from '~/components/report/ArbitrageReport.vue'
import ThinkingLog from '~/components/report/ThinkingLog.vue'
import type { AnalysisPanelTab } from '~/types'

const props = withDefaults(defineProps<{
  compact?: boolean
}>(), {
  compact: false,
})

const { t } = useAppI18n()
const taskStore = useTaskStore()
const uiStore = useUIStore()
const containerRef = ref<HTMLElement | null>(null)
const topHeight = ref(58)

const startResize = (event: MouseEvent) => {
  event.preventDefault()

  const handleMouseMove = (moveEvent: MouseEvent) => {
    const container = containerRef.value
    if (!container) return

    const rect = container.getBoundingClientRect()
    const next = ((moveEvent.clientY - rect.top) / rect.height) * 100
    topHeight.value = Math.min(72, Math.max(22, next))
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

const isDesktop = computed(() => uiStore.layoutMode === 'desktop' && !props.compact)
const activeTab = computed({
  get: () => uiStore.analysisPanelTab,
  set: (value: AnalysisPanelTab) => uiStore.setAnalysisPanelTab(value),
})
</script>

<template>
  <div ref="containerRef" class="flex h-full min-h-0 flex-col">
    <template v-if="isDesktop">
      <div class="group min-h-0 overflow-hidden" :style="{ flexBasis: `${topHeight}%` }">
        <div class="sticky top-0 z-10 flex h-10 items-center gap-2.5 border-b border-[var(--tf-border)] bg-[var(--tf-bg-panel)] px-4">
          <div class="flex items-center gap-2">
            <UIcon name="i-heroicons-bolt" class="h-4 w-4 text-slate-500 dark:text-slate-400" />
            <span class="tradeflow-section-title">{{ t('logs.title') }}</span>
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
        class="group flex h-3 shrink-0 cursor-row-resize items-center justify-center bg-white/70 transition-colors hover:bg-slate-100 dark:bg-slate-900/70 dark:hover:bg-slate-800"
        @mousedown="startResize"
      >
        <span class="h-1 w-14 rounded-full bg-slate-300 transition-colors group-hover:bg-slate-400 dark:bg-slate-700 dark:group-hover:bg-slate-500" />
      </button>

      <div class="group min-h-0 flex-1 overflow-hidden">
        <div class="sticky top-0 z-10 flex h-10 items-center gap-2.5 border-b border-[var(--tf-border)] bg-[var(--tf-bg-panel)] px-4">
          <div class="flex items-center gap-2">
            <UIcon name="i-heroicons-document-chart-bar" class="h-4 w-4 text-slate-500 dark:text-slate-400" />
            <span class="tradeflow-section-title">{{ t('report.title') }}</span>
          </div>
        </div>
        <ArbitrageReport />
      </div>
    </template>

    <template v-else>
      <div class="tradeflow-panel flex min-h-0 flex-1 flex-col overflow-hidden rounded-[var(--tf-radius-xl)]">
        <div class="sticky top-0 z-10 border-b border-[var(--tf-border)] bg-[var(--tf-bg-panel)] px-4 py-3">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="tradeflow-section-title">{{ t('report.title') }}</p>
              <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">{{ t('logs.panelHint') }}</p>
            </div>
            <div class="inline-flex rounded-2xl border border-[var(--tf-border)] bg-white/70 p-1 dark:bg-slate-950/70">
              <button
                type="button"
                class="rounded-xl px-3 py-2 text-sm font-medium transition"
                :class="activeTab === 'report' ? 'bg-[var(--tf-accent-soft)] text-[var(--tf-text)]' : 'text-slate-500 dark:text-slate-400'"
                @click="activeTab = 'report'"
              >
                {{ t('report.title') }}
              </button>
              <button
                type="button"
                class="rounded-xl px-3 py-2 text-sm font-medium transition"
                :class="activeTab === 'logs' ? 'bg-[var(--tf-accent-soft)] text-[var(--tf-text)]' : 'text-slate-500 dark:text-slate-400'"
                @click="activeTab = 'logs'"
              >
                {{ t('logs.title') }}
              </button>
            </div>
          </div>
        </div>

        <div class="min-h-0 flex-1 overflow-hidden">
          <ThinkingLog v-if="activeTab === 'logs'" />
          <ArbitrageReport v-else />
        </div>
      </div>
    </template>
  </div>
</template>
