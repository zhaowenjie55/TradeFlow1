<script setup lang="ts">
import type { ReportDetail } from '~/types'

const props = withDefaults(defineProps<{
  report?: ReportDetail | null
  candidateTitle?: string | null
  isLoading?: boolean
  allowDownload?: boolean
}>(), {
  report: undefined,
  candidateTitle: undefined,
  isLoading: undefined,
  allowDownload: true,
})

const productsStore = useProductsStore()
const storeReport = computed(() => productsStore.currentReport)
const storeCandidate = computed(() => productsStore.currentCandidate)
const report = computed(() => props.report ?? storeReport.value)
const isLoading = computed(() => props.isLoading ?? productsStore.isAnalyzingReport)
const activeTitle = computed(() => props.candidateTitle ?? storeCandidate.value?.title ?? '--')
const { t } = useAppI18n()

const riskColors = {
  low: 'text-green-700 dark:text-slate-300',
  medium: 'text-amber-700 dark:text-slate-300',
  high: 'text-red-700 dark:text-slate-300',
}

const sanitizeFileName = (value: string) => {
  return value
    .trim()
    .replace(/[<>:"/\\|?*\x00-\x1F]/g, '-')
    .replace(/\s+/g, '-')
    .toLowerCase()
}

const toTextParams = (value?: Record<string, unknown>) => {
  if (!value) return undefined

  return Object.entries(value).reduce<Record<string, string | number>>((acc, [key, current]) => {
    if (typeof current === 'string' || typeof current === 'number') {
      acc[key] = current
    } else if (current !== null && current !== undefined) {
      acc[key] = String(current)
    }
    return acc
  }, {})
}

const resolveSummary = () => {
  if (!report.value) return ''
  return t(report.value.summary.insightKey, toTextParams(report.value.summary.insightParams))
}

const resolveMatchUrl = (match: ReportDetail['domesticMatches'][number]) => {
  return match.detailUrl ?? match.searchUrl ?? '#'
}

const buildMarkdownDocument = () => {
  if (!report.value) return null

  const currentReport = report.value
  const lines = [
    `# ${currentReport.title}`,
    '',
    `- ${t('products.market')}: ${currentReport.market}`,
    `- ${t('report.generatedAt')}: ${new Date(currentReport.generatedAt).toLocaleString()}`,
    `- ${t('report.decision')}: ${t(`decision.${currentReport.decision}`)}`,
    `- ${t('report.riskLevel')}: ${t(`risk.${currentReport.riskLevel}`)}`,
    `- ${t('report.expectedMargin')}: ${currentReport.expectedMargin ?? '--'}%`,
    `- ${t('report.estimatedProfit')}: $${currentReport.costBreakdown.estimatedProfit ?? '--'}`,
    '',
    `## ${t('report.summary')}`,
    '',
    resolveSummary(),
    '',
    `## ${t('report.costs')}`,
    '',
    `- ${t('report.sourcingCost')}: $${currentReport.costBreakdown.sourcingCost ?? '--'}`,
    `- ${t('report.logisticsCost')}: $${currentReport.costBreakdown.logisticsCost ?? '--'}`,
    `- ${t('report.platformFee')}: $${currentReport.costBreakdown.platformFee ?? '--'}`,
    `- ${t('report.exchangeRateCost')}: $${currentReport.costBreakdown.exchangeRateCost ?? '--'}`,
    `- ${t('report.totalCost')}: $${currentReport.costBreakdown.totalCost ?? '--'}`,
    `- ${t('report.targetSellingPrice')}: $${currentReport.costBreakdown.targetSellingPrice ?? '--'}`,
    '',
    `## ${t('report.risk')}`,
    '',
    `- ${t('report.riskScore')}: ${currentReport.riskAssessment.score ?? '--'}`,
    ...currentReport.riskAssessment.factors.map(factor => `- ${t(`riskFactors.${factor}`)}`),
    ...(currentReport.riskAssessment.notes ?? []).map(note => `- ${note}`),
    '',
    `## ${t('report.recommendations')}`,
    '',
    ...currentReport.recommendations.map(rec => `- ${rec}`),
    '',
    `## ${t('report.domesticMatches')}`,
    '',
    ...currentReport.domesticMatches.flatMap(match => [
      `### ${match.platform} - ${match.title}`,
      `- ${t('report.matchPrice')}: $${match.price ?? '--'}`,
      `- ${t('report.matchSimilarity')}: ${match.similarityScore}%`,
      `- ${t('report.matchLink')}: ${resolveMatchUrl(match)}`,
      ...(match.reason ? [`- ${t('report.matchReason')}: ${match.reason}`] : []),
      '',
    ]),
  ]

  return {
    fileName: `${sanitizeFileName(currentReport.title)}-report.md`,
    mimeType: 'text/markdown;charset=utf-8',
    content: lines.join('\n'),
  }
}

const downloadDocument = () => {
  if (!report.value) return

  const downloadable = report.value.downloadDocument ?? buildMarkdownDocument()
  if (!downloadable) return

  const blob = new Blob([downloadable.content], { type: downloadable.mimeType })
  const objectUrl = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = downloadable.fileName
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(objectUrl)
}
</script>

<template>
  <div class="tradeflow-scrollbar h-full overflow-y-auto px-5 py-6">
    <div v-if="isLoading" class="flex h-full flex-col items-center justify-center text-center">
      <div class="rounded-2xl bg-slate-100 p-7 dark:bg-slate-900">
        <UIcon name="i-heroicons-arrow-path" class="mx-auto h-10 w-10 animate-spin text-slate-400 dark:text-slate-500" />
        <h3 class="mt-4 text-sm font-medium text-slate-600 dark:text-slate-300">
          {{ t('report.loadingTitle') }}
        </h3>
        <p class="mt-2 max-w-[260px] text-xs leading-relaxed text-slate-400 dark:text-slate-500">
          {{ t('report.loadingDescription', { title: activeTitle }) }}
        </p>
      </div>
    </div>

    <div v-else-if="!report" class="flex h-full flex-col items-center justify-center text-center">
      <div class="rounded-xl bg-slate-100 p-6 dark:bg-slate-900">
        <UIcon name="i-heroicons-document-text" class="h-10 w-10 text-slate-300 dark:text-slate-700" />
        <h3 class="mt-3 text-sm font-medium text-slate-600 dark:text-slate-400">{{ t('report.emptyTitle') }}</h3>
        <p class="mt-1 max-w-[200px] text-xs leading-relaxed text-slate-400 dark:text-slate-500">
          {{ t('report.emptyDescription') }}
        </p>
      </div>
    </div>

    <div v-else class="space-y-6">
      <div v-if="allowDownload" class="flex justify-start">
        <button
          type="button"
          class="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200 dark:hover:border-slate-600 dark:hover:bg-slate-900"
          @click="downloadDocument"
        >
          <UIcon name="i-heroicons-arrow-down-tray" class="h-4 w-4" />
          {{ t('report.download') }}
        </button>
      </div>

      <div class="rounded-2xl bg-slate-100 p-4 dark:bg-slate-900">
        <div class="flex min-w-0 gap-4">
          <div class="flex h-20 w-20 items-center justify-center overflow-hidden rounded-xl bg-white dark:bg-slate-950">
            <img v-if="report.image" :src="report.image" :alt="report.title" class="h-20 w-20 object-cover" />
            <UIcon v-else name="i-heroicons-photo" class="h-8 w-8 text-slate-400 dark:text-slate-600" />
          </div>

          <div class="min-w-0 flex-1">
            <h4 class="truncate text-base font-semibold text-slate-800 dark:text-slate-200">{{ report.title }}</h4>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-500">{{ report.market }}</p>
            <div class="mt-3 flex flex-wrap items-center gap-2">
              <span class="rounded-full bg-blue-100 px-2.5 py-1 text-xs font-semibold text-blue-700 dark:bg-slate-800 dark:text-slate-300">
                {{ t(`decision.${report.decision}`) }}
              </span>
              <span class="rounded-full bg-slate-200 px-2.5 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                {{ t(`risk.${report.riskLevel}`) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="space-y-4">
        <h5 class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <UIcon name="i-heroicons-arrow-trending-up" class="h-3.5 w-3.5" />
          {{ t('report.summary') }}
        </h5>
        <div class="grid grid-cols-2 gap-3">
          <div class="rounded-xl bg-slate-100 p-5 dark:bg-slate-900">
            <p class="text-[10px] uppercase tracking-wide text-slate-500">{{ t('report.estimatedProfit') }}</p>
            <p class="mt-2 text-2xl font-bold text-slate-800 dark:text-slate-200">${{ report.costBreakdown.estimatedProfit ?? '--' }}</p>
          </div>
          <div class="rounded-xl bg-slate-100 p-5 dark:bg-slate-900">
            <p class="text-[10px] uppercase tracking-wide text-slate-500">{{ t('report.expectedMargin') }}</p>
            <p class="mt-2 text-2xl font-bold text-green-600 dark:text-slate-200">+{{ report.expectedMargin ?? '--' }}%</p>
          </div>
        </div>
        <p class="rounded-xl bg-slate-100 p-4 text-sm leading-relaxed text-slate-600 dark:bg-slate-900 dark:text-slate-400">
          {{ resolveSummary() }}
        </p>
      </div>

      <div class="space-y-4">
        <h5 class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <UIcon name="i-heroicons-banknotes" class="h-3.5 w-3.5" />
          {{ t('report.costs') }}
        </h5>
        <div class="grid grid-cols-2 gap-3">
          <div class="rounded-xl bg-slate-100 p-4 text-xs dark:bg-slate-900">
            <p class="text-slate-500">{{ t('report.sourcingCost') }}</p>
            <p class="mt-2 font-semibold text-slate-800 dark:text-slate-200">${{ report.costBreakdown.sourcingCost ?? '--' }}</p>
          </div>
          <div class="rounded-xl bg-slate-100 p-4 text-xs dark:bg-slate-900">
            <p class="text-slate-500">{{ t('report.logisticsCost') }}</p>
            <p class="mt-2 font-semibold text-slate-800 dark:text-slate-200">${{ report.costBreakdown.logisticsCost ?? '--' }}</p>
          </div>
          <div class="rounded-xl bg-slate-100 p-4 text-xs dark:bg-slate-900">
            <p class="text-slate-500">{{ t('report.platformFee') }}</p>
            <p class="mt-2 font-semibold text-slate-800 dark:text-slate-200">${{ report.costBreakdown.platformFee ?? '--' }}</p>
          </div>
          <div class="rounded-xl bg-slate-100 p-4 text-xs dark:bg-slate-900">
            <p class="text-slate-500">{{ t('report.exchangeRateCost') }}</p>
            <p class="mt-2 font-semibold text-slate-800 dark:text-slate-200">${{ report.costBreakdown.exchangeRateCost ?? '--' }}</p>
          </div>
        </div>
      </div>

      <div class="space-y-4">
        <h5 class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <UIcon name="i-heroicons-shield-check" class="h-3.5 w-3.5" />
          {{ t('report.risk') }}
        </h5>
        <div class="rounded-xl bg-slate-100 p-5 dark:bg-slate-900">
          <div class="flex items-center justify-between">
            <span :class="['text-sm font-semibold', riskColors[report.riskLevel]]">{{ t(`risk.${report.riskLevel}`) }}</span>
            <span class="text-3xl font-bold text-slate-600 dark:text-slate-300">{{ report.riskAssessment.score ?? '--' }}</span>
          </div>
          <ul class="mt-3 space-y-2">
            <li
              v-for="(factor, idx) in report.riskAssessment.factors"
              :key="`factor-${idx}`"
              class="flex items-center gap-2 text-xs leading-relaxed text-slate-500 dark:text-slate-400"
            >
              <UIcon name="i-heroicons-check" class="h-3.5 w-3.5 text-slate-400 dark:text-slate-600" />
              {{ t(`riskFactors.${factor}`) }}
            </li>
            <li
              v-for="(note, idx) in report.riskAssessment.notes ?? []"
              :key="`note-${idx}`"
              class="flex items-start gap-2 text-xs leading-relaxed text-slate-500 dark:text-slate-400"
            >
              <UIcon name="i-heroicons-information-circle" class="mt-0.5 h-3.5 w-3.5 text-slate-400 dark:text-slate-600" />
              {{ note }}
            </li>
          </ul>
        </div>
      </div>

      <div class="space-y-4">
        <h5 class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <UIcon name="i-heroicons-link" class="h-3.5 w-3.5" />
          {{ t('report.domesticMatches') }}
        </h5>
        <div class="space-y-3">
          <div
            v-for="match in report.domesticMatches"
            :key="match.id"
            class="rounded-2xl bg-slate-100 p-4 dark:bg-slate-900"
          >
            <div class="flex items-start gap-3">
              <div v-if="match.image" class="flex h-16 w-16 items-center justify-center overflow-hidden rounded-xl bg-white dark:bg-slate-950">
                <img
                  :src="match.image"
                  :alt="match.title"
                  class="h-16 w-16 object-cover"
                />
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="rounded-full bg-white px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wide text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                    {{ match.platform }}
                  </span>
                  <span class="rounded-full bg-emerald-100 px-2.5 py-1 text-[10px] font-semibold text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300">
                    {{ t('report.matchSimilarity') }} {{ match.similarityScore }}%
                  </span>
                </div>
                <p class="mt-3 text-sm font-semibold leading-relaxed text-slate-800 dark:text-slate-100">
                  {{ match.title }}
                </p>
                <div class="mt-2 flex flex-wrap items-center gap-4 text-xs text-slate-500 dark:text-slate-400">
                  <span>{{ t('report.matchPrice') }}: <span class="font-semibold text-slate-700 dark:text-slate-200">${{ match.price ?? '--' }}</span></span>
                  <a
                    :href="resolveMatchUrl(match)"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="inline-flex items-center gap-1 font-medium text-blue-600 transition hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300"
                  >
                    {{ t('report.matchLinkAction') }}
                    <UIcon name="i-heroicons-arrow-top-right-on-square" class="h-3.5 w-3.5" />
                  </a>
                </div>
                <p v-if="match.reason" class="mt-2 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                  {{ match.reason }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="space-y-4">
        <h5 class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <UIcon name="i-heroicons-light-bulb" class="h-3.5 w-3.5" />
          {{ t('report.recommendations') }}
        </h5>
        <ul class="space-y-2">
          <li
            v-for="(rec, idx) in report.recommendations"
            :key="idx"
            class="flex items-start gap-2.5 rounded-xl bg-slate-100 p-4 text-xs leading-relaxed text-slate-600 dark:bg-slate-900 dark:text-slate-400"
          >
            <UIcon name="i-heroicons-check-circle" class="mt-0.5 h-4 w-4 flex-shrink-0 text-slate-400 dark:text-slate-500" />
            {{ rec }}
          </li>
        </ul>
      </div>

      <p class="text-center text-[10px] text-slate-400 dark:text-slate-600">
        {{ new Date(report.generatedAt).toLocaleTimeString() }}
      </p>
    </div>
  </div>
</template>
