import { defineStore } from 'pinia'
import type { CandidateSummary, ReportDetail } from '~/types'

export interface ProductFilters {
  keyword: string
  market: string
}

export const useProductsStore = defineStore('products', () => {
  const candidates = ref<CandidateSummary[]>([])
  const filters = ref<ProductFilters>({
    keyword: '',
    market: 'all',
  })
  const isLoading = ref(false)
  const isAnalyzingReport = ref(false)
  const selectedProductId = ref<string | null>(null)
  const reportsByProductId = ref<Record<string, ReportDetail>>({})

  const filteredCandidates = computed(() => {
    return candidates.value.filter((candidate) => {
      if (filters.value.keyword && !candidate.title.toLowerCase().includes(filters.value.keyword.toLowerCase())) {
        return false
      }
      if (filters.value.market !== 'all' && filters.value.market && candidate.market !== filters.value.market) {
        return false
      }
      return true
    })
  })

  const currentReport = computed(() => {
    if (!selectedProductId.value) return null
    return reportsByProductId.value[selectedProductId.value] ?? null
  })

  const currentCandidate = computed(() => {
    if (!selectedProductId.value) return null
    return candidates.value.find((item) => item.productId === selectedProductId.value) ?? null
  })

  const setCandidates = (items: CandidateSummary[]) => {
    candidates.value = items
  }

  const setReports = (reports: Record<string, ReportDetail>) => {
    reportsByProductId.value = reports
  }

  const setReport = (report: ReportDetail) => {
    reportsByProductId.value = {
      ...reportsByProductId.value,
      [report.productId]: report,
    }
  }

  const selectProduct = (productId: string | null) => {
    selectedProductId.value = productId
  }

  const setFilter = <K extends keyof ProductFilters>(key: K, value: ProductFilters[K]) => {
    filters.value[key] = value
  }

  const setLoading = (value: boolean) => {
    isLoading.value = value
  }

  const setReportAnalyzing = (value: boolean) => {
    isAnalyzingReport.value = value
  }

  const reset = () => {
    candidates.value = []
    reportsByProductId.value = {}
    selectedProductId.value = null
    isLoading.value = false
    isAnalyzingReport.value = false
    filters.value = {
      keyword: '',
      market: 'all',
    }
  }

  return {
    candidates,
    filters,
    isLoading,
    isAnalyzingReport,
    selectedProductId,
    currentReport,
    currentCandidate,
    filteredCandidates,
    setCandidates,
    setReports,
    setReport,
    selectProduct,
    setFilter,
    setLoading,
    setReportAnalyzing,
    reset,
  }
})
