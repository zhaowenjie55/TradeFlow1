<script setup lang="ts">
import type { DomesticProductMatch, ReportDetail } from '~/types'
import { compactNarrativeSummary, getReadableMatchTitle, getReadableOriginalTitle, getReadableProductName, getReportFileName } from '~/utils/presentation'

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
const activeTitle = computed(() => getReadableProductName(props.candidateTitle ?? storeCandidate.value?.title ?? '--'))
const { t } = useAppI18n()
const isPreviewOpen = ref(false)
const traceSectionState = reactive({
  rewrite: false,
  retrieval: false,
  pricing: false,
  llm: false,
})

const riskColors = {
  low: 'text-green-700 dark:text-slate-300',
  medium: 'text-amber-700 dark:text-slate-300',
  high: 'text-red-700 dark:text-slate-300',
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
  const rawSummary = t(report.value.summary.insightKey, toTextParams(report.value.summary.insightParams))
  return compactNarrativeSummary(rawSummary, {
    productTitle: report.value.title,
    domesticTitle: report.value.domesticMatches?.[0]?.title,
  })
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

const displayTitle = computed(() => {
  if (!report.value?.title) return '--'
  return getReadableProductName(report.value.title, {
    domesticHint: report.value.domesticMatches?.[0]?.title,
  })
})

const originalTitle = computed(() => {
  if (!report.value?.title) return ''
  return getReadableOriginalTitle(report.value.title, displayTitle.value)
})

const displayMatchTitle = (match: DomesticProductMatch) => getReadableMatchTitle(match)
const originalMatchTitle = (match: DomesticProductMatch) => getReadableOriginalTitle(match.title, displayMatchTitle(match))
const detailReadyLabel = (match: DomesticProductMatch) => t(match.detailReady ? 'report.detailReady' : 'report.detailPending')
const detailSourceLabel = (match: DomesticProductMatch) => match.detailSource ?? '--'

const headlineMetrics = computed(() => {
  if (!report.value) return []

  return [
    { key: 'profit', label: t('report.estimatedProfit'), value: formatCurrency(report.value.costBreakdown.estimatedProfit) },
    { key: 'margin', label: t('report.expectedMargin'), value: formatPercent(report.value.expectedMargin, true) },
    { key: 'decision', label: t('report.decision'), value: t(`decision.${report.value.decision}`) },
    { key: 'risk', label: t('report.riskLevel'), value: t(`risk.${report.value.riskLevel}`) },
  ]
})

const getTraceSummary = (section: keyof typeof traceSectionState) => {
  if (!report.value?.analysisTrace) return ''

  if (section === 'rewrite' && report.value.analysisTrace.rewrite) {
    return `${report.value.analysisTrace.rewrite.rewrittenText} / ${report.value.analysisTrace.rewrite.keywords.length} 个扩展词`
  }

  if (section === 'retrieval' && report.value.analysisTrace.retrieval) {
    return `${formatList(report.value.analysisTrace.retrieval.retrievalTerms)}`
  }

  if (section === 'pricing' && report.value.analysisTrace.pricing) {
    return `${report.value.analysisTrace.pricing.formulaLines.length} 条测算公式`
  }

  if (section === 'llm' && report.value.analysisTrace.llm) {
    return `${report.value.analysisTrace.llm.provider} / ${report.value.analysisTrace.llm.model}`
  }

  return ''
}

const buildMarkdownDocument = () => {
  if (!report.value) return null

  const currentReport = report.value
  const lines = [
    `# ${displayTitle.value}`,
    ...(originalTitle.value ? ['', `原始标题：${currentReport.title}`] : []),
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
      `### ${match.platform} - ${displayMatchTitle(match)}`,
      ...(originalMatchTitle(match) ? [`- 原始标题: ${match.title}`] : []),
      `- ${t('report.matchPrice')}: ${formatCurrency(match.price)}`,
      `- ${t('report.matchSimilarity')}: ${match.similarityScore}%`,
      `- ${t('report.matchSource')}: ${match.matchSource ?? '--'}`,
      `- ${t('report.detailStatus')}: ${detailReadyLabel(match)}`,
      `- ${t('report.detailSource')}: ${detailSourceLabel(match)}`,
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
    fileName: getReportFileName(currentReport.title, currentReport.domesticMatches?.[0]?.title),
    mimeType: 'text/markdown;charset=utf-8',
    content: lines.join('\n'),
  }
}

const previewDocument = computed(() => {
  if (!report.value) return null
  return report.value.downloadDocument ?? buildMarkdownDocument()
})

const previewDisplayTitle = computed(() => {
  if (!report.value?.title) return '--'
  return getReadableProductName(report.value.title, {
    domesticHint: report.value.domesticMatches?.[0]?.title,
  })
})

const previewKeyFacts = computed(() => {
  if (!report.value) return []

  return [
    { label: t('products.market'), value: report.value.market },
    { label: t('report.decision'), value: t(`decision.${report.value.decision}`) },
    { label: t('report.riskLevel'), value: t(`risk.${report.value.riskLevel}`) },
    { label: t('report.expectedMargin'), value: formatPercent(report.value.expectedMargin, true) },
    { label: t('report.estimatedProfit'), value: formatCurrency(report.value.costBreakdown.estimatedProfit) },
    { label: t('report.generatedAt'), value: formatDateTime(report.value.generatedAt) },
  ]
})

const previewTopMatch = computed(() => report.value?.domesticMatches?.[0] ?? null)

const openProductOnMarket = () => {
  if (!report.value || !import.meta.client) return
  const market = report.value.market.toLowerCase()
  if (market.includes('amazon') && report.value.productId) {
    window.open(`https://www.amazon.com/dp/${report.value.productId}`, '_blank', 'noopener,noreferrer')
  }
}

const formattedScoreEntries = (scoreBreakdown?: Record<string, number> | null) => {
  const scoreLabelMap: Record<string, string> = {
    titleOverlap: '标题重合',
    rewriteCoverage: '改写覆盖',
    priceReasonability: '价格合理性',
    vectorBoost: '语义召回',
    attributeAlignment: '属性对齐',
  }

  return scoreEntries(scoreBreakdown).map(([key, value]) => ({
    key,
    label: scoreLabelMap[key] ?? key.replace(/([A-Z])/g, ' $1').replace(/^./, letter => letter.toUpperCase()),
    value: Number(value).toFixed(2),
  }))
}

const openPreview = () => {
  if (!previewDocument.value) return
  isPreviewOpen.value = true
}

const closePreview = () => {
  isPreviewOpen.value = false
}

const handlePreviewKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    closePreview()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handlePreviewKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handlePreviewKeydown)
})

