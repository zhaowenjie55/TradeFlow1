<script setup lang="ts">
import AppSelect from '~/components/ui/AppSelect.vue'
import { getDemoConfig } from '~/services/settings'
import type { AnalysisFormValues, DemoConfigResponse } from '~/types'

const { t } = useAppI18n()
const { startTask } = useTaskRunner()
const taskStore = useTaskStore()
const productsStore = useProductsStore()
const settingsStore = useSettingsStore()

const config = ref<DemoConfigResponse | null>(null)
const isSubmitting = ref(false)
const defaultMarket = 'AmazonUS'

const form = reactive<AnalysisFormValues>({
  keyword: '',
  market: defaultMarket,
  targetProfitMargin: 30,
  topN: 8,
})

const isBusy = computed(() => isSubmitting.value || taskStore.isPolling || productsStore.isAnalyzingReport)
const marketOptions = computed(() => config.value?.markets?.length ? config.value.markets : [defaultMarket])
const statusNoticeKey = computed(() => {
  if (taskStore.fallbackTriggered) return 'task.fallbackNotice'
  if (taskStore.mode === 'MOCK') return 'task.mockNotice'
  if (taskStore.mode === 'AUTO_FALLBACK') return 'task.modeNotice'
  return null
})

const syncFromSettings = () => {
  form.market = settingsStore.settings.defaultMarket || defaultMarket
}

const fillSample = () => {
  form.keyword = 'Acrylic Desktop Organizer'
  form.targetProfitMargin = 30
  form.topN = 8
  syncFromSettings()
}

const resetForm = () => {
  form.keyword = ''
  form.targetProfitMargin = 30
  form.topN = 8
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
</script>

<template>
  <div class="flex h-full min-h-0 flex-col p-6">
    <div class="mb-6">
      <h2 class="text-base font-semibold tracking-tight text-slate-800 dark:text-slate-200">{{ t('task.title') }}</h2>
      <p class="mt-1 text-sm leading-relaxed text-slate-500 dark:text-slate-500">{{ t('task.subtitle') }}</p>
    </div>

    <div class="flex min-h-0 flex-1 flex-col">
      <div class="tradeflow-scrollbar flex min-h-0 flex-1 flex-col gap-6 overflow-y-auto pr-2">
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-950/60">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-xs uppercase tracking-wide text-slate-400">{{ t('task.currentTask') }}</p>
              <p class="mt-1 text-sm font-medium text-slate-700 dark:text-slate-300">{{ form.keyword || '--' }}</p>
            </div>
            <span class="rounded-full bg-blue-100 px-2.5 py-1 text-xs font-medium text-blue-700 dark:bg-slate-900 dark:text-slate-300">
              {{ t(`status.${taskStore.status}`) }}
            </span>
          </div>

          <div class="mt-4">
            <div class="mb-2 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
              <span>{{ t('task.progress') }}</span>
              <span>{{ taskStore.progress }}%</span>
            </div>
            <div class="h-2 rounded-full bg-slate-200 dark:bg-slate-800">
              <div class="h-2 rounded-full bg-blue-500 transition-all duration-500" :style="{ width: `${taskStore.progress}%` }" />
            </div>
            <p class="mt-2 text-xs text-slate-500 dark:text-slate-400">{{ t(`stage.${taskStore.stage}`) }}</p>
          </div>
        </div>

        <div class="rounded-xl border border-blue-100 bg-blue-50/80 p-4 text-xs leading-relaxed text-blue-700 dark:border-slate-800 dark:bg-slate-950/70 dark:text-slate-300">
          <p class="font-medium text-blue-800 dark:text-slate-100">{{ t('task.backendNotice') }}</p>
          <p v-if="statusNoticeKey" class="mt-2 text-blue-700/90 dark:text-slate-400">{{ t(statusNoticeKey) }}</p>
        </div>

        <div class="space-y-2">
          <label for="keyword" class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('task.keyword') }}</label>
          <input
            id="keyword"
            v-model="form.keyword"
            type="text"
            :placeholder="t('task.keywordPlaceholder')"
            class="block w-full rounded-lg bg-white px-4 py-3 text-sm text-slate-800 placeholder:text-slate-400 ring-1 ring-slate-200 transition-all duration-200 focus:outline-none focus:ring-slate-300 dark:bg-slate-900 dark:text-slate-200 dark:placeholder:text-slate-600 dark:ring-slate-800 dark:focus:ring-slate-700"
            :disabled="isBusy"
          />
        </div>

        <div class="space-y-2">
          <label for="market" class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('task.market') }}</label>
          <AppSelect
            v-model="form.market"
            :options="marketOptions"
            :disabled="isBusy"
          />
          <p class="text-xs leading-relaxed text-slate-400 dark:text-slate-500">{{ t('task.marketHint') }}</p>
        </div>

        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <label for="topN" class="text-xs font-medium uppercase tracking-wide text-slate-500">{{ t('task.topN') }}</label>
            <span class="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600 dark:bg-slate-900 dark:text-slate-300">{{ form.topN }}</span>
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




        <div
          v-if="taskStore.errorMessage"
          class="rounded-lg border border-red-200 bg-red-50 p-3 text-xs leading-relaxed text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-200"
        >
          {{ taskStore.errorMessage }}
        </div>
      </div>

      <div class="mt-4 space-y-3 border-t border-slate-200 pt-4 dark:border-slate-800">
        <div
          v-if="productsStore.currentCandidate"
          class="rounded-xl bg-slate-100 p-3 text-xs leading-relaxed text-slate-600 dark:bg-slate-900 dark:text-slate-300"
        >
          {{ t('products.selectedTaskHint', { title: productsStore.currentCandidate.title }) }}
        </div>

        <div class="grid grid-cols-2 gap-3">
          <button
            type="button"
            class="rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-slate-600 transition hover:bg-slate-50 dark:border-slate-800 dark:text-slate-300 dark:hover:bg-slate-900"
            :disabled="isBusy"
            @click="fillSample"
          >
            {{ t('task.sampleFill') }}
          </button>
          <button
            type="button"
            class="rounded-lg border border-slate-200 px-4 py-3 text-sm font-medium text-slate-600 transition hover:bg-slate-50 dark:border-slate-800 dark:text-slate-300 dark:hover:bg-slate-900"
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
            'flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-6 py-3.5 text-sm font-semibold tracking-wide transition-all duration-200',
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
