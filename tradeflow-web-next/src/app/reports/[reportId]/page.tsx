"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { ArrowLeft } from "lucide-react"

import { AppShell } from "@/components/layout/app-shell"
import { ArbitrageReport } from "@/components/report/arbitrage-report"
import { Button } from "@/components/ui/button"
import { getReportByReportId } from "@/lib/api/report"
import type { ReportDetail } from "@/types"

export default function ReportDetailPage({ params }: { params: Promise<{ reportId: string }> }) {
  const [report, setReport] = useState<ReportDetail | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState("")

  useEffect(() => {
    let alive = true
    void params.then(({ reportId }) => {
      void getReportByReportId(reportId)
        .then((next) => {
          if (alive) setReport(next)
        })
        .catch((error) => {
          if (alive) setErrorMessage(error instanceof Error ? error.message : "Failed to load report")
        })
        .finally(() => {
          if (alive) setIsLoading(false)
        })
    })
    return () => {
      alive = false
    }
  }, [params])

  return (
    <AppShell activePanel="reports">
      <div className="mx-auto flex min-h-full max-w-6xl flex-col">
        <div className="flex items-center gap-3">
          <Link href="/reports">
            <Button variant="outline" size="sm">
              <ArrowLeft className="size-4" />
              返回报告列表
            </Button>
          </Link>
          {report && (
            <Link href={`/?taskId=${report.taskId}&reportId=${report.reportId}`}>
              <Button variant="outline" size="sm">
                在工作台中打开
              </Button>
            </Link>
          )}
        </div>

        {errorMessage ? (
          <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-6 text-sm text-red-700">{errorMessage}</div>
        ) : (
          <div className="tradeflow-panel mt-6 min-h-0 flex-1 overflow-auto rounded-[var(--tf-radius-2xl)]">
            <ArbitrageReport report={report} candidateTitle={report?.title ?? null} isLoading={isLoading} />
          </div>
        )}
      </div>
    </AppShell>
  )
}
