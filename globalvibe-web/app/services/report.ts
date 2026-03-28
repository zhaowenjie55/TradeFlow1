import type { ReportDetail, ReportListResponse } from '~/types'
import { apiRequest } from './api'

export const getReportList = () => {
  return apiRequest<ReportListResponse>('/api/report/list')
}

export const getReportByTaskId = (taskId: string) => {
  return apiRequest<ReportDetail>(`/api/report/${taskId}`)
}

export const getReportByReportId = (reportId: string) => {
  return apiRequest<ReportDetail>(`/api/report/by-report/${reportId}`)
}
