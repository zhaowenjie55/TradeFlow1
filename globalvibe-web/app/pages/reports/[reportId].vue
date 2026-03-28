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
  <NuxtLayout name="default">
    <div class="tradeflow-scrollbar h-full overflow-auto p-8">
      <div class="flex items-center gap-3">
        <NuxtLink
          to="/reports"
          class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-950"
        >
          <UIcon name="i-heroicons-arrow-left" class="h-4 w-4" />
          {{ t('reportsPage.backToList') }}
        </NuxtLink>
      </div>

      <div v-if="errorMessage" class="mt-6 rounded-2xl bg-red-50 p-6 text-sm text-red-700 ring-1 ring-red-100 dark:bg-red-950/30 dark:text-red-200 dark:ring-red-900/50">
        {{ errorMessage }}
      </div>

      <div v-else class="mt-6 h-[calc(100vh-15rem)] min-h-[38rem] rounded-3xl bg-white ring-1 ring-slate-200 dark:bg-slate-950 dark:ring-slate-800">
        <ArbitrageReport
          :report="report"
          :candidate-title="report?.title ?? null"
          :is-loading="isLoading"
        />
      </div>
    </div>
  </NuxtLayout>
</template>
