<script setup lang="ts">
import type { DomesticProductMatch, ReportDetail } from '~/types'

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

const resolveMatchUrl = (match: DomesticProductMatch) => {
  return match.detailUrl ?? match.searchUrl ?? '#'
}

const formatCurrency = (value?: number | null) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '--'
  return `¥${Number(value).toFixed(2)}`
}

const formatPercent = (value?: number | null, withPlus = false) => {
  if (value === null || value === undefined || Number.isNaN(value)) return '--'
  const prefix = withPlus && value > 0 ? '+' : ''
  return `${prefix}${Number(value).toFixed(1)}%`
}

const formatDateTime = (value?: string | null) => {
  if (!value) return '--'
  return new Date(value).toLocaleString()
}

const formatList = (items?: string[] | null) => {
  if (!items?.length) return '--'
  return items.join(' / ')
}

const scoreEntries = (scoreBreakdown?: Record<string, number> | null) => {
  if (!scoreBreakdown) return []
  return Object.entries(scoreBreakdown)
}

const costItems = computed(() => {
  if (!report.value) return []

  return [
    { key: 'sourcingCost', value: report.value.costBreakdown.sourcingCost },
    { key: 'domesticShippingCost', value: report.value.costBreakdown.domesticShippingCost },
    { key: 'logisticsCost', value: report.value.costBreakdown.logisticsCost },
    { key: 'platformFee', value: report.value.costBreakdown.platformFee },
    { key: 'exchangeRateCost', value: report.value.costBreakdown.exchangeRateCost },
    { key: 'totalCost', value: report.value.costBreakdown.totalCost },
    { key: 'targetSellingPrice', value: report.value.costBreakdown.targetSellingPrice },
  ]
})

