<script setup lang="ts">
import AppSelect from '~/components/ui/AppSelect.vue'
import { getDemoConfig } from '~/services/settings'
import type { AnalysisFormValues, DemoConfigResponse } from '~/types'
import { getReadableProductName } from '~/utils/presentation'

const { t } = useAppI18n()
const { startTask } = useTaskRunner()
const taskStore = useTaskStore()
const productsStore = useProductsStore()
const settingsStore = useSettingsStore()
const props = withDefaults(defineProps<{
  compact?: boolean
}>(), {
  compact: false,
})

const config = ref<DemoConfigResponse | null>(null)
const isSubmitting = ref(false)
const defaultMarket = 'AmazonUS'

const form = reactive<AnalysisFormValues>({
  keyword: '',
  market: defaultMarket,
  targetProfitMargin: 30,
  topN: 9,
})

const isBusy = computed(() => isSubmitting.value || taskStore.isPolling || productsStore.isAnalyzingReport)
const marketOptions = computed(() => config.value?.markets?.length ? config.value.markets : [defaultMarket])
const syncFromSettings = () => {
  form.market = settingsStore.settings.defaultMarket || defaultMarket
}

const fillSample = () => {
  form.keyword = 'Acrylic Desktop Organizer'
  form.targetProfitMargin = 30
  form.topN = 9
  syncFromSettings()
}

const resetForm = () => {
  form.keyword = ''
  form.targetProfitMargin = 30
  form.topN = 9
  syncFromSettings()
}

const handleSubmit = async () => {
  if (!form.keyword.trim()) return

  isSubmitting.value = true

  try {
    await startTask({ ...form })
  } catch {
    // Error state is already written into the task store.
  } finally {
    isSubmitting.value = false
  }
}

onMounted(async () => {
  try {
    config.value = await getDemoConfig()
  } catch {
    config.value = null
  }
  syncFromSettings()
})

const selectedDisplayTitle = computed(() => {
  if (!productsStore.currentCandidate?.title) return ''
  return getReadableProductName(productsStore.currentCandidate.title)
})

const selectedCandidateMeta = computed(() => {
  if (!productsStore.currentCandidate) return []

  return [
    {
      key: 'margin',
      label: t('products.margin'),
      value: productsStore.currentCandidate.estimatedMargin !== null ? `+${productsStore.currentCandidate.estimatedMargin}%` : '--',
    },
    {
      key: 'risk',
      label: t('products.riskTag'),
      value: productsStore.currentCandidate.riskTag ?? '--',
    },
  ]
})

const panelClass = computed(() => props.compact ? 'h-auto p-5' : 'h-full px-4 py-4 xl:px-5 xl:py-4')
const scrollClass = computed(() => props.compact ? 'gap-5 pr-0' : 'gap-3.5 pr-1')
</script>

