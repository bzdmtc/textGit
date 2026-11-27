import http from './http'
import type { ApiResponse, AuditLog, PageResult, User, UserPermissionView } from '@/types'

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

export const userApi = {
  page(keyword: string, page: number, size: number) {
    return http.get<ApiResponse<PageResult<User>>>('/admin/users', {
      params: { keyword: keyword || undefined, page, size },
    })
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
  auditLogs(targetType: string, page: number, size: number) {
    return http.get<ApiResponse<PageResult<AuditLog>>>('/admin/audit-logs', {
      params: { targetType: targetType || undefined, page, size },
    })
  },
}
