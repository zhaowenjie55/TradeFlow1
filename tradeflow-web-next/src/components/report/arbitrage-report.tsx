"use client"

import { useEffect, useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { AlertTriangle, CheckCircle2, Download, Eye, ShieldAlert, ShieldCheck } from "lucide-react"

import { useAppI18n } from "@/components/layout/locale-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { ImageWithFallback } from "@/components/ui/image-with-fallback"
import { getReportByTaskId } from "@/lib/api/report"
import {
  compactNarrativeSummary,
  getReportTrustIndicator,
  getReadableLlmProvider,
  getReadableMatchTitle,
  getReadableOriginalTitle,
  getReadableProductName,
  humanizeQualityTier,
  humanizeReportSource,
} from "@/lib/presentation"
import { useProductsStore } from "@/stores/products-store"
import { useTaskStore } from "@/stores/task-store"
import type { ReportDetail } from "@/types"

function formatCurrency(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) return "--"
  return `¥${Number(value).toFixed(2)}`
}

function formatPercent(value?: number | null, withPlus = false) {
  if (value === null || value === undefined || Number.isNaN(value)) return "--"
  const prefix = withPlus && value > 0 ? "+" : ""
  return `${prefix}${Number(value).toFixed(1)}%`
}

export function ArbitrageReport({
  report: externalReport,
  candidateTitle,
  isLoading: externalLoading,
}: {
  report?: ReportDetail | null
  candidateTitle?: string | null
  isLoading?: boolean
}) {
  const { t } = useAppI18n()
  const router = useRouter()
  const selectedProductId = useProductsStore((state) => state.selectedProductId)
  const reportsByProductId = useProductsStore((state) => state.reportsByProductId)
  const setReport = useProductsStore((state) => state.setReport)
  const currentTaskId = useTaskStore((state) => state.currentTaskId)
  const currentTaskPhase = useTaskStore((state) => state.currentTaskPhase)
  const status = useTaskStore((state) => state.status)

  const [hydratedReport, setHydratedReport] = useState<ReportDetail | null>(null)
  const [isHydrating, setIsHydrating] = useState(false)

  const report = externalReport ?? (selectedProductId ? reportsByProductId[selectedProductId] ?? hydratedReport : hydratedReport)
  const isLoading = externalLoading ?? isHydrating
  const isPdfDownloadReady = Boolean(
    report?.downloadDocument?.content && report.downloadDocument.mimeType?.toLowerCase().includes("pdf"),
  )

  useEffect(() => {
    let alive = true
    if (externalReport || currentTaskPhase !== "PHASE2" || status !== "REPORT_READY" || !currentTaskId || report) return
    const hydrateReport = async () => {
      setIsHydrating(true)
      try {
        const next = await getReportByTaskId(currentTaskId)
        if (!alive) return
        setHydratedReport(next)
        setReport(next)
      } finally {
        if (alive) setIsHydrating(false)
      }
    }
    void hydrateReport()
    return () => {
      alive = false
    }
  }, [currentTaskId, currentTaskPhase, externalReport, report, setReport, status])

  const summary = useMemo(() => {
    if (!report) return ""
    return compactNarrativeSummary(report.summary.insightKey, {
      productTitle: report.title,
      domesticTitle: report.domesticMatches?.[0]?.title,
    })
  }, [report])

  const openReportDetail = () => {
    if (!report?.reportId) return
    router.push(`/reports/${report.reportId}`)
  }

  const downloadChineseReport = () => {
    if (!report?.downloadDocument?.content || !report.downloadDocument.mimeType?.toLowerCase().includes("pdf")) return
    const byteCharacters = atob(report.downloadDocument.content)
    const byteNumbers = Array.from(byteCharacters, (char) => char.charCodeAt(0))
    const blob = new Blob([new Uint8Array(byteNumbers)], {
      type: report.downloadDocument.mimeType || "application/octet-stream",
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement("a")
    link.href = url
    link.download = report.downloadDocument.fileName || "tradeflow-report"
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  }

  if (!report) {
    return (
      <div className="flex h-full min-h-[18rem] items-center justify-center px-6 text-center text-sm text-[var(--tf-text-subtle)]">
        {isLoading ? `${t("report.loadingTitle")}...` : t("report.emptyDescription")}
      </div>
    )
  }

  const topMatch = report.domesticMatches[0]
  const provenance = report.provenance
  const reportTitle = getReadableProductName(report.title, { domesticHint: topMatch?.title })
  const originalTitle = getReadableOriginalTitle(report.title, reportTitle)
  const displayTitle = candidateTitle ? getReadableProductName(candidateTitle, { domesticHint: topMatch?.title }) : reportTitle
  const decisionLabel = t(`decision.${report.decision}`)
  const riskLabel = t(`risk.${report.riskLevel}`)
  const llmProvider = provenance?.llmProvider
    ? getReadableLlmProvider(provenance.llmProvider)
    : report.analysisTrace?.llm?.provider
      ? getReadableLlmProvider(report.analysisTrace.llm.provider)
      : "--"
  const rewriteProvider = provenance?.rewriteProvider ? getReadableLlmProvider(provenance.rewriteProvider) : "--"
  const retrievalSource = provenance?.retrievalSource ?? topMatch?.matchSource ?? report.analysisTrace?.retrieval?.matchSource
  const detailSource = provenance?.detailSource ?? topMatch?.detailSource
  const qualityTier = provenance?.qualityTier
  const fallbackUsed = provenance?.fallbackUsed ?? false
  const fallbackReason = provenance?.fallbackReason
  const trustIndicator = getReportTrustIndicator(qualityTier, fallbackUsed, t)

  return (
    <div className="tradeflow-scrollbar h-full overflow-auto px-3 py-3">
      <div className="space-y-3">
        <Card>
          <CardContent className="space-y-3 p-3.5">
            <div className="flex flex-col gap-3">
              <div className="flex min-w-0 gap-3">
                <ImageWithFallback
                  src={report.image}
                  alt={displayTitle}
                  wrapperClassName="size-16 rounded-2xl shrink-0"
                  className="size-16 rounded-2xl"
                />
                <div className="min-w-0 space-y-2">
                  <p className="text-[11px] uppercase tracking-[0.18em] text-[var(--tf-text-subtle)]">{t("report.previewTitle")}</p>
                  <h2 className="line-clamp-2 break-words text-[clamp(1rem,1.25vw,1.45rem)] font-semibold leading-[1.2] text-[var(--tf-text)]">{displayTitle}</h2>
                  {originalTitle && <p className="line-clamp-2 text-[11px] leading-5 text-[var(--tf-text-muted)]">{originalTitle}</p>}
                  <p className="line-clamp-3 max-w-2xl text-xs leading-5 text-[var(--tf-text-muted)]">{summary}</p>
                </div>
              </div>
              <div className="flex flex-nowrap items-center gap-2 overflow-x-auto">
                <Button variant="outline" size="sm" className="h-9 min-w-[104px] rounded-xl px-3 text-[11px]" onClick={openReportDetail} disabled={!report.reportId}>
                  <Eye className="size-4" />
                  {t("report.preview")}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="h-9 min-w-[118px] rounded-xl px-3 text-[11px]"
                  onClick={downloadChineseReport}
                  disabled={!isPdfDownloadReady}
                >
                  <Download className="size-4" />
                  {t("report.download")}
                </Button>
              </div>
            </div>

            <div className="flex flex-wrap gap-2">
              <SourceBadge label={t("report.provider")} value={llmProvider} />
              {rewriteProvider !== "--" && <SourceBadge label={t("report.rewrite")} value={rewriteProvider} />}
              {retrievalSource && <SourceBadge label={t("report.matchSource")} value={humanizeReportSource(retrievalSource, t)} />}
              {detailSource && <SourceBadge label={t("report.detailSource")} value={humanizeReportSource(detailSource, t)} />}
              {topMatch && <SourceBadge label={t("report.detailStatus")} value={topMatch.detailReady ? t("report.detailReady") : t("report.detailPending")} />}
              {qualityTier && <SourceBadge label={t("report.qualityTier")} value={humanizeQualityTier(qualityTier, t)} />}
              {provenance?.pricingConfigVersion && <SourceBadge label={t("report.pricingConfigVersion")} value={provenance.pricingConfigVersion} />}
              <SourceBadge label={t("report.fallback")} value={fallbackUsed ? t("common.yes") : t("common.no")} />
            </div>

            <div className={`rounded-2xl border px-3 py-2.5 ${getTrustToneClasses(trustIndicator.tone)}`}>
              <div className="flex items-start gap-2">
                <TrustIcon tone={trustIndicator.tone} />
                <div className="min-w-0">
                  <p className="text-xs font-semibold leading-5">{trustIndicator.title}</p>
                  <p className="mt-1 text-xs leading-5">{trustIndicator.description}</p>
                  {fallbackReason && (
                    <p className="mt-1.5 text-[11px] leading-5 opacity-90">
                      {t("report.fallbackReason")}: {fallbackReason}
                    </p>
                  )}
                </div>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-1.5">
              <MetricCard label={t("report.expectedMargin")} value={formatPercent(report.expectedMargin, true)} />
              <MetricCard label={t("report.estimatedProfit")} value={formatCurrency(report.costBreakdown.estimatedProfit)} />
              <MetricCard label={t("report.decision")} value={decisionLabel} />
              <MetricCard label={t("report.riskLevel")} value={riskLabel} />
            </div>

            {report.costBreakdown && (
              <div className="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-3">
                <p className="mb-2 text-[11px] font-semibold uppercase tracking-widest text-[var(--tf-text-subtle)]">{t("report.costs")}</p>
                <div className="space-y-1">
                  {[
                    { key: t("report.sourcingCost"), val: report.costBreakdown.sourcingCost },
                    { key: t("report.domesticShippingCost"), val: report.costBreakdown.domesticShippingCost },
                    { key: t("report.logisticsCost"), val: report.costBreakdown.logisticsCost },
                    { key: t("report.platformFee"), val: report.costBreakdown.platformFee },
                    { key: t("report.exchangeRateCost"), val: report.costBreakdown.exchangeRateCost },
                    { key: t("report.totalCost"), val: report.costBreakdown.totalCost },
                    { key: t("report.targetSellingPrice"), val: report.costBreakdown.targetSellingPrice },
                  ]
                    .filter((row) => row.val !== null && row.val !== undefined)
                    .map((row) => (
                      <div key={row.key} className="flex items-center justify-between text-xs">
                        <span className="text-[var(--tf-text-muted)]">{row.key}</span>
                        <span className="font-medium text-[var(--tf-text)]">{formatCurrency(row.val)}</span>
                      </div>
                    ))}
                </div>
              </div>
            )}

            {report.riskAssessment && (report.riskAssessment.score !== null || report.riskAssessment.factors.length > 0) && (
              <div className="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-3">
                <div className="mb-2 flex items-center justify-between">
                  <p className="text-[11px] font-semibold uppercase tracking-widest text-[var(--tf-text-subtle)]">{t("report.risk")}</p>
                  {report.riskAssessment.score !== null && (
                    <span className="text-xs font-semibold text-[var(--tf-text)]">{t("report.riskScore")}: {report.riskAssessment.score}</span>
                  )}
                </div>
                {report.riskAssessment.factors.length > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {report.riskAssessment.factors.map((factor) => (
                      <span key={factor} className="rounded-full border border-[var(--tf-border)] px-2 py-0.5 text-[11px] text-[var(--tf-text-muted)]">
                        {factor}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        {topMatch && (
          <Card>
            <CardHeader>
              <CardTitle>{t("report.topDomesticMatch")}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex gap-4">
                <MatchImage src={topMatch.image} alt={topMatch.title} className="size-16 rounded-2xl" />
                <div className="min-w-0 flex-1 space-y-2">
                  <h3 className="line-clamp-2 break-words text-[15px] font-semibold leading-[1.25] text-[var(--tf-text)]">{getReadableMatchTitle(topMatch)}</h3>
                  <p className="text-xs leading-5 text-[var(--tf-text-muted)]">
                    {topMatch.reason ?? (t("report.sourceRealtimeHybrid"))}
                  </p>
                  <div className="flex flex-wrap gap-2">
                    <SourceBadge label={t("report.matchSource")} value={humanizeReportSource(topMatch.matchSource, t)} />
                    <SourceBadge label={t("report.detailSource")} value={humanizeReportSource(topMatch.detailSource, t)} />
                    <SourceBadge label={t("report.detailStatus")} value={topMatch.detailReady ? t("report.detailReady") : t("report.detailPending")} />
                    <SourceBadge label={t("report.matchPrice")} value={formatCurrency(topMatch.price)} />
                    <SourceBadge label={t("report.matchSimilarity")} value={`${topMatch.similarityScore}%`} />
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        )}

        {report.analysisTrace && (
          <Card>
            <CardHeader>
              <CardTitle>{t("report.analysisTrace")}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {report.analysisTrace.pricing && (
                <div className="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-3">
                  <div className="mb-2 flex items-center justify-between gap-3">
                    <p className="text-[11px] font-semibold uppercase tracking-widest text-[var(--tf-text-subtle)]">
                      {t("report.pricingTrace")}
                    </p>
                    <span className="text-[11px] text-[var(--tf-text-muted)]">
                      {t("report.currency")}: {report.analysisTrace.pricing.currency} · {t("report.usdToCnyRate")}:{" "}
                      {report.analysisTrace.pricing.usdToCnyRate}
                    </span>
                  </div>
                  {report.analysisTrace.pricing.formulaLines.length > 0 && (
                    <div className="space-y-1.5">
                      <p className="text-[11px] font-medium text-[var(--tf-text-subtle)]">{t("report.formulaLines")}</p>
                      <ul className="space-y-1.5 text-xs leading-5 text-[var(--tf-text-muted)]">
                        {report.analysisTrace.pricing.formulaLines.map((line, index) => (
                          <li key={`${line}-${index}`} className="rounded-xl border border-[var(--tf-border)] bg-white px-2.5 py-2">
                            {line}
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                  {report.analysisTrace.pricing.assumptions.length > 0 && (
                    <div className="mt-3 space-y-1.5">
                      <p className="text-[11px] font-medium text-[var(--tf-text-subtle)]">{t("report.assumptions")}</p>
                      <div className="flex flex-wrap gap-1.5">
                        {report.analysisTrace.pricing.assumptions.map((assumption) => (
                          <span
                            key={assumption}
                            className="rounded-full border border-[var(--tf-border)] bg-white px-2.5 py-1 text-[11px] text-[var(--tf-text-muted)]"
                          >
                            {assumption}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {(report.analysisTrace.retrieval || report.analysisTrace.llm) && (
                <div className="grid gap-3 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)]">
                  {report.analysisTrace.retrieval && (
                    <div className="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-3">
                      <p className="mb-2 text-[11px] font-semibold uppercase tracking-widest text-[var(--tf-text-subtle)]">
                        {t("report.retrieval")}
                      </p>
                      <div className="flex flex-wrap gap-1.5">
                        <SourceBadge
                          label={t("report.matchSource")}
                          value={humanizeReportSource(report.analysisTrace.retrieval.matchSource, t)}
                        />
                        {report.analysisTrace.retrieval.retrievalTerms.slice(0, 3).map((term) => (
                          <SourceBadge key={term} label={t("report.retrievalTerms")} value={term} />
                        ))}
                      </div>
                      {report.analysisTrace.retrieval.evidence.length > 0 && (
                        <ul className="mt-3 space-y-1.5 text-xs leading-5 text-[var(--tf-text-muted)]">
                          {report.analysisTrace.retrieval.evidence.slice(0, 3).map((line, index) => (
                            <li key={`${line}-${index}`} className="rounded-xl border border-[var(--tf-border)] bg-white px-2.5 py-2">
                              {line}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}

                  {report.analysisTrace.llm && (
                    <div className="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-3">
                      <p className="mb-2 text-[11px] font-semibold uppercase tracking-widest text-[var(--tf-text-subtle)]">
                        {t("report.llm")}
                      </p>
                      <div className="grid gap-1.5">
                        <MetricCard label={t("report.provider")} value={llmProvider} />
                        <MetricCard label={t("report.model")} value={provenance?.llmModel || report.analysisTrace.llm.model || "--"} />
                        <MetricCard
                          label={t("report.generatedAt")}
                          value={new Date(report.analysisTrace.llm.generatedAt).toLocaleString()}
                        />
                      </div>
                    </div>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        )}

        <Card>
          <CardHeader>
            <CardTitle>{t("report.domesticMatches")}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {report.domesticMatches.map((match) => (
              <div key={match.id} className="rounded-2xl border border-[var(--tf-border)] p-3">
                <div className="flex gap-3">
                  <MatchImage src={match.image} alt={match.title} className="size-10 rounded-xl" />
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap gap-2">
                      <SourceBadge label={t("report.matchLink")} value={match.platform} />
                      <SourceBadge label={t("report.matchSource")} value={humanizeReportSource(match.matchSource, t)} />
                      <SourceBadge label={t("report.detailSource")} value={humanizeReportSource(match.detailSource, t)} />
                    </div>
                    <h4 className="mt-2 line-clamp-2 break-words text-sm font-semibold leading-[1.25] text-[var(--tf-text)]">{getReadableMatchTitle(match)}</h4>
                    <p className="mt-1.5 text-xs leading-5 text-[var(--tf-text-muted)]">{match.reason ?? t("report.matchNote")}</p>
                  </div>
                </div>
                <div className="mt-3 grid grid-cols-2 gap-1.5">
                  <MetricCard label={t("report.matchPrice")} value={formatCurrency(match.price)} />
                  <MetricCard label={t("report.matchSimilarity")} value={`${match.similarityScore}%`} />
                  <MetricCard label={t("report.detailStatus")} value={match.detailReady ? t("report.detailReady") : t("report.detailPending")} />
                  <MetricCard label={t("report.evidence")} value={`${match.evidence.length}`} />
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-[var(--tf-border)] bg-white/80 px-2.5 py-2">
      <div className="flex items-center justify-between gap-2">
        <p className="min-w-0 truncate text-[10px] leading-4 text-[var(--tf-text-subtle)]">{label}</p>
        <p className="shrink-0 text-[13px] font-semibold leading-none text-[var(--tf-text)]">{value}</p>
      </div>
    </div>
  )
}

function MatchImage({ src, alt, className }: { src: string | null; alt: string; className?: string }) {
  return <ImageWithFallback src={src} alt={alt} wrapperClassName={`shrink-0 ${className ?? ""}`} className={className} iconClassName="size-5" />
}

function SourceBadge({ label, value }: { label: string; value: string }) {
  return (
    <span className="inline-flex max-w-full min-w-0 items-center rounded-full border border-[var(--tf-border)] bg-white px-2.5 py-1 text-[10px] leading-4 text-[var(--tf-text-muted)]">
      <span className="truncate">
        {label} · {value}
      </span>
    </span>
  )
}

function TrustIcon({ tone }: { tone: "success" | "info" | "warning" | "danger" }) {
  switch (tone) {
    case "success":
      return <ShieldCheck className="mt-0.5 size-4 shrink-0" />
    case "warning":
      return <AlertTriangle className="mt-0.5 size-4 shrink-0" />
    case "danger":
      return <ShieldAlert className="mt-0.5 size-4 shrink-0" />
    default:
      return <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
  }
}

function getTrustToneClasses(tone: "success" | "info" | "warning" | "danger") {
  switch (tone) {
    case "success":
      return "border-emerald-200 bg-emerald-50 text-emerald-900"
    case "warning":
      return "border-amber-200 bg-amber-50 text-amber-900"
    case "danger":
      return "border-red-200 bg-red-50 text-red-900"
    default:
      return "border-sky-200 bg-sky-50 text-sky-900"
  }
}
