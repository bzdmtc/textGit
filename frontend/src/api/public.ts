import http from './http'
import type {
  ApiResponse,
  Article,
  ArticleCategory,
  Banner,
  Course,
  CourseDetail,
  HomeData,
  Material,
  PageResult,
  Teacher,
} from '@/types'

export const publicApi = {
  home() {
    return http.get<ApiResponse<HomeData>>('/public/home')
  },
  courses(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<Course>>>('/public/courses', { params })
  },
  courseDetail(id: number) {
    return http.get<ApiResponse<CourseDetail>>(`/public/courses/${id}`)
  },
  teachers() {
    return http.get<ApiResponse<Teacher[]>>('/public/teachers')
  },
  teacherDetail(id: number) {
    return http.get<ApiResponse<Teacher>>(`/public/teachers/${id}`)
  },
  articleCategories() {
    return http.get<ApiResponse<ArticleCategory[]>>('/public/article-categories')
  },
  articles(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<Article>>>('/public/articles', { params })
  },
  articleDetail(id: number) {
    return http.get<ApiResponse<Article>>(`/public/articles/${id}`)
  },
  materials(subject?: string) {
    return http.get<ApiResponse<Material[]>>('/public/materials', { params: { subject: subject || undefined } })
  },
  banners() {
    return http.get<ApiResponse<Banner[]>>('/public/banners')
  },
}