const buildMarkdownDocument = () => {
  if (!report.value) return null

  const currentReport = report.value
  const lines = [
    `# ${currentReport.title}`,
    '',
    `- ${t('products.market')}: ${currentReport.market}`,
    `- ${t('report.generatedAt')}: ${formatDateTime(currentReport.generatedAt)}`,
    `- ${t('report.decision')}: ${t(`decision.${currentReport.decision}`)}`,
    `- ${t('report.riskLevel')}: ${t(`risk.${currentReport.riskLevel}`)}`,
    `- ${t('report.expectedMargin')}: ${formatPercent(currentReport.expectedMargin)}`,
    `- ${t('report.estimatedProfit')}: ${formatCurrency(currentReport.costBreakdown.estimatedProfit)}`,
    '',
    `## ${t('report.summary')}`,
    '',
    resolveSummary(),
    '',
    `## ${t('report.costs')}`,
    '',
    `- ${t('report.sourcingCost')}: ${formatCurrency(currentReport.costBreakdown.sourcingCost)}`,
    `- ${t('report.domesticShippingCost')}: ${formatCurrency(currentReport.costBreakdown.domesticShippingCost)}`,
    `- ${t('report.logisticsCost')}: ${formatCurrency(currentReport.costBreakdown.logisticsCost)}`,
    `- ${t('report.platformFee')}: ${formatCurrency(currentReport.costBreakdown.platformFee)}`,
    `- ${t('report.exchangeRateCost')}: ${formatCurrency(currentReport.costBreakdown.exchangeRateCost)}`,
    `- ${t('report.totalCost')}: ${formatCurrency(currentReport.costBreakdown.totalCost)}`,
    `- ${t('report.targetSellingPrice')}: ${formatCurrency(currentReport.costBreakdown.targetSellingPrice)}`,
    '',
    `## ${t('report.domesticMatches')}`,
    '',
    ...currentReport.domesticMatches.flatMap(match => [
      `### ${match.platform} - ${match.title}`,
      `- ${t('report.matchPrice')}: ${formatCurrency(match.price)}`,
      `- ${t('report.matchSimilarity')}: ${match.similarityScore}%`,
      `- ${t('report.matchSource')}: ${match.matchSource ?? '--'}`,
      `- ${t('report.retrievalTerms')}: ${formatList(match.retrievalTerms)}`,
      ...(match.reason ? [`- ${t('report.matchReason')}: ${match.reason}`] : []),
      ...scoreEntries(match.scoreBreakdown).map(([key, value]) => `- ${t('report.scoreBreakdown')} ${key}: ${Number(value).toFixed(2)}`),
      ...match.evidence.map(item => `- ${t('report.evidence')}: ${item}`),
      `- ${t('report.matchLink')}: ${resolveMatchUrl(match)}`,
      '',
    ]),
  ]

  if (currentReport.analysisTrace) {
    lines.push(`## ${t('report.analysisTrace')}`, '')
    if (currentReport.analysisTrace.rewrite) {
      lines.push(`- ${t('report.sourceTitle')}: ${currentReport.analysisTrace.rewrite.sourceTitle}`)
      lines.push(`- ${t('report.rewrittenText')}: ${currentReport.analysisTrace.rewrite.rewrittenText}`)
      lines.push(`- ${t('report.keywords')}: ${formatList(currentReport.analysisTrace.rewrite.keywords)}`)
      lines.push(`- ${t('report.provider')}: ${currentReport.analysisTrace.rewrite.provider}`)
    }
    if (currentReport.analysisTrace.retrieval) {
      lines.push(`- ${t('report.retrievalTerms')}: ${formatList(currentReport.analysisTrace.retrieval.retrievalTerms)}`)
      lines.push(`- ${t('report.matchSource')}: ${currentReport.analysisTrace.retrieval.matchSource}`)
      lines.push(...scoreEntries(currentReport.analysisTrace.retrieval.scoreBreakdown).map(([key, value]) => `- ${t('report.scoreBreakdown')} ${key}: ${Number(value).toFixed(2)}`))
      lines.push(...currentReport.analysisTrace.retrieval.evidence.map(item => `- ${t('report.evidence')}: ${item}`))
    }
    if (currentReport.analysisTrace.pricing) {
      lines.push(`- ${t('report.currency')}: ${currentReport.analysisTrace.pricing.currency}`)
      lines.push(`- ${t('report.usdToCnyRate')}: ${Number(currentReport.analysisTrace.pricing.usdToCnyRate).toFixed(2)}`)
      lines.push(...currentReport.analysisTrace.pricing.formulaLines.map(item => `- ${t('report.formulaLines')}: ${item}`))
      lines.push(...currentReport.analysisTrace.pricing.assumptions.map(item => `- ${t('report.assumptions')}: ${item}`))
    }
    if (currentReport.analysisTrace.llm) {
      lines.push(`- ${t('report.provider')}: ${currentReport.analysisTrace.llm.provider}`)
      lines.push(`- ${t('report.model')}: ${currentReport.analysisTrace.llm.model}`)
      lines.push(`- ${t('report.generatedAt')}: ${formatDateTime(currentReport.analysisTrace.llm.generatedAt)}`)
    }
    lines.push('')
  }

  lines.push(`## ${t('report.risk')}`, '')
  lines.push(`- ${t('report.riskScore')}: ${currentReport.riskAssessment.score ?? '--'}`)
  lines.push(...currentReport.riskAssessment.factors.map(factor => `- ${t(`riskFactors.${factor}`)}`))
  lines.push(...(currentReport.riskAssessment.notes ?? []).map(note => `- ${note}`))
  lines.push('', `## ${t('report.recommendations')}`, '')
  lines.push(...currentReport.recommendations.map(rec => `- ${rec}`))

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
              <span class="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-950 dark:text-slate-300">
                {{ formatDateTime(report.generatedAt) }}
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
            <p class="mt-2 text-2xl font-bold text-slate-800 dark:text-slate-200">{{ formatCurrency(report.costBreakdown.estimatedProfit) }}</p>
          </div>
          <div class="rounded-xl bg-slate-100 p-5 dark:bg-slate-900">
            <p class="text-[10px] uppercase tracking-wide text-slate-500">{{ t('report.expectedMargin') }}</p>
            <p class="mt-2 text-2xl font-bold text-green-600 dark:text-slate-200">{{ formatPercent(report.expectedMargin, true) }}</p>
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
          <div
            v-for="item in costItems"
            :key="item.key"
            class="rounded-xl bg-slate-100 p-4 text-xs dark:bg-slate-900"
          >
            <p class="text-slate-500">{{ t(`report.${item.key}`) }}</p>
            <p class="mt-2 font-semibold text-slate-800 dark:text-slate-200">{{ formatCurrency(item.value) }}</p>
          </div>
        </div>
      </div>

      <div v-if="report.analysisTrace" class="space-y-4">
        <h5 class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <UIcon name="i-heroicons-command-line" class="h-3.5 w-3.5" />
          {{ t('report.analysisTrace') }}
        </h5>
        <div class="grid gap-3 md:grid-cols-2">
          <div class="rounded-2xl bg-slate-100 p-4 text-xs leading-relaxed text-slate-600 dark:bg-slate-900 dark:text-slate-300">
            <p class="font-semibold text-slate-800 dark:text-slate-100">{{ t('report.rewrite') }}</p>
            <div v-if="report.analysisTrace.rewrite" class="mt-3 space-y-2">
              <p><span class="text-slate-500">{{ t('report.sourceTitle') }}:</span> {{ report.analysisTrace.rewrite.sourceTitle }}</p>
              <p><span class="text-slate-500">{{ t('report.rewrittenText') }}:</span> {{ report.analysisTrace.rewrite.rewrittenText }}</p>
              <p><span class="text-slate-500">{{ t('report.keywords') }}:</span> {{ formatList(report.analysisTrace.rewrite.keywords) }}</p>
              <p><span class="text-slate-500">{{ t('report.provider') }}:</span> {{ report.analysisTrace.rewrite.provider }}</p>
            </div>
          </div>

          <div class="rounded-2xl bg-slate-100 p-4 text-xs leading-relaxed text-slate-600 dark:bg-slate-900 dark:text-slate-300">
            <p class="font-semibold text-slate-800 dark:text-slate-100">{{ t('report.retrieval') }}</p>
            <div v-if="report.analysisTrace.retrieval" class="mt-3 space-y-3">
              <p><span class="text-slate-500">{{ t('report.retrievalTerms') }}:</span> {{ formatList(report.analysisTrace.retrieval.retrievalTerms) }}</p>
              <p><span class="text-slate-500">{{ t('report.matchSource') }}:</span> {{ report.analysisTrace.retrieval.matchSource }}</p>
              <div>
                <p class="text-slate-500">{{ t('report.scoreBreakdown') }}</p>
                <div class="mt-2 flex flex-wrap gap-2">
                  <span
                    v-for="[key, value] in scoreEntries(report.analysisTrace.retrieval.scoreBreakdown)"
                    :key="`trace-score-${key}`"
                    class="rounded-full bg-white px-2.5 py-1 font-medium text-slate-700 dark:bg-slate-950 dark:text-slate-300"
                  >
                    {{ key }} {{ Number(value).toFixed(2) }}
                  </span>
                </div>
              </div>
              <ul class="space-y-1.5">
                <li v-for="(item, idx) in report.analysisTrace.retrieval.evidence" :key="`trace-evidence-${idx}`">
                  {{ item }}
                </li>
              </ul>
            </div>
          </div>

          <div class="rounded-2xl bg-slate-100 p-4 text-xs leading-relaxed text-slate-600 dark:bg-slate-900 dark:text-slate-300">
            <p class="font-semibold text-slate-800 dark:text-slate-100">{{ t('report.pricingTrace') }}</p>
            <div v-if="report.analysisTrace.pricing" class="mt-3 space-y-3">
              <p><span class="text-slate-500">{{ t('report.currency') }}:</span> {{ report.analysisTrace.pricing.currency }}</p>
              <p><span class="text-slate-500">{{ t('report.usdToCnyRate') }}:</span> {{ Number(report.analysisTrace.pricing.usdToCnyRate).toFixed(2) }}</p>
              <div>
                <p class="text-slate-500">{{ t('report.formulaLines') }}</p>
                <ul class="mt-2 space-y-1.5">
                  <li v-for="(item, idx) in report.analysisTrace.pricing.formulaLines" :key="`trace-formula-${idx}`">{{ item }}</li>
                </ul>
              </div>
              <div>
                <p class="text-slate-500">{{ t('report.assumptions') }}</p>
                <ul class="mt-2 space-y-1.5">
                  <li v-for="(item, idx) in report.analysisTrace.pricing.assumptions" :key="`trace-assumption-${idx}`">{{ item }}</li>
                </ul>
              </div>
            </div>
          </div>

          <div class="rounded-2xl bg-slate-100 p-4 text-xs leading-relaxed text-slate-600 dark:bg-slate-900 dark:text-slate-300">
            <p class="font-semibold text-slate-800 dark:text-slate-100">{{ t('report.llm') }}</p>
            <div v-if="report.analysisTrace.llm" class="mt-3 space-y-2">
              <p><span class="text-slate-500">{{ t('report.provider') }}:</span> {{ report.analysisTrace.llm.provider }}</p>
              <p><span class="text-slate-500">{{ t('report.model') }}:</span> {{ report.analysisTrace.llm.model }}</p>
              <p><span class="text-slate-500">{{ t('report.generatedAt') }}:</span> {{ formatDateTime(report.analysisTrace.llm.generatedAt) }}</p>
            </div>
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
                <img :src="match.image" :alt="match.title" class="h-16 w-16 object-cover" />
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="rounded-full bg-white px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wide text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                    {{ match.platform }}
                  </span>
                  <span class="rounded-full bg-emerald-100 px-2.5 py-1 text-[10px] font-semibold text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300">
                    {{ t('report.matchSimilarity') }} {{ match.similarityScore }}%
                  </span>
                  <span class="rounded-full bg-slate-200 px-2.5 py-1 text-[10px] font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                    {{ match.matchSource ?? '--' }}
                  </span>
                </div>
                <p class="mt-3 text-sm font-semibold leading-relaxed text-slate-800 dark:text-slate-100">
                  {{ match.title }}
                </p>
                <div class="mt-2 flex flex-wrap items-center gap-4 text-xs text-slate-500 dark:text-slate-400">
                  <span>{{ t('report.matchPrice') }}: <span class="font-semibold text-slate-700 dark:text-slate-200">{{ formatCurrency(match.price) }}</span></span>
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
                <p v-if="match.retrievalTerms?.length" class="mt-2 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                  {{ t('report.retrievalTerms') }}: {{ formatList(match.retrievalTerms) }}
                </p>
                <div v-if="scoreEntries(match.scoreBreakdown).length" class="mt-3 flex flex-wrap gap-2">
                  <span
                    v-for="[key, value] in scoreEntries(match.scoreBreakdown)"
                    :key="`${match.id}-${key}`"
                    class="rounded-full bg-white px-2.5 py-1 text-[10px] font-medium text-slate-700 dark:bg-slate-950 dark:text-slate-300"
                  >
                    {{ key }} {{ Number(value).toFixed(2) }}
                  </span>
                </div>
                <ul v-if="match.evidence?.length" class="mt-3 space-y-1.5 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                  <li v-for="(item, idx) in match.evidence" :key="`${match.id}-evidence-${idx}`">{{ item }}</li>
                </ul>
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
