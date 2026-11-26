import http from './http'
import type { ApiResponse, Group, Priv } from '@/types'

export interface GroupPayload {
  code: string
  name: string
  role?: string
  dataScope: string
  limited: boolean
  scopeType?: string
  scopeId?: number
  sort?: number
}

export const permissionApi = {
  groups() {
    return http.get<ApiResponse<Group[]>>('/admin/groups')
  },
  groupPrivIds(groupId: number) {
    return http.get<ApiResponse<number[]>>(`/admin/groups/${groupId}/privs`)
  },
  createGroup(payload: GroupPayload) {
    return http.post<ApiResponse<Group>>('/admin/groups', payload)
  },
  updateGroup(groupId: number, payload: GroupPayload) {
    return http.put<ApiResponse<Group>>(`/admin/groups/${groupId}`, payload)
  },
  assignPrivs(groupId: number, privIds: number[]) {
    return http.put<ApiResponse<void>>(`/admin/groups/${groupId}/privs`, { privIds })
  },
  disableGroup(groupId: number) {
    return http.delete<ApiResponse<void>>(`/admin/groups/${groupId}`)
  },
  privs() {
    return http.get<ApiResponse<Priv[]>>('/admin/privs')
  },
}
