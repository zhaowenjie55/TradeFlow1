"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { ArrowRight } from "lucide-react"

import { AppShell } from "@/components/layout/app-shell"
import { useAppI18n } from "@/components/layout/locale-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { getReportList } from "@/lib/api/report"
import { getReadableOriginalTitle, getReadableProductName, humanizeQualityTier, humanizeReportSource } from "@/lib/presentation"
import type { ReportListItem } from "@/types"

function formatPercent(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(value)) return "--"
  return `${Number(value).toFixed(1)}%`
}

export default function ReportsPage() {
  const { t } = useAppI18n()
  const [reports, setReports] = useState<ReportListItem[]>([])

  useEffect(() => {
    void getReportList()
      .then((response) => setReports(response.items))
      .catch(() => setReports([]))
  }, [])

  return (
    <AppShell activePanel="reports">
      <div className="mx-auto max-w-6xl">
        <h1 className="text-2xl font-semibold text-[var(--tf-text)]">{t("reportsPage.title")}</h1>
        <p className="mt-2 text-sm text-[var(--tf-text-muted)]">{t("reportsPage.subtitle")}</p>

        {reports.length === 0 ? (
          <Card className="mt-8">
            <CardContent className="p-6 text-sm text-[var(--tf-text-muted)]">{t("reportsPage.empty")}</CardContent>
          </Card>
        ) : (
          <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {reports.map((report) => {
              const title = getReadableProductName(report.title)
              const original = getReadableOriginalTitle(report.title, title)
              return (
                <div
                  key={report.reportId}
                  className="tradeflow-card rounded-[var(--tf-radius-xl)] p-5 transition hover:-translate-y-0.5 hover:border-[var(--tf-border-strong)]"
                >
                  <Link href={`/reports/${report.reportId}`} className="block">
                    <p className="text-base font-semibold text-[var(--tf-text)]">{title}</p>
                    {original && <p className="mt-1 line-clamp-2 text-xs leading-6 text-[var(--tf-text-muted)]">{original}</p>}
                    <div className="mt-4 flex flex-wrap gap-2 text-[11px] text-[var(--tf-text-muted)]">
                      <span className="rounded-full border border-[var(--tf-border)] px-2 py-1">{t(`decision.${report.decision}`)}</span>
                      <span className="rounded-full border border-[var(--tf-border)] px-2 py-1">{t(`risk.${report.riskLevel}`)}</span>
                      {report.qualityTier && (
                        <span className="rounded-full border border-[var(--tf-border)] px-2 py-1">
                          {humanizeQualityTier(report.qualityTier, t)}
                        </span>
                      )}
                      <span className="rounded-full border border-[var(--tf-border)] px-2 py-1">
                        {t("report.fallback")}: {report.fallbackUsed ? t("common.yes") : t("common.no")}
                      </span>
                    </div>
                    <div className="mt-3 grid grid-cols-2 gap-2">
                      <div className="rounded-xl border border-[var(--tf-border)] bg-white/80 px-3 py-2">
                        <p className="text-[10px] text-[var(--tf-text-subtle)]">{t("report.expectedMargin")}</p>
                        <p className="mt-1 text-sm font-semibold text-emerald-600">{formatPercent(report.margin)}</p>
                      </div>
                      <div className="rounded-xl border border-[var(--tf-border)] bg-white/80 px-3 py-2">
                        <p className="text-[10px] text-[var(--tf-text-subtle)]">{t("report.matchSource")}</p>
                        <p className="mt-1 line-clamp-2 text-xs font-medium text-[var(--tf-text)]">{humanizeReportSource(report.retrievalSource, t)}</p>
                      </div>
                    </div>
                    {report.detailSource && (
                      <p className="mt-3 text-xs text-[var(--tf-text-muted)]">
                        {t("report.detailSource")}: {humanizeReportSource(report.detailSource, t)}
                      </p>
                    )}
                    <p className="mt-3 text-xs text-[var(--tf-text-subtle)]">{new Date(report.generatedAt).toLocaleString()}</p>
                  </Link>
                  <div className="mt-4">
                    <Link href={`/?taskId=${report.taskId}&reportId=${report.reportId}`}>
                      <Button variant="outline" size="sm">
                        {t("productGrid.openWorkbench")}
                        <ArrowRight className="size-4" />
                      </Button>
                    </Link>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </AppShell>
  )
}
