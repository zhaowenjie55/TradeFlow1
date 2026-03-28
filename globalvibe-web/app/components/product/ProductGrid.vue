<script setup lang="ts">
import ProductCard from '~/components/product/ProductCard.vue'

const productsStore = useProductsStore()
const taskStore = useTaskStore()
const { analyzeProduct } = useTaskRunner()
const { t } = useAppI18n()
const containerRef = ref<HTMLElement | null>(null)
const topHeight = ref(36)

const gridCols = 'grid-cols-1 md:grid-cols-2 xl:grid-cols-3'

const workflowSteps = computed(() => {
  const hasCandidates = productsStore.filteredCandidates.length > 0
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
    topHeight.value = Math.min(62, Math.max(24, next))
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
    <div class="globalvibe-scrollbar min-h-0 overflow-auto border-b border-slate-200 dark:border-slate-800" :style="{ flexBasis: `${topHeight}%` }">
      <div class="px-6 py-4">
        <div class="flex items-start justify-between gap-4">
          <div>
            <div class="flex items-center gap-2">
              <UIcon name="i-heroicons-shopping-bag" class="h-5 w-5 text-slate-400 dark:text-slate-500" />
              <span class="font-medium text-slate-700 dark:text-slate-300">{{ t('products.title') }}</span>
              <span v-if="productsStore.filteredCandidates.length > 0" class="ml-1 text-sm text-slate-400 dark:text-slate-500">
                {{ t('products.count', { count: productsStore.filteredCandidates.length }) }}
              </span>
            </div>
            <p class="mt-2 max-w-2xl text-sm leading-relaxed text-slate-500 dark:text-slate-400">
              {{ t('products.subtitle') }}
            </p>
          </div>

          <span
            v-if="taskStore.status !== 'IDLE'"
            class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600 dark:bg-slate-900 dark:text-slate-300"
          >
            {{ t(`stage.${taskStore.stage}`) }}
          </span>
        </div>

        <div class="mt-4 grid gap-3 xl:grid-cols-3">
          <div
            v-for="(step, index) in workflowSteps"
            :key="step.key"
            :class="[
              'rounded-2xl border p-4 transition-colors',
              workflowCardClass(step.state),
            ]"
          >
            <div class="flex items-center gap-2">
              <span class="flex h-6 w-6 items-center justify-center rounded-full bg-white/80 text-[11px] font-semibold dark:bg-slate-900/60">
                {{ index + 1 }}
              </span>
              <p class="text-sm font-semibold">{{ t(step.titleKey) }}</p>
            </div>
            <p class="mt-2 text-xs leading-relaxed opacity-90">
              {{ t(step.descriptionKey) }}
            </p>
          </div>
        </div>

        <div
          v-if="productsStore.filteredCandidates.length > 0"
          class="mt-4 rounded-xl bg-slate-100 px-4 py-3 text-sm text-slate-600 dark:bg-slate-900 dark:text-slate-300"
        >
          {{ productsStore.isAnalyzingReport ? t('products.selectionHint') : t('products.selectionReady') }}
        </div>

        <div
          v-if="productsStore.currentCandidate"
          class="mt-4 rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-800 dark:bg-slate-950/50"
        >
          <div class="flex flex-col gap-4 xl:flex-row xl:items-start">
            <div class="flex h-16 w-16 items-center justify-center overflow-hidden rounded-xl bg-slate-100 dark:bg-slate-800">
              <img
                v-if="productsStore.currentCandidate.imageUrl"
                :src="productsStore.currentCandidate.imageUrl"
                :alt="productsStore.currentCandidate.title"
                class="h-16 w-16 object-cover"
              />
              <UIcon v-else name="i-heroicons-photo" class="h-8 w-8 text-slate-400 dark:text-slate-600" />
            </div>

            <div class="min-w-0 flex-1">
              <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400 dark:text-slate-500">
                {{ t('products.selectedTitle') }}
              </p>
              <h3 class="mt-1 truncate text-sm font-semibold text-slate-800 dark:text-slate-100">
                {{ productsStore.currentCandidate.title }}
              </h3>
              <p class="mt-1 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                {{
                  productsStore.isAnalyzingReport
                    ? t('products.selectedDescriptionLoading', { title: productsStore.currentCandidate.title })
                    : t('products.selectedDescriptionReady', { title: productsStore.currentCandidate.title })
                }}
              </p>
              <p v-if="productsStore.currentCandidate.recommendationReason" class="mt-3 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                {{ productsStore.currentCandidate.recommendationReason }}
              </p>
            </div>

            <div class="grid grid-cols-1 gap-3 text-xs text-slate-500 sm:grid-cols-3 xl:text-right dark:text-slate-400">
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
      type="button"
      aria-label="Resize product panels"
      class="group flex h-4 shrink-0 cursor-row-resize items-center justify-center bg-white/70 transition-colors hover:bg-slate-100 dark:bg-slate-900/70 dark:hover:bg-slate-800"
      @mousedown="startResize"
    >
      <span class="h-1 w-14 rounded-full bg-slate-300 transition-colors group-hover:bg-slate-400 dark:bg-slate-700 dark:group-hover:bg-slate-500" />
    </button>

    <div class="globalvibe-scrollbar min-h-0 flex-1 overflow-auto p-6">
      <div v-if="productsStore.isLoading && productsStore.filteredCandidates.length === 0" :class="['grid gap-6', gridCols]">
        <div
          v-for="i in 8"
          :key="i"
          class="animate-pulse rounded-xl bg-slate-100 p-4 dark:bg-slate-900"
        >
          <div class="aspect-square rounded-lg bg-slate-200 dark:bg-slate-800" />
          <div class="mt-3 h-4 rounded bg-slate-200 dark:bg-slate-800" />
          <div class="mt-2 h-4 w-2/3 rounded bg-slate-200 dark:bg-slate-800" />
        </div>
      </div>

      <div
        v-else-if="productsStore.filteredCandidates.length === 0"
        class="flex h-full flex-col items-center justify-center text-center"
      >
        <UIcon name="i-heroicons-cube" class="h-16 w-16 text-slate-300 dark:text-slate-700" />
        <h3 class="mt-4 text-lg font-medium text-slate-600 dark:text-slate-400">{{ t('products.emptyTitle') }}</h3>
        <p class="mt-2 max-w-xs text-sm leading-relaxed text-slate-400 dark:text-slate-500">
          {{ t('products.emptyDescription') }}
        </p>
      </div>

      <div v-else :class="['grid gap-6', gridCols]">
        <ProductCard
          v-for="product in productsStore.filteredCandidates"
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
