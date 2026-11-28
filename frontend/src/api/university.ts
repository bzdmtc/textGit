import http from './http'
import type {
  AdmissionStat,
  ApiResponse,
  PageResult,
  ScoreLine,
  University,
  UniversityDetail,
  UniversityMajor,
  UniversityOptions,
} from '@/types'

export interface UniversityPayload {
  name: string
  province: string
  city?: string
  level?: string
  type?: string
  logoUrl?: string
  website?: string
  introduction?: string
  featured?: boolean
  sort?: number
}

export interface MajorPayload {
  name: string
  code?: string
  degreeType: string
  duration?: string
  tuition?: number | null
  examSubjects?: string
  introduction?: string
  sort?: number
}

export interface ScoreLinePayload {
  majorId: number
  year: number
  political?: number | null
  english?: number | null
  math?: number | null
  majorScore?: number | null
  total: number
}

export interface AdmissionStatPayload {
  majorId: number
  year: number
  applicants: number
  admitted: number
  minScore?: number | null
}

export const publicUniversityApi = {
  page(params: {
    keyword?: string
    province?: string
    level?: string
    type?: string
    page?: number
    size?: number
  }) {
    return http.get<ApiResponse<PageResult<University>>>('/public/universities', { params })
  },
  options() {
    return http.get<ApiResponse<UniversityOptions>>('/public/universities/options')
  },
  featured(limit = 6) {
    return http.get<ApiResponse<University[]>>('/public/universities/featured', { params: { limit } })
  },
  detail(id: number) {
    return http.get<ApiResponse<UniversityDetail>>(`/public/universities/${id}`)
  },
}

export const universityApi = {
  page(params: {
    keyword?: string
    province?: string
    level?: string
    type?: string
    status?: string
    page?: number
    size?: number
  }) {
    return http.get<ApiResponse<PageResult<University>>>('/admin/universities', { params })
  },
  options() {
    return http.get<ApiResponse<UniversityOptions>>('/admin/universities/options')
  },
  detail(id: number) {
    return http.get<ApiResponse<UniversityDetail>>(`/admin/universities/${id}`)
  },
  create(payload: UniversityPayload) {
    return http.post<ApiResponse<University>>('/admin/universities', payload)
  },
  update(id: number, payload: UniversityPayload) {
    return http.put<ApiResponse<University>>(`/admin/universities/${id}`, payload)
  },
  setStatus(id: number, status: string) {
    return http.put<ApiResponse<University>>(`/admin/universities/${id}/status`, { status })
  },
  remove(id: number) {
    return http.delete<ApiResponse<void>>(`/admin/universities/${id}`)
  },
  createMajor(universityId: number, payload: MajorPayload) {
    return http.post<ApiResponse<UniversityMajor>>(`/admin/universities/${universityId}/majors`, payload)
  },
  updateMajor(universityId: number, majorId: number, payload: MajorPayload) {
    return http.put<ApiResponse<UniversityMajor>>(
      `/admin/universities/${universityId}/majors/${majorId}`,
      payload,
    )
  },
  removeMajor(universityId: number, majorId: number) {
    return http.delete<ApiResponse<void>>(`/admin/universities/${universityId}/majors/${majorId}`)
  },
  createScoreLine(universityId: number, payload: ScoreLinePayload) {
    return http.post<ApiResponse<ScoreLine>>(`/admin/universities/${universityId}/score-lines`, payload)
  },
  updateScoreLine(universityId: number, lineId: number, payload: ScoreLinePayload) {
    return http.put<ApiResponse<ScoreLine>>(
      `/admin/universities/${universityId}/score-lines/${lineId}`,
      payload,
    )
  },
  removeScoreLine(universityId: number, lineId: number) {
    return http.delete<ApiResponse<void>>(`/admin/universities/${universityId}/score-lines/${lineId}`)
  },
  createAdmissionStat(universityId: number, payload: AdmissionStatPayload) {
    return http.post<ApiResponse<AdmissionStat>>(
      `/admin/universities/${universityId}/admission-stats`,
      payload,
    )
  },
  updateAdmissionStat(universityId: number, statId: number, payload: AdmissionStatPayload) {
    return http.put<ApiResponse<AdmissionStat>>(
      `/admin/universities/${universityId}/admission-stats/${statId}`,
      payload,
    )
  },
  removeAdmissionStat(universityId: number, statId: number) {
    return http.delete<ApiResponse<void>>(`/admin/universities/${universityId}/admission-stats/${statId}`)
  },
}
