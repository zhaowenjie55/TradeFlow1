<script setup lang="ts">
import ProductCard from '~/components/product/ProductCard.vue'
import { getLiveStageMessage, getReadableOriginalTitle, getReadableProductName } from '~/utils/presentation'

const props = withDefaults(defineProps<{
  compact?: boolean
}>(), {
  compact: false,
})

const productsStore = useProductsStore()
const taskStore = useTaskStore()
const agentStore = useAgentStore()
const { analyzeProduct } = useTaskRunner()
const { t } = useAppI18n()
const containerRef = ref<HTMLElement | null>(null)
const topHeight = ref(22)

const gridCols = 'grid-cols-1 md:grid-cols-2 xl:grid-cols-3'

const displayedCandidates = computed(() => {
  const isLogPlaybackActive = agentStore.isLogPlaybackRunning || agentStore.pendingTaskLogs.length > 0
  const shouldDelayReveal = taskStore.currentTaskPhase === 'PHASE1'
    && taskStore.status === 'WAITING_USER_SELECTION'
    && isLogPlaybackActive

  return shouldDelayReveal ? [] : productsStore.filteredCandidates
})

const isThinkingStage = computed(() => {
  const isLogPlaybackActive = agentStore.isLogPlaybackRunning || agentStore.pendingTaskLogs.length > 0
  const waitingForReveal = taskStore.currentTaskPhase === 'PHASE1'
    && taskStore.status === 'WAITING_USER_SELECTION'
    && productsStore.filteredCandidates.length > 0
    && isLogPlaybackActive

  return (productsStore.isLoading || waitingForReveal) && displayedCandidates.value.length === 0
})

const thinkingTitle = computed(() => {
  const isLogPlaybackActive = agentStore.isLogPlaybackRunning || agentStore.pendingTaskLogs.length > 0
  if (taskStore.status === 'WAITING_USER_SELECTION' && isLogPlaybackActive) {
    return t('products.thinkingRevealTitle')
  }

  return t('products.thinkingTitle')
})

const thinkingDescription = computed(() => {
  const isLogPlaybackActive = agentStore.isLogPlaybackRunning || agentStore.pendingTaskLogs.length > 0
  if (taskStore.status === 'WAITING_USER_SELECTION' && isLogPlaybackActive) {
    return t('products.thinkingRevealDescription')
  }

  return getLiveStageMessage(taskStore.stage)
})

const workflowSteps = computed(() => {
  const hasCandidates = displayedCandidates.value.length > 0
  const hasSelection = Boolean(productsStore.selectedProductId)
  const hasReport = Boolean(productsStore.currentReport) && !productsStore.isAnalyzingReport

  return [
    {
      key: 'discover',
      titleKey: 'products.stepDiscoverTitle',
      descriptionKey: 'products.stepDiscoverDescription',
      state: hasCandidates ? 'complete' : taskStore.isPolling ? 'active' : 'pending',
    },
    {
      key: 'select',
      titleKey: 'products.stepSelectTitle',
      descriptionKey: 'products.stepSelectDescription',
      state: hasSelection ? 'complete' : hasCandidates ? 'active' : 'pending',
    },
    {
      key: 'report',
      titleKey: 'products.stepReportTitle',
      descriptionKey: productsStore.isAnalyzingReport ? 'products.stepReportLoading' : 'products.stepReportDescription',
      state: hasReport ? 'complete' : productsStore.isAnalyzingReport ? 'active' : 'pending',
    },
  ] as const
})

const workflowCardClass = (state: 'pending' | 'active' | 'complete') => {
  if (state === 'complete') {
    return 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-950/30 dark:text-emerald-300'
  }

  if (state === 'active') {
    return 'border-blue-200 bg-blue-50 text-blue-700 dark:border-blue-900/50 dark:bg-blue-950/30 dark:text-blue-300'
  }

  return 'border-slate-200 bg-white text-slate-500 dark:border-slate-800 dark:bg-slate-950/40 dark:text-slate-400'
}

const handleSelectProduct = (productId: string) => {
  if (productsStore.isAnalyzingReport) return
  void analyzeProduct(productId).catch(() => undefined)
}

