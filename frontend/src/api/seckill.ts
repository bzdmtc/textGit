import http from './http'
import type {
  ApiResponse,
  PageResult,
  PublicSeckill,
  SeckillActivity,
  SeckillClaimResult,
  SeckillRecord,
  SeckillResult,
} from '@/types'

export interface SeckillActivityPayload {
  courseId: number
  seckillPrice: number
  totalStock: number
  perUserLimit: number
  startAt: string
  endAt: string
  status?: string
}

export const seckillApi = {
  list() {
    return http.get<ApiResponse<PublicSeckill[]>>('/seckill/activities')
  },
  claim(activityId: number) {
    return http.post<ApiResponse<SeckillClaimResult>>(`/seckill/activities/${activityId}/claim`)
  },
  result(activityId: number) {
    return http.get<ApiResponse<SeckillResult>>(`/seckill/activities/${activityId}/result`)
  },
}

export const seckillAdminApi = {
  page(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<SeckillActivity>>>('/admin/seckill/activities', { params })
  },
  create(payload: SeckillActivityPayload) {
    return http.post<ApiResponse<SeckillActivity>>('/admin/seckill/activities', payload)
  },
  update(id: number, payload: SeckillActivityPayload) {
    return http.put<ApiResponse<SeckillActivity>>(`/admin/seckill/activities/${id}`, payload)
  },
  setStatus(id: number, status: string) {
    return http.put<ApiResponse<SeckillActivity>>(`/admin/seckill/activities/${id}/status`, { status })
  },
  remove(id: number) {
    return http.delete<ApiResponse<void>>(`/admin/seckill/activities/${id}`)
  },
  reconcile(id: number) {
    return http.post<ApiResponse<SeckillActivity>>(`/admin/seckill/activities/${id}/reconcile`)
  },
  records(id: number, params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<SeckillRecord>>>(`/admin/seckill/activities/${id}/records`, { params })
  },
}