<template>
  <div :class="['flex min-h-0 flex-col', panelClass]">
    <div class="mb-3">
      <div class="flex items-center justify-between gap-3">
        <div>
          <h2 class="text-lg font-semibold tracking-tight text-slate-900 dark:text-slate-100">{{ t('task.title') }}</h2>
          <p class="mt-0.5 text-[13px] leading-5 text-slate-500 dark:text-slate-400">{{ t('task.subtitle') }}</p>
        </div>
        <span class="rounded-full border border-[var(--tf-border)] bg-white/80 px-2.5 py-1 text-[11px] font-medium text-slate-600 dark:bg-slate-950/80 dark:text-slate-300">
          {{ t(`status.${taskStore.status}`) }}
        </span>
      </div>
    </div>

    <div class="flex min-h-0 flex-1 flex-col">
      <div :class="['tradeflow-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto', scrollClass]">
        <div class="tradeflow-panel rounded-[var(--tf-radius-xl)] px-4 py-3.5">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="tradeflow-section-title">{{ t('task.currentTask') }}</p>
              <p class="mt-1.5 line-clamp-2 text-[15px] font-semibold leading-6 text-slate-900 dark:text-slate-100">{{ form.keyword || '--' }}</p>
              <p class="mt-0.5 text-[13px] text-slate-500 dark:text-slate-400">{{ t(`common.${taskStore.mode}`) }}</p>
            </div>
          </div>

          <div class="mt-3">
            <div class="mb-1.5 flex items-center justify-between text-[13px] text-slate-500 dark:text-slate-400">
              <span>{{ t('task.progress') }}</span>
              <span class="font-medium text-slate-700 dark:text-slate-200">{{ taskStore.progress }}%</span>
            </div>
            <div class="h-2 rounded-full bg-slate-200/80 dark:bg-slate-800">
              <div class="h-2 rounded-full bg-blue-500 transition-all duration-500" :style="{ width: `${taskStore.progress}%` }" />
            </div>
            <p class="mt-1.5 text-[13px] text-slate-500 dark:text-slate-400">{{ t(`stage.${taskStore.stage}`) }}</p>
          </div>
        </div>

        <div class="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] px-4 py-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
          <p class="line-clamp-2 font-medium leading-5 text-slate-800 dark:text-slate-100">{{ t('task.backendNotice') }}</p>
        </div>

        <div class="space-y-2">
          <label for="keyword" class="tradeflow-section-title">{{ t('task.keyword') }}</label>
          <input
            id="keyword"
            v-model="form.keyword"
            type="text"
            :placeholder="t('task.keywordPlaceholder')"
            class="block w-full rounded-2xl border border-[var(--tf-border)] bg-white px-4 py-3 text-[15px] text-slate-800 shadow-sm transition-all duration-200 placeholder:text-slate-400 focus:border-[var(--tf-border-strong)] focus:outline-none dark:bg-slate-900 dark:text-slate-200 dark:placeholder:text-slate-600"
            :disabled="isBusy"
          />
        </div>

        <div class="grid gap-3 xl:grid-cols-[minmax(0,1fr)_9rem]">
          <div class="space-y-2">
            <label for="market" class="tradeflow-section-title">{{ t('task.market') }}</label>
          <AppSelect
            id="market"
            v-model="form.market"
            :options="marketOptions"
            :disabled="isBusy"
          />
        </div>

          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <label for="topN" class="tradeflow-section-title">{{ t('task.topN') }}</label>
              <span class="rounded-full border border-[var(--tf-border)] bg-white/70 px-2.5 py-1 text-[12px] font-semibold text-slate-700 dark:bg-slate-950/80 dark:text-slate-200">{{ form.topN }}</span>
            </div>
            <input
              id="topN"
              v-model="form.topN"
              type="range"
              min="5"
              max="20"
              step="1"
              class="h-2 w-full cursor-pointer rounded-full bg-slate-200 accent-blue-500 dark:bg-slate-800 dark:accent-slate-500"
              :disabled="isBusy"
            />
          </div>
        </div>

        <div
          v-if="taskStore.errorMessage"
          class="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm leading-6 text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-200"
        >
          {{ taskStore.errorMessage }}
        </div>

        <div
          v-if="productsStore.currentCandidate"
          class="tradeflow-panel rounded-[var(--tf-radius-xl)] px-4 py-3.5"
        >
          <p class="tradeflow-section-title">{{ t('products.selectedTitle') }}</p>
          <p class="mt-1.5 line-clamp-2 text-[15px] font-semibold leading-6 text-slate-900 dark:text-slate-100">{{ selectedDisplayTitle }}</p>
          <p class="mt-1.5 line-clamp-2 text-[13px] leading-5 text-slate-500 dark:text-slate-400">
            {{ t('products.selectedTaskHint', { title: selectedDisplayTitle }) }}
          </p>
          <div class="mt-3 grid grid-cols-2 gap-2.5">
            <div
              v-for="item in selectedCandidateMeta"
              :key="item.key"
              class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-2.5 dark:bg-slate-950/70"
            >
              <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">{{ item.label }}</p>
              <p class="mt-1 text-sm font-semibold text-slate-800 dark:text-slate-100">{{ item.value }}</p>
            </div>
          </div>
        </div>
      </div>

      <div :class="['mt-3 space-y-2.5 border-t border-[var(--tf-border)] pt-3', props.compact ? 'sticky bottom-0 bg-transparent' : '']">
        <div class="grid grid-cols-2 gap-3">
          <button
            type="button"
            class="rounded-2xl border border-[var(--tf-border)] bg-white/70 px-4 py-2.5 text-sm font-medium text-slate-600 transition hover:border-[var(--tf-border-strong)] hover:bg-white dark:bg-slate-950/70 dark:text-slate-300 dark:hover:bg-slate-900"
            :disabled="isBusy"
            @click="fillSample"
          >
            {{ t('task.sampleFill') }}
          </button>
          <button
            type="button"
            class="rounded-2xl border border-[var(--tf-border)] bg-white/70 px-4 py-3 text-sm font-medium text-slate-600 transition hover:border-[var(--tf-border-strong)] hover:bg-white dark:bg-slate-950/70 dark:text-slate-300 dark:hover:bg-slate-900"
            :disabled="isBusy"
            @click="resetForm"
          >
            {{ t('task.reset') }}
          </button>
        </div>

        <button
          type="button"
          :disabled="isBusy || !form.keyword.trim()"
          :class="[
            'flex w-full items-center justify-center gap-2 rounded-2xl bg-blue-600 px-6 py-3 text-sm font-semibold tracking-wide transition-all duration-200',
            isBusy
              ? 'cursor-not-allowed bg-slate-100 text-slate-400 dark:bg-slate-800 dark:text-slate-500'
              : 'text-white hover:bg-blue-500',
          ]"
          @click="handleSubmit"
        >
          <UIcon v-if="isBusy" name="i-heroicons-arrow-path" class="h-4 w-4 animate-spin" />
          <UIcon v-else name="i-heroicons-play" class="h-4 w-4" />
          {{ isBusy ? t('task.submitting') : t('task.submit') }}
        </button>
      </div>
    </div>
  </div>
</template>
