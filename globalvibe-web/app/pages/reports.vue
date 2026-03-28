<script setup lang="ts">
import { getReportList } from '~/services/report'
import type { ReportListItem } from '~/types'

const { t } = useAppI18n()
const reports = ref<ReportListItem[]>([])

onMounted(async () => {
  try {
    const response = await getReportList()
    reports.value = response.items
  } catch {
    reports.value = []
  }
})
</script>

<template>
  <NuxtLayout name="default">
    <div class="globalvibe-scrollbar h-full overflow-auto p-8">
      <h1 class="text-2xl font-semibold text-slate-800 dark:text-slate-100">{{ t('reportsPage.title') }}</h1>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ t('reportsPage.subtitle') }}</p>

      <div v-if="reports.length === 0" class="mt-8 rounded-2xl bg-white p-6 text-sm text-slate-500 dark:bg-slate-900 dark:text-slate-400">
        {{ t('reportsPage.empty') }}
      </div>

      <div v-else class="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <NuxtLink
          v-for="report in reports"
          :key="report.reportId"
          :to="`/reports/${report.reportId}`"
          class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-800"
        >
          <p class="text-sm font-semibold text-slate-800 dark:text-slate-100">{{ report.title }}</p>
          <div class="mt-4 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
            <span>{{ t(`decision.${report.decision}`) }}</span>
            <span>{{ t(`risk.${report.riskLevel}`) }}</span>
          </div>
          <p class="mt-3 text-lg font-semibold text-green-600 dark:text-green-400">+{{ report.margin ?? '--' }}%</p>
          <p class="mt-3 text-xs text-slate-400 dark:text-slate-500">{{ new Date(report.generatedAt).toLocaleString() }}</p>
        </NuxtLink>
      </div>
    </div>
  </NuxtLayout>
</template>
