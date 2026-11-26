import http from './http'
import type {
  ApiResponse,
  Article,
  ArticleCategory,
  Banner,
  Material,
  PageResult,
} from '@/types'

export interface ArticlePayload {
  categoryId?: number | null
  title: string
  summary?: string
  coverUrl?: string
  contentHtml?: string
  author?: string
  status?: string
}

export interface MaterialPayload {
  title: string
  subject?: string
  fileUrl: string
  fileSize?: number
  status?: string
}

export interface BannerPayload {
  title: string
  imageUrl: string
  linkUrl?: string
  sort?: number
  status?: string
  startAt?: string | null
  endAt?: string | null
}

export const contentAdminApi = {
  articles(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<Article>>>('/admin/articles', { params })
  },
  categories() {
    return http.get<ApiResponse<ArticleCategory[]>>('/admin/article-categories')
  },
  createArticle(payload: ArticlePayload) {
    return http.post<ApiResponse<Article>>('/admin/articles', payload)
  },
  updateArticle(id: number, payload: ArticlePayload) {
    return http.put<ApiResponse<Article>>(`/admin/articles/${id}`, payload)
  },
  publishArticle(id: number, status: string) {
    return http.put<ApiResponse<Article>>(`/admin/articles/${id}/status`, { status })
  },
  disableArticle(id: number) {
    return http.delete<ApiResponse<void>>(`/admin/articles/${id}`)
  },
  materials(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<Material>>>('/admin/materials', { params })
  },
  createMaterial(payload: MaterialPayload) {
    return http.post<ApiResponse<Material>>('/admin/materials', payload)
  },
  updateMaterial(id: number, payload: MaterialPayload) {
    return http.put<ApiResponse<Material>>(`/admin/materials/${id}`, payload)
  },
  disableMaterial(id: number) {
    return http.delete<ApiResponse<void>>(`/admin/materials/${id}`)
  },
  banners() {
    return http.get<ApiResponse<Banner[]>>('/admin/banners')
  },
  createBanner(payload: BannerPayload) {
    return http.post<ApiResponse<Banner>>('/admin/banners', payload)
  },
  updateBanner(id: number, payload: BannerPayload) {
    return http.put<ApiResponse<Banner>>(`/admin/banners/${id}`, payload)
  },
  disableBanner(id: number) {
    return http.delete<ApiResponse<void>>(`/admin/banners/${id}`)
  },
}
