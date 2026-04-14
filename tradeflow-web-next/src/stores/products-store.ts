"use client"

import type { CandidateSummary, ReportDetail } from "@/types"
import { create } from "zustand"

interface ProductFilters {
  keyword: string
  market: string
}

interface ProductsState {
  candidates: CandidateSummary[]
  filters: ProductFilters
  isLoading: boolean
  isAnalyzingReport: boolean
  selectedProductId: string | null
  reportsByProductId: Record<string, ReportDetail>
  setCandidates: (items: CandidateSummary[]) => void
  setReport: (report: ReportDetail) => void
  setReports: (reports: Record<string, ReportDetail>) => void
  selectProduct: (productId: string | null) => void
  setFilter: <K extends keyof ProductFilters>(key: K, value: ProductFilters[K]) => void
  setLoading: (value: boolean) => void
  setReportAnalyzing: (value: boolean) => void
  reset: () => void
}

const defaultFilters: ProductFilters = {
  keyword: "",
  market: "all",
}

export const useProductsStore = create<ProductsState>((set) => ({
  candidates: [],
  filters: { ...defaultFilters },
  isLoading: false,
  isAnalyzingReport: false,
  selectedProductId: null,
  reportsByProductId: {},
  setCandidates: (items) => set({ candidates: items }),
  setReport: (report) =>
    set((state) => ({
      reportsByProductId: {
        ...state.reportsByProductId,
        [report.productId]: report,
      },
    })),
  setReports: (reports) => set({ reportsByProductId: reports }),
  selectProduct: (productId) => set({ selectedProductId: productId }),
  setFilter: (key, value) =>
    set((state) => ({
      filters: {
        ...state.filters,
        [key]: value,
      },
    })),
  setLoading: (value) => set({ isLoading: value }),
  setReportAnalyzing: (value) => set({ isAnalyzingReport: value }),
  reset: () =>
    set({
      candidates: [],
      reportsByProductId: {},
      selectedProductId: null,
      isLoading: false,
      isAnalyzingReport: false,
      filters: { ...defaultFilters },
    }),
}))

export function getFilteredCandidates(state: ProductsState) {
  return state.candidates.filter((candidate) => {
    if (
      state.filters.keyword &&
      !candidate.title.toLowerCase().includes(state.filters.keyword.toLowerCase())
    ) {
      return false
    }
    if (state.filters.market !== "all" && state.filters.market && candidate.market !== state.filters.market) {
      return false
    }
    return true
  })
}