const startResize = (event: MouseEvent) => {
  event.preventDefault()

  const handleMouseMove = (moveEvent: MouseEvent) => {
    const container = containerRef.value
    if (!container) return

    const rect = container.getBoundingClientRect()
    const next = ((moveEvent.clientY - rect.top) / rect.height) * 100
    topHeight.value = Math.min(58, Math.max(18, next))
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

const selectedDisplayTitle = computed(() => {
  if (!productsStore.currentCandidate?.title) return ''
  return getReadableProductName(productsStore.currentCandidate.title)
})

const selectedOriginalTitle = computed(() => {
  if (!productsStore.currentCandidate?.title) return ''
  return getReadableOriginalTitle(productsStore.currentCandidate.title, selectedDisplayTitle.value)
})

const canResize = computed(() => !props.compact)
</script>

<template>
  <div ref="containerRef" class="flex h-full min-h-0 flex-col">
    <div
      class="tradeflow-scrollbar min-h-0 overflow-auto"
      :class="canResize ? 'border-b border-[var(--tf-border)]' : ''"
      :style="canResize ? { flexBasis: `${topHeight}%` } : undefined"
    >
      <div :class="[props.compact ? 'px-4 py-4' : 'px-4 py-4 xl:px-5']">
        <div class="flex items-start justify-between gap-4">
          <div>
            <div class="flex items-center gap-2">
              <UIcon name="i-heroicons-shopping-bag" class="h-5 w-5 text-slate-400 dark:text-slate-500" />
              <span class="text-lg font-semibold text-slate-800 dark:text-slate-100">{{ t('products.title') }}</span>
              <span v-if="displayedCandidates.length > 0" class="ml-1 text-sm text-slate-400 dark:text-slate-500">
                {{ t('products.count', { count: displayedCandidates.length }) }}
              </span>
            </div>
            <p class="mt-1 max-w-2xl text-[13px] leading-5 text-slate-500 dark:text-slate-400">
              {{ t('products.subtitle') }}
            </p>
          </div>

          <span
            v-if="taskStore.status !== 'IDLE'"
            class="rounded-full border border-[var(--tf-border)] bg-white/80 px-3 py-1 text-[12px] text-slate-600 dark:bg-slate-950/80 dark:text-slate-300"
          >
            {{ t(`stage.${taskStore.stage}`) }}
          </span>
        </div>

        <div class="mt-3 flex flex-wrap gap-2.5">
          <div
            v-for="(step, index) in workflowSteps"
            :key="step.key"
            :class="[
              'min-w-[11rem] flex-1 rounded-2xl border px-3.5 py-3 transition-colors',
              workflowCardClass(step.state),
            ]"
          >
            <div class="flex items-center gap-2">
              <span class="flex h-6 w-6 items-center justify-center rounded-full bg-white/80 text-[11px] font-semibold dark:bg-slate-900/60">
                {{ index + 1 }}
              </span>
              <p class="text-sm font-semibold">{{ t(step.titleKey) }}</p>
            </div>
            <p class="mt-1.5 text-[12px] leading-5 opacity-90">
              {{ t(step.descriptionKey) }}
            </p>
          </div>
        </div>

        <div
          v-if="displayedCandidates.length > 0"
          class="mt-3 rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] px-4 py-2.5 text-[13px] text-slate-600 dark:text-slate-300"
        >
          {{ productsStore.isAnalyzingReport ? t('products.selectionHint') : t('products.selectionReady') }}
        </div>

        <div
          v-if="productsStore.currentCandidate"
          class="tradeflow-panel mt-3 rounded-[var(--tf-radius-xl)] px-4 py-3.5"
        >
          <div class="flex flex-col gap-3 xl:flex-row xl:items-start">
            <div class="flex h-14 w-14 items-center justify-center overflow-hidden rounded-xl bg-slate-100 dark:bg-slate-800">
              <img
                v-if="productsStore.currentCandidate.imageUrl"
                :src="productsStore.currentCandidate.imageUrl"
                :alt="productsStore.currentCandidate.title"
                class="h-14 w-14 object-cover"
              />
              <UIcon v-else name="i-heroicons-photo" class="h-8 w-8 text-slate-400 dark:text-slate-600" />
            </div>

            <div class="min-w-0 flex-1">
              <p class="tradeflow-section-title">
                {{ t('products.selectedTitle') }}
              </p>
              <h3 class="mt-1.5 text-[17px] font-semibold leading-6 text-slate-900 dark:text-slate-100">
                {{ selectedDisplayTitle }}
              </h3>
              <p v-if="selectedOriginalTitle" class="mt-1 line-clamp-1 text-[13px] leading-5 text-slate-500 dark:text-slate-400">
                {{ selectedOriginalTitle }}
              </p>
              <p class="mt-1.5 text-[13px] leading-5 text-slate-500 dark:text-slate-400">
                {{
                  productsStore.isAnalyzingReport
                    ? t('products.selectedDescriptionLoading', { title: selectedDisplayTitle })
                    : t('products.selectedDescriptionReady', { title: selectedDisplayTitle })
                }}
              </p>
              <p v-if="productsStore.currentCandidate.recommendationReason" class="mt-2 line-clamp-2 text-[13px] leading-5 text-slate-500 dark:text-slate-400">
                {{ productsStore.currentCandidate.recommendationReason }}
              </p>
            </div>

            <div class="grid grid-cols-3 gap-3 text-[12px] text-slate-500 xl:min-w-[15rem] xl:text-right dark:text-slate-400">
              <div>
                <p>{{ t('products.price') }}</p>
                <p class="mt-1 font-semibold text-slate-700 dark:text-slate-200">
                  ${{ productsStore.currentCandidate.overseasPrice ?? '--' }}
                </p>
              </div>
              <div>
                <p>{{ t('products.margin') }}</p>
                <p class="mt-1 font-semibold text-emerald-600 dark:text-emerald-400">
                  {{ productsStore.currentCandidate.estimatedMargin !== null ? `+${productsStore.currentCandidate.estimatedMargin}%` : '--' }}
                </p>
              </div>
              <div>
                <p>{{ t('products.riskTag') }}</p>
                <p class="mt-1 font-semibold text-slate-700 dark:text-slate-200">
                  {{ productsStore.currentCandidate.riskTag ?? '--' }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <button
      v-if="canResize"
      type="button"
      aria-label="Resize product panels"
      class="group flex h-4 shrink-0 cursor-row-resize items-center justify-center bg-white/70 transition-colors hover:bg-slate-100 dark:bg-slate-900/70 dark:hover:bg-slate-800"
      @mousedown="startResize"
    >
      <span class="h-1 w-14 rounded-full bg-slate-300 transition-colors group-hover:bg-slate-400 dark:bg-slate-700 dark:group-hover:bg-slate-500" />
    </button>

    <div :class="['tradeflow-scrollbar overflow-auto', canResize ? 'min-h-0 flex-1 p-4 xl:p-5' : 'p-4 pt-0 md:p-5 md:pt-0']">
      <div
        v-if="isThinkingStage"
        class="flex h-full flex-col items-center justify-center text-center"
      >
        <div class="flex h-20 w-20 items-center justify-center rounded-full border border-blue-200/60 bg-blue-50/80 dark:border-blue-900/40 dark:bg-blue-950/20">
          <UIcon name="i-heroicons-sparkles" class="h-10 w-10 animate-pulse text-blue-500 dark:text-blue-300" />
        </div>
        <h3 class="mt-5 text-lg font-semibold text-slate-700 dark:text-slate-200">{{ thinkingTitle }}</h3>
        <p class="mt-2 max-w-md text-sm leading-7 text-slate-500 dark:text-slate-400">
          {{ thinkingDescription }}
        </p>
        <div class="mt-5 flex items-center gap-2 text-sm text-slate-400 dark:text-slate-500">
          <span class="h-2.5 w-2.5 rounded-full bg-blue-500 animate-pulse" />
          <span>{{ t('logs.running') }}</span>
        </div>
      </div>

      <div
        v-else-if="displayedCandidates.length === 0"
        class="flex h-full flex-col items-center justify-center text-center"
      >
        <UIcon name="i-heroicons-cube" class="h-16 w-16 text-slate-300 dark:text-slate-700" />
        <h3 class="mt-4 text-lg font-medium text-slate-600 dark:text-slate-400">{{ t('products.emptyTitle') }}</h3>
        <p class="mt-2 max-w-xs text-sm leading-relaxed text-slate-400 dark:text-slate-500">
          {{ t('products.emptyDescription') }}
        </p>
      </div>

      <div v-else :class="['grid gap-3.5 xl:gap-4', gridCols]">
        <ProductCard
          v-for="product in displayedCandidates"
          :key="product.productId"
          :product="product"
          :is-selected="productsStore.selectedProductId === product.productId"
          :is-analyzing="productsStore.isAnalyzingReport && productsStore.selectedProductId === product.productId"
          :is-disabled="productsStore.isAnalyzingReport"
          @select="handleSelectProduct"
        />
      </div>
    </div>
  </div>
</template>
