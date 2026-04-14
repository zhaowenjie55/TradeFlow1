"use client"

import { useEffect } from "react"
import { useSearchParams } from "next/navigation"
import { GripVertical } from "lucide-react"
import { Group, Panel, Separator } from "react-resizable-panels"

import { AnalysisPanel } from "@/components/layout/analysis-panel"
import { Sidebar } from "@/components/layout/sidebar"
import { Topbar } from "@/components/layout/topbar"
import { ParameterForm } from "@/components/agent/parameter-form"
import { ProductGrid } from "@/components/product/product-grid"
import { useTaskRunner } from "@/hooks/use-task-runner"
import { getReportByReportId, getReportByTaskId } from "@/lib/api/report"
import { getTaskStatus } from "@/lib/api/task"
import { useAgentStore } from "@/stores/agent-store"
import { useProductsStore } from "@/stores/products-store"
import { useSettingsStore } from "@/stores/settings-store"
import { useTaskStore } from "@/stores/task-store"
import { useUIStore } from "@/stores/ui-store"

export function Workbench() {
  const searchParams = useSearchParams()
  const hydrateUI = useUIStore((state) => state.hydrate)
  const setLayoutMode = useUIStore((state) => state.setLayoutMode)
  const layoutMode = useUIStore((state) => state.layoutMode)
  const interfaceMode = useUIStore((state) => state.interfaceMode)
  const hydrateSettings = useSettingsStore((state) => state.hydrate)
  const { refreshHistory } = useTaskRunner()

  useEffect(() => {
    hydrateUI()
    hydrateSettings()
    void refreshHistory()
    const handleResize = () => setLayoutMode(window.innerWidth)
    window.addEventListener("resize", handleResize)
    return () => window.removeEventListener("resize", handleResize)
  }, [hydrateSettings, hydrateUI, refreshHistory, setLayoutMode])

  useEffect(() => {
    const taskId = searchParams.get("taskId")
    const reportId = searchParams.get("reportId")
    if (!taskId && !reportId) return

    let alive = true

    const hydrateFromTask = async () => {
      try {
        const snapshot = taskId ? await getTaskStatus(taskId) : null
        if (!alive) return

        if (snapshot) {
          useTaskStore.getState().applyStatusSnapshot(snapshot)
          useTaskStore.getState().setPolling(false)
          useAgentStore.getState().setTaskLogs(snapshot.logs)

          if (snapshot.phase === "PHASE1") {
            useProductsStore.getState().setCandidates(snapshot.candidates)
            useProductsStore.getState().setLoading(false)
            useProductsStore.getState().setReportAnalyzing(false)
          } else {
            useProductsStore.getState().selectProduct(snapshot.productId)
            useProductsStore.getState().setLoading(false)
            useProductsStore.getState().setReportAnalyzing(false)

            if (snapshot.report) {
              useProductsStore.getState().setReport(snapshot.report)
            } else if (snapshot.status === "REPORT_READY") {
              const report = await getReportByTaskId(snapshot.taskId)
              if (!alive) return
              useProductsStore.getState().setReport(report)
              useProductsStore.getState().selectProduct(report.productId)
            }
          }
        }

        if (!snapshot && reportId) {
          const report = await getReportByReportId(reportId)
          if (!alive) return
          useProductsStore.getState().setReport(report)
          useProductsStore.getState().selectProduct(report.productId)
        }
      } catch {
        // keep workbench usable even if deep-link hydration fails
      }
    }

    void hydrateFromTask()

    return () => {
      alive = false
    }
  }, [searchParams])

  return (
    <div className="tradeflow-shell flex h-screen overflow-hidden bg-[var(--tf-bg)] text-[var(--tf-text)]">
      <Sidebar />
      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <Topbar />
        <main className="min-h-0 flex-1 overflow-hidden p-1.5">
          {layoutMode === "desktop" ? (
            <section
              className={
                interfaceMode === "stream"
                  ? "tradeflow-panel grid h-full overflow-hidden rounded-[22px] [grid-template-columns:minmax(18rem,19rem)_minmax(0,1fr)]"
                  : "tradeflow-panel grid h-full overflow-hidden rounded-[22px] [grid-template-columns:minmax(18rem,19rem)_minmax(44rem,1fr)_minmax(22.5rem,23rem)]"
              }
            >
              <div className="min-h-0 border-r border-[var(--tf-border)]">
                <ParameterForm />
              </div>

              {interfaceMode === "stream" ? (
                <div className="min-h-0">
                  <Group orientation="vertical" className="h-full min-h-0">
                    <Panel defaultSize={60} minSize={50} className="min-h-0">
                      <ProductGrid />
                    </Panel>
                    <ResizeHandle orientation="vertical" />
                    <Panel defaultSize={40} minSize={28} className="min-h-0">
                      <div className="h-full min-h-0 border-t border-[var(--tf-border)]">
                        <AnalysisPanel />
                      </div>
                    </Panel>
                  </Group>
                </div>
              ) : (
                <>
                  <div className="min-h-0">
                    <ProductGrid />
                  </div>
                  <div className="min-h-0 border-l border-[var(--tf-border)]">
                    <AnalysisPanel splitView />
                  </div>
                </>
              )}
            </section>
          ) : (
            <div className="space-y-3">
              <section className="tradeflow-panel overflow-hidden rounded-[22px]">
                <ParameterForm />
              </section>
              <section className="tradeflow-panel min-h-[24rem] overflow-hidden rounded-[22px]">
                <ProductGrid />
              </section>
              <section className="tradeflow-panel min-h-[22rem] overflow-hidden rounded-[22px]">
                <AnalysisPanel />
              </section>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}

function ResizeHandle({ orientation = "horizontal" }: { orientation?: "horizontal" | "vertical" }) {
  return (
    <Separator
      className={
        orientation === "horizontal"
          ? "group relative flex w-2 shrink-0 items-center justify-center bg-transparent outline-none"
          : "group relative flex h-2 shrink-0 items-center justify-center bg-transparent outline-none"
      }
    >
      <div
        className={
          orientation === "horizontal"
            ? "h-full w-px bg-[var(--tf-border)] transition-colors group-data-[resize-handle-active]:bg-[var(--tf-accent)]"
            : "h-px w-full bg-[var(--tf-border)] transition-colors group-data-[resize-handle-active]:bg-[var(--tf-accent)]"
        }
      />
      <div
        className={
          orientation === "horizontal"
            ? "absolute flex size-6 items-center justify-center rounded-full border border-[var(--tf-border)] bg-white text-[var(--tf-text-subtle)] shadow-sm transition group-data-[resize-handle-active]:border-[var(--tf-accent)] group-data-[resize-handle-active]:text-[var(--tf-accent)]"
            : "absolute flex size-6 items-center justify-center rounded-full border border-[var(--tf-border)] bg-white text-[var(--tf-text-subtle)] shadow-sm transition group-data-[resize-handle-active]:border-[var(--tf-accent)] group-data-[resize-handle-active]:text-[var(--tf-accent)] rotate-90"
        }
      >
        <GripVertical className="size-3.5" />
      </div>
    </Separator>
  )
}
