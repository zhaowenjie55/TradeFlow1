"use client"

import { Group, Panel, Separator } from "react-resizable-panels"

import { useAppI18n } from "@/components/layout/locale-provider"
import { humanizeQualityTier } from "@/lib/presentation"
import { useProductsStore } from "@/stores/products-store"
import { useUIStore } from "@/stores/ui-store"
import { Button } from "@/components/ui/button"
import { ArbitrageReport } from "@/components/report/arbitrage-report"
import { ThinkingLog } from "@/components/report/thinking-log"

interface AnalysisPanelProps {
  splitView?: boolean
}

export function AnalysisPanel({ splitView = false }: AnalysisPanelProps) {
  const { t } = useAppI18n()
  const selectedProductId = useProductsStore((state) => state.selectedProductId)
  const currentReport = useProductsStore((state) =>
    state.selectedProductId ? state.reportsByProductId[state.selectedProductId] ?? null : null,
  )
  const analysisPanelTab = useUIStore((state) => state.analysisPanelTab)
  const setAnalysisPanelTab = useUIStore((state) => state.setAnalysisPanelTab)
  const qualityTier = currentReport?.provenance?.qualityTier
  const fallbackUsed = currentReport?.provenance?.fallbackUsed

  if (splitView) {
    return (
      <div className="flex h-full min-h-0 flex-col">
        <div className="border-b border-[var(--tf-border)] px-3 py-2">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs uppercase tracking-[0.2em] text-[var(--tf-text-subtle)]">{t("analysisPanel.eyebrow")}</p>
              <h2 className="mt-0.5 text-sm font-semibold text-[var(--tf-text)]">{t("analysisPanel.title")}</h2>
            </div>
            {selectedProductId && currentReport?.provenance && (
              <div className="flex flex-wrap justify-end gap-1.5">
                {qualityTier && (
                  <span className="rounded-full border border-[var(--tf-border)] bg-white px-2 py-1 text-[10px] text-[var(--tf-text-muted)]">
                    {humanizeQualityTier(qualityTier, t)}
                  </span>
                )}
                <span className="rounded-full border border-[var(--tf-border)] bg-white px-2 py-1 text-[10px] text-[var(--tf-text-muted)]">
                  {t("report.fallback")} · {t(fallbackUsed ? "common.yes" : "common.no")}
                </span>
              </div>
            )}
          </div>
        </div>
        <div className="min-h-0 flex-1">
          <Group orientation="vertical" className="h-full min-h-0">
            <Panel defaultSize={42} minSize={25} className="min-h-0">
              <ThinkingLog />
            </Panel>
            <Separator className="group relative flex h-1.5 shrink-0 items-center justify-center bg-[var(--tf-border)] transition-colors hover:bg-[var(--tf-accent)]" />
            <Panel defaultSize={58} minSize={30} className="min-h-0">
              <ArbitrageReport />
            </Panel>
          </Group>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="flex items-center justify-between border-b border-[var(--tf-border)] px-3 py-2">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-[var(--tf-text-subtle)]">{t("analysisPanel.eyebrow")}</p>
          <h2 className="mt-0.5 text-sm font-semibold text-[var(--tf-text)]">{t("analysisPanel.title")}</h2>
        </div>
        <div className="flex items-center gap-2">
          {selectedProductId && currentReport?.provenance && qualityTier && (
            <span className="rounded-full border border-[var(--tf-border)] bg-white px-2 py-1 text-[10px] text-[var(--tf-text-muted)]">
              {humanizeQualityTier(qualityTier, t)}
            </span>
          )}
          <div className="inline-flex rounded-xl border border-[var(--tf-border)] bg-white p-0.5">
            <Button
              size="sm"
              variant={analysisPanelTab === "logs" ? "secondary" : "ghost"}
              onClick={() => setAnalysisPanelTab("logs")}
            >
              {t("analysisPanel.logs")}
            </Button>
            <Button
              size="sm"
              variant={analysisPanelTab === "report" ? "secondary" : "ghost"}
              onClick={() => setAnalysisPanelTab("report")}
            >
              {t("analysisPanel.report")}
            </Button>
          </div>
        </div>
      </div>
      <div className="min-h-0 flex-1">
        {analysisPanelTab === "logs" ? <ThinkingLog /> : <ArbitrageReport />}
      </div>
    </div>
  )
}
