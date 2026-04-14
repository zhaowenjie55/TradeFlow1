"use client"

import { useMemo } from "react"
import { AlertTriangle, CheckCircle2, Clock3, ShoppingBag } from "lucide-react"

import { useAppI18n } from "@/components/layout/locale-provider"
import { ProductCard } from "@/components/product/product-card"
import { ImageWithFallback } from "@/components/ui/image-with-fallback"
import { Card, CardContent } from "@/components/ui/card"
import { getReadableOriginalTitle, getReadableProductName, getReadableStageLabel, humanizeQualityTier, humanizeReportSource } from "@/lib/presentation"
import { useTaskRunner } from "@/hooks/use-task-runner"
import { getFilteredCandidates, useProductsStore } from "@/stores/products-store"
import { useTaskStore } from "@/stores/task-store"

export function ProductGrid() {
  const { t } = useAppI18n()
  const productState = useProductsStore()
  const candidates = useMemo(() => getFilteredCandidates(productState), [productState])
  const selectedProductId = useProductsStore((state) => state.selectedProductId)
  const isLoading = useProductsStore((state) => state.isLoading)
  const isAnalyzingReport = useProductsStore((state) => state.isAnalyzingReport)
  const status = useTaskStore((state) => state.status)
  const stage = useTaskStore((state) => state.stage)
  const isPolling = useTaskStore((state) => state.isPolling)
  const { analyzeProduct } = useTaskRunner()

  const currentCandidate = useMemo(() => {
    if (!selectedProductId) return null
    return productState.candidates.find((item) => item.productId === selectedProductId) ?? null
  }, [productState.candidates, selectedProductId])
  const currentReport = useMemo(() => {
    if (!selectedProductId) return null
    return productState.reportsByProductId[selectedProductId] ?? null
  }, [productState.reportsByProductId, selectedProductId])

  const workflowSteps = useMemo(() => {
    const hasCandidates = candidates.length > 0
    const hasSelection = Boolean(selectedProductId)
    return [
      { key: "discover", title: t("productGrid.step1"), state: hasCandidates ? "complete" : isPolling ? "active" : "pending" },
      { key: "select", title: t("productGrid.step2"), state: hasSelection ? "complete" : hasCandidates ? "active" : "pending" },
      { key: "report", title: t("productGrid.step3"), state: isAnalyzingReport ? "active" : status === "REPORT_READY" ? "complete" : "pending" },
    ] as const
  }, [candidates.length, isAnalyzingReport, isPolling, selectedProductId, status, t])

  const currentReportBadges = useMemo(() => {
    const provenance = currentReport?.provenance
    if (!provenance) return []
    return [
      provenance.qualityTier
        ? { key: "quality", value: humanizeQualityTier(provenance.qualityTier, t) }
        : null,
      provenance.retrievalSource
        ? { key: "retrieval", value: humanizeReportSource(provenance.retrievalSource, t) }
        : null,
      provenance.detailSource
        ? { key: "detail", value: humanizeReportSource(provenance.detailSource, t) }
        : null,
      { key: "fallback", value: provenance.fallbackUsed ? t("common.yes") : t("common.no") },
    ].filter((item): item is { key: string; value: string } => Boolean(item))
  }, [currentReport?.provenance, t])

  const processBanner = useMemo(() => {
    if (status === "WAITING_1688_VERIFICATION") {
      return {
        tone: "warning" as const,
        title: t("productGrid.bannerVerificationTitle"),
        description: t("productGrid.bannerVerificationDescription"),
      }
    }

    if (status === "REPORT_READY" && currentCandidate) {
      return {
        tone: "success" as const,
        title: t("productGrid.bannerReportReadyTitle"),
        description: t("productGrid.bannerReportReadyDescription", {
          title: getReadableProductName(currentCandidate.title),
        }),
      }
    }

    if (isAnalyzingReport && currentCandidate) {
      return {
        tone: "info" as const,
        title: t("productGrid.bannerAnalyzingTitle"),
        description: t("productGrid.bannerAnalyzingDescription", {
          title: getReadableProductName(currentCandidate.title),
        }),
      }
    }

    if (status === "WAITING_USER_SELECTION" && candidates.length > 0) {
      return {
        tone: "info" as const,
        title: t("productGrid.bannerSelectionTitle"),
        description: t("productGrid.bannerSelectionDescription"),
      }
    }

    if ((isLoading || isPolling) && candidates.length === 0) {
      return {
        tone: "info" as const,
        title: t("productGrid.bannerDiscoveryTitle"),
        description: t("productGrid.bannerDiscoveryDescription"),
      }
    }

    return null
  }, [candidates.length, currentCandidate, isAnalyzingReport, isLoading, isPolling, status, t])

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="border-b border-[var(--tf-border)] px-4 py-3">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <ShoppingBag className="size-5 text-[var(--tf-text-subtle)]" />
              <h2 className="text-lg font-semibold text-[var(--tf-text)]">{t("productGrid.title")}</h2>
              <span className="shrink-0 text-sm text-[var(--tf-text-subtle)]">{t("productGrid.results", { count: candidates.length })}</span>
            </div>
            <p className="mt-1 text-xs leading-5 text-[var(--tf-text-muted)]">{t("productGrid.subtitle")}</p>
          </div>
          <span className="shrink-0 rounded-full border border-[var(--tf-border)] px-3 py-1 text-xs text-[var(--tf-text-muted)]">{getReadableStageLabel(stage)}</span>
        </div>

          <div className="mt-3 grid gap-2 md:grid-cols-3">
            {workflowSteps.map((step, index) => (
              <Card
                key={step.key}
                className={
                  step.state === "complete"
                  ? "tf-step-complete border-emerald-300 bg-emerald-50 shadow-[inset_0_0_0_1px_rgba(16,185,129,0.15)] transition-colors duration-300"
                  : step.state === "active"
                    ? "tf-step-active border-blue-300 bg-blue-50 shadow-[inset_0_0_0_1px_rgba(59,130,246,0.18)] transition-all duration-300"
                    : "border-[var(--tf-border)] bg-white transition-colors duration-300"
                }
              >
              <CardContent className="p-3">
                <div className="flex items-center gap-3">
                  <span
                    className={
                      step.state === "complete"
                        ? "flex size-7 items-center justify-center rounded-full bg-emerald-500 text-[11px] font-semibold text-white"
                        : step.state === "active"
                          ? "flex size-7 items-center justify-center rounded-full bg-blue-500 text-[11px] font-semibold text-white"
                          : "flex size-7 items-center justify-center rounded-full bg-[var(--tf-bg-soft)] text-[11px] font-semibold text-[var(--tf-text)]"
                    }
                  >
                    {index + 1}
                  </span>
                  <div>
                    <p
                      className={
                        step.state === "complete"
                          ? "text-sm font-semibold text-emerald-800"
                          : step.state === "active"
                            ? "text-sm font-semibold text-blue-800"
                            : "text-sm font-semibold text-[var(--tf-text)]"
                      }
                    >
                      {step.title}
                    </p>
                    <p
                      className={
                        step.state === "complete"
                          ? "mt-0.5 text-[11px] text-emerald-700"
                          : step.state === "active"
                            ? "mt-0.5 text-[11px] text-blue-700"
                            : "mt-0.5 text-[11px] text-[var(--tf-text-subtle)]"
                      }
                    >
                      {step.state === "complete"
                        ? t("productGrid.stepStateComplete")
                        : step.state === "active"
                          ? t("productGrid.stepStateActive")
                          : t("productGrid.stepStatePending")}
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {processBanner && (
          <div className={`mt-3 rounded-2xl border px-3 py-2.5 ${getBannerToneClasses(processBanner.tone)}`}>
            <div className="flex items-start gap-2">
              <ProcessBannerIcon tone={processBanner.tone} />
              <div className="min-w-0">
                <p className="text-xs font-semibold leading-5">{processBanner.title}</p>
                <p className="mt-1 text-xs leading-5">{processBanner.description}</p>
              </div>
            </div>
          </div>
        )}

        {currentCandidate && (
          <Card className="tf-summary-active mt-3 border-blue-200 bg-[linear-gradient(180deg,rgba(239,246,255,0.9),rgba(255,255,255,0.95))]">
            <CardContent className="p-3">
              <div className="flex items-start gap-3">
                <ImageWithFallback
                  src={currentCandidate.imageUrl}
                  alt={currentCandidate.title}
                  wrapperClassName="size-14 rounded-2xl shrink-0"
                  className="size-14 rounded-2xl"
                />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-xs uppercase tracking-[0.18em] text-[var(--tf-text-subtle)]">{t("productGrid.currentProduct")}</p>
                    <span className="shrink-0 rounded-full border border-blue-200 bg-white px-2 py-0.5 text-[10px] font-medium text-blue-700">
                      {isAnalyzingReport ? t("productGrid.stepStateActive") : t("products.selected")}
                    </span>
                  </div>
                  <h3 className="mt-1 text-base font-semibold leading-tight text-[var(--tf-text)]">{getReadableProductName(currentCandidate.title)}</h3>
                  <p className="mt-1 text-xs leading-5 text-[var(--tf-text-muted)]">
                    {getReadableOriginalTitle(currentCandidate.title, getReadableProductName(currentCandidate.title))}
                  </p>
                  {currentReportBadges.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {currentReportBadges.map((badge) => (
                        <span
                          key={badge.key}
                          className="rounded-full border border-blue-200 bg-white px-2 py-1 text-[10px] font-medium leading-none text-blue-700"
                        >
                          {badge.value}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      <div className="tradeflow-scrollbar min-h-0 flex-1 overflow-auto px-4 py-4">
        {isLoading && candidates.length === 0 ? (
          <div className="grid gap-3 lg:grid-cols-3">
            {Array.from({ length: 9 }).map((_, i) => (
              <div key={i} className="overflow-hidden rounded-[22px] border border-[var(--tf-border)] bg-[var(--tf-bg-soft)]">
                <div className="tf-skeleton aspect-[4/3]" />
                <div className="space-y-2 p-3">
                  <div className="tf-skeleton h-3 w-3/4 rounded" />
                  <div className="tf-skeleton h-3 w-1/2 rounded" />
                  <div className="tf-skeleton h-8 w-full rounded-xl" />
                </div>
              </div>
            ))}
          </div>
        ) : candidates.length === 0 ? (
          <div className="flex h-full min-h-[24rem] items-center justify-center text-center text-sm text-[var(--tf-text-subtle)]">
            {t("productGrid.empty")}
          </div>
        ) : (
          <div className="grid gap-3 lg:grid-cols-3">
            {candidates.map((candidate) => (
              <ProductCard
                key={candidate.productId}
                candidate={candidate}
                selected={selectedProductId === candidate.productId}
                analyzing={isAnalyzingReport}
                disabled={isAnalyzingReport}
                onAnalyze={analyzeProduct}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function ProcessBannerIcon({ tone }: { tone: "info" | "warning" | "success" }) {
  switch (tone) {
    case "warning":
      return <AlertTriangle className="mt-0.5 size-4 shrink-0" />
    case "success":
      return <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
    default:
      return <Clock3 className="mt-0.5 size-4 shrink-0" />
  }
}

function getBannerToneClasses(tone: "info" | "warning" | "success") {
  switch (tone) {
    case "warning":
      return "border-amber-200 bg-amber-50 text-amber-900"
    case "success":
      return "border-emerald-200 bg-emerald-50 text-emerald-900"
    default:
      return "border-sky-200 bg-sky-50 text-sky-900"
  }
}
