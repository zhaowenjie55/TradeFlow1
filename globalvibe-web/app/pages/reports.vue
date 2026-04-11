<script setup lang="ts">
import { getReportList } from '~/services/report'
import type { ReportListItem } from '~/types'
import { getReadableOriginalTitle, getReadableProductName, humanizeQualityTier, humanizeReportSource } from '~/utils/presentation'

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

const displayTitle = (title: string) => getReadableProductName(title)
const originalTitle = (title: string) => getReadableOriginalTitle(title, displayTitle(title))
const formatPercent = (value?: number | null) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '--'
  return `${Number(value).toFixed(1)}%`
}
const formatSignedPercent = (value?: number | null) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '--'
  const prefix = value > 0 ? '+' : ''
  return `${prefix}${formatPercent(value)}`
}
</script>

<template>
  <div class="tradeflow-scrollbar min-h-[calc(100dvh-4.5rem)] overflow-auto p-4 md:p-6 xl:p-8">
    <div class="mx-auto max-w-6xl">
      <h1 class="text-2xl font-semibold text-slate-800 dark:text-slate-100">{{ t('reportsPage.title') }}</h1>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ t('reportsPage.subtitle') }}</p>

      <div v-if="reports.length === 0" class="tradeflow-card mt-8 rounded-[var(--tf-radius-xl)] p-6 text-sm text-slate-500 dark:text-slate-400">
        {{ t('reportsPage.empty') }}
      </div>

      <div v-else class="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <NuxtLink
          v-for="report in reports"
          :key="report.reportId"
          :to="`/reports/${report.reportId}`"
          class="tradeflow-card rounded-[var(--tf-radius-xl)] p-5 transition hover:-translate-y-0.5 hover:border-[var(--tf-border-strong)]"
        >
          <p class="text-base font-semibold text-slate-800 dark:text-slate-100">{{ displayTitle(report.title) }}</p>
          <p v-if="originalTitle(report.title)" class="mt-1 line-clamp-2 text-xs leading-6 text-slate-500 dark:text-slate-400">
            {{ originalTitle(report.title) }}
          </p>
          <div class="mt-4 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
            <span>{{ t(`decision.${report.decision}`) }}</span>
            <span>{{ t(`risk.${report.riskLevel}`) }}</span>
          </div>
          <div class="mt-3 flex flex-wrap gap-2">
            <span class="rounded-full border border-slate-200 px-2.5 py-1 text-[11px] font-semibold text-slate-600 dark:border-slate-700 dark:text-slate-300">
              {{ t('report.qualityTier') }} {{ humanizeQualityTier(report.qualityTier, t) }}
            </span>
            <span class="rounded-full border border-slate-200 px-2.5 py-1 text-[11px] font-semibold text-slate-600 dark:border-slate-700 dark:text-slate-300">
              {{ t('report.fallback') }} {{ t(report.fallbackUsed ? 'common.yes' : 'common.no') }}
            </span>
          </div>
          <p class="mt-3 text-lg font-semibold text-green-600 dark:text-green-400">{{ formatSignedPercent(report.margin) }}</p>
          <div class="mt-3 space-y-1 text-[11px] text-slate-400 dark:text-slate-500">
            <p>{{ t('report.matchSource') }}: {{ humanizeReportSource(report.retrievalSource, t) }}</p>
            <p v-if="report.detailSource">{{ t('report.detailSource') }}: {{ humanizeReportSource(report.detailSource, t) }}</p>
          </div>
          <p class="mt-3 text-xs text-slate-400 dark:text-slate-500">{{ new Date(report.generatedAt).toLocaleString() }}</p>
        </NuxtLink>
      </div>
    </div>
  </div>
</template>