const downloadDocument = () => {
  if (!report.value) return

  const downloadable = report.value.downloadDocument ?? buildMarkdownDocument()
  if (!downloadable) return

  const blob = new Blob([downloadable.content], { type: downloadable.mimeType })
  const objectUrl = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = getReportFileName(report.value.title, report.value.domesticMatches?.[0]?.title)
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(objectUrl)
}
</script>

<template>
  <div class="tradeflow-scrollbar h-full overflow-y-auto px-4 py-4">
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

    <div v-else class="space-y-4">
      <div v-if="allowDownload" class="flex flex-wrap gap-2.5">
        <button
          type="button"
          class="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200 dark:hover:border-slate-600 dark:hover:bg-slate-900"
          @click="downloadDocument"
        >
          <UIcon name="i-heroicons-arrow-down-tray" class="h-4 w-4" />
          {{ t('report.download') }}
        </button>

        <button
          type="button"
          class="flex items-center gap-2 rounded-xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] px-4 py-2.5 text-sm font-medium text-slate-700 shadow-sm transition hover:border-[var(--tf-border-strong)] hover:bg-white dark:text-slate-200 dark:hover:bg-slate-900"
          @click="openPreview"
        >
          <UIcon name="i-heroicons-eye" class="h-4 w-4" />
          {{ t('report.preview') }}
        </button>
      </div>

      <div class="rounded-3xl border border-slate-200/70 bg-gradient-to-br from-white via-slate-50 to-slate-100 p-4 shadow-sm dark:border-slate-800 dark:from-slate-900 dark:via-slate-900 dark:to-slate-950">
        <div class="flex min-w-0 gap-3">
          <div class="flex h-16 w-16 items-center justify-center overflow-hidden rounded-xl bg-white dark:bg-slate-950">
            <img v-if="report.image" :src="report.image" :alt="report.title" class="h-16 w-16 object-cover" />
            <UIcon v-else name="i-heroicons-photo" class="h-8 w-8 text-slate-400 dark:text-slate-600" />
          </div>

          <div class="min-w-0 flex-1">
            <h4 class="text-lg font-semibold leading-7 text-slate-900 dark:text-slate-50">{{ displayTitle }}</h4>
            <p v-if="originalTitle" class="mt-1 line-clamp-1 text-[12px] leading-5 text-slate-500 dark:text-slate-400">{{ originalTitle }}</p>
            <p class="mt-1 text-[13px] text-slate-500 dark:text-slate-500">{{ report.market }}</p>
            <div class="mt-2 flex flex-wrap items-center gap-2">
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

        <div class="mt-4 grid gap-2.5 sm:grid-cols-2 xl:grid-cols-4">
          <div
            v-for="metric in headlineMetrics"
            :key="metric.key"
            class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3.5 py-3 dark:bg-slate-950/70"
          >
            <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">{{ metric.label }}</p>
            <p class="mt-1 text-base font-semibold text-slate-900 dark:text-slate-100">{{ metric.value }}</p>
          </div>
        </div>
      </div>

      <div class="space-y-4">
        <h5 class="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <UIcon name="i-heroicons-arrow-trending-up" class="h-3.5 w-3.5" />
          {{ t('report.summary') }}
        </h5>
        <div class="grid grid-cols-2 gap-3">
          <div class="rounded-2xl border border-emerald-100 bg-emerald-50/70 p-5 dark:border-emerald-950/50 dark:bg-emerald-950/20">
            <p class="text-[10px] uppercase tracking-wide text-slate-500">{{ t('report.estimatedProfit') }}</p>
            <p class="mt-2 text-3xl font-bold text-slate-900 dark:text-slate-100">{{ formatCurrency(report.costBreakdown.estimatedProfit) }}</p>
          </div>
          <div class="rounded-2xl border border-blue-100 bg-blue-50/70 p-5 dark:border-blue-950/50 dark:bg-blue-950/20">
            <p class="text-[10px] uppercase tracking-wide text-slate-500">{{ t('report.expectedMargin') }}</p>
            <p class="mt-2 text-3xl font-bold text-emerald-600 dark:text-emerald-300">{{ formatPercent(report.expectedMargin, true) }}</p>
          </div>
        </div>
        <p class="rounded-2xl border border-slate-200/70 bg-white p-5 text-[15px] leading-8 text-slate-700 shadow-sm dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">
          {{ resolveSummary() }}
        </p>
      </div>

      <div class="rounded-3xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)] p-5">
        <div class="flex items-start gap-3">
          <UIcon name="i-heroicons-document-magnifying-glass" class="mt-0.5 h-5 w-5 text-slate-400 dark:text-slate-500" />
          <div>
            <p class="text-sm font-semibold text-slate-800 dark:text-slate-100">{{ t('report.detailMovedTitle') }}</p>
            <p class="mt-2 text-[14px] leading-7 text-slate-500 dark:text-slate-400">
              {{ t('report.detailMovedDescription') }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="isPreviewOpen && previewDocument"
        class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 p-4 backdrop-blur-sm"
        @click.self="closePreview"
      >
        <div class="tradeflow-panel flex h-[min(88dvh,920px)] w-[min(960px,100%)] flex-col overflow-hidden rounded-[var(--tf-radius-2xl)]">
          <div class="flex items-center justify-between gap-4 border-b border-[var(--tf-border)] px-5 py-4">
            <div class="min-w-0">
              <p class="tradeflow-section-title">{{ t('report.previewTitle') }}</p>
              <h3 class="mt-1 truncate text-lg font-semibold text-slate-900 dark:text-slate-100">
                {{ previewDisplayTitle }}
              </h3>
            </div>

            <button
              type="button"
              class="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-[var(--tf-border)] bg-white/80 text-slate-600 transition hover:border-[var(--tf-border-strong)] hover:bg-white dark:bg-slate-950/80 dark:text-slate-300"
              @click="closePreview"
            >
              <UIcon name="i-heroicons-x-mark" class="h-5 w-5" />
            </button>
          </div>

          <div class="tradeflow-scrollbar min-h-0 flex-1 overflow-y-auto px-5 py-5">
            <div v-if="report" class="mx-auto max-w-4xl space-y-5">
              <section class="rounded-3xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-5">
                <div class="flex flex-col gap-4 lg:flex-row lg:items-start">
                  <div class="flex h-20 w-20 items-center justify-center overflow-hidden rounded-2xl bg-white dark:bg-slate-950">
                    <img v-if="report.image" :src="report.image" :alt="report.title" class="h-20 w-20 object-cover" />
                    <UIcon v-else name="i-heroicons-photo" class="h-8 w-8 text-slate-400 dark:text-slate-600" />
                  </div>

                  <div class="min-w-0 flex-1">
                    <h1 class="text-2xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
                      {{ previewDisplayTitle }}
                    </h1>
                    <p class="mt-2 text-[15px] leading-7 text-slate-600 dark:text-slate-300">
                      {{ resolveSummary() }}
                    </p>
                  </div>

                  <button
                    type="button"
                    class="inline-flex items-center gap-2 self-start rounded-xl border border-[var(--tf-border)] bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:border-[var(--tf-border-strong)] hover:bg-slate-50 dark:bg-slate-950 dark:text-slate-200 dark:hover:bg-slate-900"
                    @click="openProductOnMarket"
                  >
                    <UIcon name="i-heroicons-arrow-top-right-on-square" class="h-4 w-4" />
                    {{ t('report.openMarketProduct') }}
                  </button>
                </div>

                <div class="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                  <div
                    v-for="item in previewKeyFacts"
                    :key="item.label"
                    class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-4 py-3 dark:bg-slate-950/70"
                  >
                    <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">{{ item.label }}</p>
                    <p class="mt-1 text-base font-semibold text-slate-900 dark:text-slate-100">{{ item.value }}</p>
                  </div>
                </div>
              </section>

              <section class="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
                <div class="rounded-3xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)] p-5">
                  <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">{{ t('report.costs') }}</h2>
                  <div class="mt-4 grid gap-3 sm:grid-cols-2">
                    <div
                      v-for="item in costItems"
                      :key="item.key"
                      class="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] px-4 py-3"
                    >
                      <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">{{ t(`report.${item.key}`) }}</p>
                      <p class="mt-1 text-base font-semibold text-slate-900 dark:text-slate-100">{{ formatCurrency(item.value) }}</p>
                    </div>
                  </div>
                </div>

                <div class="rounded-3xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)] p-5">
                  <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">{{ t('report.risk') }}</h2>
                  <div class="mt-4 rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-4">
                    <div class="flex items-center justify-between gap-3">
                      <span :class="['text-sm font-semibold', riskColors[report.riskLevel]]">{{ t(`risk.${report.riskLevel}`) }}</span>
                      <span class="text-2xl font-semibold text-slate-900 dark:text-slate-100">{{ report.riskAssessment.score ?? '--' }}</span>
                    </div>
                    <ul class="mt-3 space-y-2 text-[14px] leading-6 text-slate-600 dark:text-slate-300">
                      <li v-for="(factor, index) in report.riskAssessment.factors" :key="`preview-factor-${index}`">
                        {{ t(`riskFactors.${factor}`) }}
                      </li>
                      <li v-for="(note, index) in report.riskAssessment.notes ?? []" :key="`preview-note-${index}`">
                        {{ note }}
                      </li>
                    </ul>
                  </div>
                </div>
              </section>

              <section v-if="previewTopMatch" class="rounded-3xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)] p-5">
                <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">{{ t('report.topDomesticMatch') }}</h2>
                <div class="mt-4 flex flex-col gap-4 lg:flex-row">
                  <div v-if="previewTopMatch.image" class="flex h-24 w-24 items-center justify-center overflow-hidden rounded-2xl bg-white dark:bg-slate-950">
                    <img :src="previewTopMatch.image" :alt="previewTopMatch.title" class="h-24 w-24 object-cover" />
                  </div>
                  <div class="min-w-0 flex-1">
                    <p class="text-base font-semibold text-slate-900 dark:text-slate-100">{{ displayMatchTitle(previewTopMatch) }}</p>
                    <p v-if="previewTopMatch.reason" class="mt-2 text-[14px] leading-6 text-slate-600 dark:text-slate-300">{{ previewTopMatch.reason }}</p>
                    <div class="mt-3 flex flex-wrap gap-2">
                      <span class="rounded-full bg-[var(--tf-accent-soft)] px-3 py-1 text-xs font-semibold text-slate-700 dark:text-slate-200">
                        {{ t('report.matchPrice') }} {{ formatCurrency(previewTopMatch.price) }}
                      </span>
                      <span class="rounded-full bg-[var(--tf-success-soft)] px-3 py-1 text-xs font-semibold text-slate-700 dark:text-slate-200">
                        {{ t('report.matchSimilarity') }} {{ previewTopMatch.similarityScore }}%
                      </span>
                      <span
                        :class="[
                          'rounded-full px-3 py-1 text-xs font-semibold',
                          previewTopMatch.detailReady
                            ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                            : 'bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300',
                        ]"
                      >
                        {{ t('report.detailStatus') }} {{ detailReadyLabel(previewTopMatch) }}
                      </span>
                      <span class="rounded-full border border-[var(--tf-border)] px-3 py-1 text-xs font-semibold text-slate-500 dark:text-slate-400">
                        {{ t('report.matchSource') }} {{ previewTopMatch.matchSource ?? '--' }}
                      </span>
                      <span class="rounded-full border border-[var(--tf-border)] px-3 py-1 text-xs font-semibold text-slate-500 dark:text-slate-400">
                        {{ t('report.detailSource') }} {{ detailSourceLabel(previewTopMatch) }}
                      </span>
                      <a
                        :href="resolveMatchUrl(previewTopMatch)"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="inline-flex items-center gap-1 rounded-full border border-[var(--tf-border)] px-3 py-1 text-xs font-semibold text-slate-700 transition hover:border-[var(--tf-border-strong)] dark:text-slate-200"
                      >
                        {{ t('report.matchLinkAction') }}
                        <UIcon name="i-heroicons-arrow-top-right-on-square" class="h-3.5 w-3.5" />
                      </a>
                    </div>
                    <div v-if="formattedScoreEntries(previewTopMatch.scoreBreakdown).length" class="mt-4 grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
                      <div
                        v-for="item in formattedScoreEntries(previewTopMatch.scoreBreakdown)"
                        :key="item.key"
                        class="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] px-3 py-2.5"
                      >
                        <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">{{ item.label }}</p>
                        <p class="mt-1 text-sm font-semibold text-slate-900 dark:text-slate-100">{{ item.value }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </section>

              <section v-if="report.domesticMatches.length > 1" class="rounded-3xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)] p-5">
                <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">{{ t('report.domesticMatches') }}</h2>
                <div class="mt-4 space-y-3">
                  <div
                    v-for="match in report.domesticMatches"
                    :key="`preview-domestic-${match.id}`"
                    class="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-4"
                  >
                    <div class="flex flex-wrap items-center gap-2">
                      <span class="rounded-full bg-white px-2.5 py-1 text-[11px] font-semibold text-slate-700 dark:bg-slate-950 dark:text-slate-300">
                        {{ match.platform }}
                      </span>
                      <span class="rounded-full bg-[var(--tf-success-soft)] px-2.5 py-1 text-[11px] font-semibold text-slate-700 dark:text-slate-200">
                        {{ t('report.matchSimilarity') }} {{ match.similarityScore }}%
                      </span>
                      <span
                        :class="[
                          'rounded-full px-2.5 py-1 text-[11px] font-semibold',
                          match.detailReady
                            ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                            : 'bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300',
                        ]"
                      >
                        {{ detailReadyLabel(match) }}
                      </span>
                      <span class="rounded-full border border-[var(--tf-border)] px-2.5 py-1 text-[11px] font-semibold text-slate-500 dark:text-slate-400">
                        {{ match.matchSource ?? '--' }}
                      </span>
                      <span class="rounded-full border border-[var(--tf-border)] px-2.5 py-1 text-[11px] font-semibold text-slate-500 dark:text-slate-400">
                        {{ detailSourceLabel(match) }}
                      </span>
                    </div>
                    <p class="mt-3 text-base font-semibold text-slate-900 dark:text-slate-100">{{ displayMatchTitle(match) }}</p>
                    <p v-if="match.reason" class="mt-2 text-[14px] leading-6 text-slate-600 dark:text-slate-300">{{ match.reason }}</p>
                    <div class="mt-3 flex flex-wrap gap-2">
                      <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-700 dark:bg-slate-950 dark:text-slate-300">
                        {{ t('report.matchPrice') }} {{ formatCurrency(match.price) }}
                      </span>
                      <a
                        :href="resolveMatchUrl(match)"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="inline-flex items-center gap-1 rounded-full border border-[var(--tf-border)] px-3 py-1 text-xs font-semibold text-slate-700 transition hover:border-[var(--tf-border-strong)] dark:text-slate-200"
                      >
                        {{ t('report.matchLinkAction') }}
                        <UIcon name="i-heroicons-arrow-top-right-on-square" class="h-3.5 w-3.5" />
                      </a>
                    </div>
                    <div v-if="formattedScoreEntries(match.scoreBreakdown).length" class="mt-4 grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
                      <div
                        v-for="item in formattedScoreEntries(match.scoreBreakdown)"
                        :key="`${match.id}-${item.key}`"
                        class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-2.5 dark:bg-slate-950/70"
                      >
                        <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">{{ item.label }}</p>
                        <p class="mt-1 text-sm font-semibold text-slate-900 dark:text-slate-100">{{ item.value }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </section>

              <section v-if="report.recommendations?.length" class="rounded-3xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)] p-5">
                <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">{{ t('report.recommendations') }}</h2>
                <ul class="mt-4 space-y-2 text-[14px] leading-6 text-slate-600 dark:text-slate-300">
                  <li v-for="(item, index) in report.recommendations" :key="`preview-rec-${index}`" class="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] px-4 py-3">
                    {{ item }}
                  </li>
                </ul>
              </section>

              <section v-if="report.analysisTrace" class="rounded-3xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)] p-5">
                <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">{{ t('report.analysisTrace') }}</h2>
                <div class="mt-4 space-y-3">
                  <div v-if="report.analysisTrace.rewrite" class="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-4">
                    <p class="text-base font-semibold text-slate-900 dark:text-slate-100">{{ t('report.rewrite') }}</p>
                    <p class="mt-2 text-[14px] leading-6 text-slate-600 dark:text-slate-300">{{ report.analysisTrace.rewrite.rewrittenText }}</p>
                    <p class="mt-2 text-[14px] leading-6 text-slate-500 dark:text-slate-400">
                      {{ t('report.keywords') }}: {{ formatList(report.analysisTrace.rewrite.keywords) }}
                    </p>
                  </div>

                  <div v-if="report.analysisTrace.retrieval" class="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-4">
                    <p class="text-base font-semibold text-slate-900 dark:text-slate-100">{{ t('report.retrieval') }}</p>
                    <p class="mt-2 text-[14px] leading-6 text-slate-600 dark:text-slate-300">
                      {{ t('report.retrievalTerms') }}: {{ formatList(report.analysisTrace.retrieval.retrievalTerms) }}
                    </p>
                    <div v-if="formattedScoreEntries(report.analysisTrace.retrieval.scoreBreakdown).length" class="mt-3 grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
                      <div
                        v-for="item in formattedScoreEntries(report.analysisTrace.retrieval.scoreBreakdown)"
                        :key="`trace-preview-${item.key}`"
                        class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-2.5 dark:bg-slate-950/70"
                      >
                        <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">{{ item.label }}</p>
                        <p class="mt-1 text-sm font-semibold text-slate-900 dark:text-slate-100">{{ item.value }}</p>
                      </div>
                    </div>
                  </div>

                  <div v-if="report.analysisTrace.pricing" class="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-4">
                    <p class="text-base font-semibold text-slate-900 dark:text-slate-100">{{ t('report.pricingTrace') }}</p>
                    <ul class="mt-3 space-y-2 text-[14px] leading-6 text-slate-600 dark:text-slate-300">
                      <li v-for="(item, index) in report.analysisTrace.pricing.formulaLines" :key="`trace-pricing-${index}`">{{ item }}</li>
                    </ul>
                  </div>

                  <div v-if="report.analysisTrace.llm" class="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-4">
                    <p class="text-base font-semibold text-slate-900 dark:text-slate-100">{{ t('report.llm') }}</p>
                    <p class="mt-2 text-[14px] leading-6 text-slate-600 dark:text-slate-300">
                      {{ report.analysisTrace.llm.provider }} / {{ report.analysisTrace.llm.model }}
                    </p>
                    <p class="mt-1 text-[14px] leading-6 text-slate-500 dark:text-slate-400">
                      {{ formatDateTime(report.analysisTrace.llm.generatedAt) }}
                    </p>
                  </div>
                </div>
              </section>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
