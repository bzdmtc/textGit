import http from './http'
import type { ApiResponse, RouteCheck } from '@/types'

export const systemApi = {
  readRoute() {
    return http.get<ApiResponse<RouteCheck>>('/system/route-check/read')
  },
  masterRoute() {
    return http.get<ApiResponse<RouteCheck>>('/system/route-check/master')
  },
}
