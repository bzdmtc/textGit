import http from './http'
import type { ApiResponse, DashboardData } from '@/types'

export const statsApi = {
  dashboard(days = 14) {
    return http.get<ApiResponse<DashboardData>>('/admin/stats/dashboard', { params: { days } })
  },
}
