import type { ReportDetail, ReportListResponse } from "@/types"
import { apiRequest } from "@/lib/api/api"

export function getReportList() {
  return apiRequest<ReportListResponse>("/api/report/list")
}

export function getReportByTaskId(taskId: string) {
  return apiRequest<ReportDetail>(`/api/report/${taskId}`)
}

export function getReportByReportId(reportId: string) {
  return apiRequest<ReportDetail>(`/api/report/by-report/${reportId}`)
}
