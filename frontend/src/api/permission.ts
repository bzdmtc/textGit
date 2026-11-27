import http from './http'
import type { ApiResponse, Group, GroupUser, Position, Priv } from '@/types'

export interface PositionPayload {
  name: string
  groupId: number
  sort?: number
}

export interface GroupPayload {
  code: string
  name: string
  description?: string | null
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
  groupUsers(groupId: number) {
    return http.get<ApiResponse<GroupUser[]>>(`/admin/groups/${groupId}/users`)
  },
  positions() {
    return http.get<ApiResponse<Position[]>>('/admin/positions')
  },
  createPosition(payload: PositionPayload) {
    return http.post<ApiResponse<Position>>('/admin/positions', payload)
  },
  updatePosition(id: number, payload: PositionPayload) {
    return http.put<ApiResponse<Position>>(`/admin/positions/${id}`, payload)
  },
  setPositionStatus(id: number, status: string) {
    return http.put<ApiResponse<Position>>(`/admin/positions/${id}/status`, { status })
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
