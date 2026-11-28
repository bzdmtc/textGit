import http from './http'
import type { ApiResponse, AuditLog, PageResult, User, UserActivity, UserPermissionView } from '@/types'

export interface UserCreatePayload {
  account: string
  password: string
  realname?: string
  nickname?: string
  position?: string
  phone?: string
  email?: string
  groupIds?: number[]
}

export interface UserUpdatePayload {
  realname?: string
  nickname?: string
  position?: string
  phone?: string
  email?: string
  avatar?: string
  groupIds?: number[]
}

export interface UserPageParams {
  keyword?: string
  status?: string
  position?: string
  page: number
  size: number
}

export interface AuditLogParams {
  targetType?: string
  action?: string
  userId?: number
  from?: string
  to?: string
  page: number
  size: number
}

export const userApi = {
  page(params: UserPageParams) {
    return http.get<ApiResponse<PageResult<User>>>('/admin/users', { params })
  },
  get(userId: number) {
    return http.get<ApiResponse<User>>(`/admin/users/${userId}`)
  },
  create(payload: UserCreatePayload) {
    return http.post<ApiResponse<User>>('/admin/users', payload)
  },
  update(userId: number, payload: UserUpdatePayload) {
    return http.put<ApiResponse<User>>(`/admin/users/${userId}`, payload)
  },
  setStatus(userId: number, status: string) {
    return http.put<ApiResponse<User>>(`/admin/users/${userId}/status`, { status })
  },
  assignGroups(userId: number, groupIds: number[]) {
    return http.put<ApiResponse<User>>(`/admin/users/${userId}/groups`, { groupIds })
  },
  permissions(userId: number) {
    return http.get<ApiResponse<UserPermissionView>>(`/admin/users/${userId}/permissions`)
  },
  activity(userId: number) {
    return http.get<ApiResponse<UserActivity>>(`/admin/users/${userId}/activity`)
  },
  auditLogs(params: AuditLogParams) {
    return http.get<ApiResponse<PageResult<AuditLog>>>('/admin/audit-logs', { params })
  },
  auditTargetTypes() {
    return http.get<ApiResponse<string[]>>('/admin/audit-logs/target-types')
  },
}
