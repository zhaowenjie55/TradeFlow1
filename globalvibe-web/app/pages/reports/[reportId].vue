<script setup lang="ts">
import ArbitrageReport from '~/components/report/ArbitrageReport.vue'
import { getReportByReportId } from '~/services/report'
import type { ReportDetail } from '~/types'

const route = useRoute()
const { t } = useAppI18n()
const report = ref<ReportDetail | null>(null)
const isLoading = ref(true)
const errorMessage = ref('')

onMounted(async () => {
  try {
    report.value = await getReportByReportId(String(route.params.reportId))
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Failed to load report'
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <div class="tradeflow-scrollbar min-h-[calc(100dvh-4.5rem)] overflow-auto p-4 md:p-6 xl:p-8">
    <div class="mx-auto max-w-6xl">
      <div class="flex items-center gap-3">
        <NuxtLink
          to="/reports"
          class="inline-flex items-center gap-2 rounded-2xl border border-[var(--tf-border)] bg-white/80 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-white dark:bg-slate-900 dark:text-slate-200"
        >
          <UIcon name="i-heroicons-arrow-left" class="h-4 w-4" />
          {{ t('reportsPage.backToList') }}
        </NuxtLink>
      </div>

      <div v-if="errorMessage" class="mt-6 rounded-2xl bg-red-50 p-6 text-sm text-red-700 ring-1 ring-red-100 dark:bg-red-950/30 dark:text-red-200 dark:ring-red-900/50">
        {{ errorMessage }}
      </div>

      <div v-else class="tradeflow-panel mt-6 overflow-hidden rounded-[var(--tf-radius-2xl)]">
        <ArbitrageReport
          :report="report"
          :candidate-title="report?.title ?? null"
          :is-loading="isLoading"
        />
      </div>
    </div>
  </div>
</template>
