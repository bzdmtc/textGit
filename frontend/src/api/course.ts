import http from './http'
import type { ApiResponse, Chapter, Course, CourseDetail, Lesson, PageResult, Teacher } from '@/types'

export interface CoursePayload {
  title: string
  subtitle?: string
  subject: string
  stage: string
  coverUrl?: string
  description?: string
  price: number
  originalPrice?: number | null
  status?: string
  teacherIds?: number[]
}

export interface TeacherPayload {
  name: string
  title?: string
  avatar?: string
  subject?: string
  introduction?: string
  sort?: number
  status?: string
}

export interface ChapterPayload {
  title: string
  sort?: number
}

export interface LessonPayload {
  title: string
  videoUrl?: string
  durationSec?: number
  isTrial?: boolean
  sort?: number
  status?: string
}

export const courseAdminApi = {
  page(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<Course>>>('/admin/courses', { params })
  },
  detail(id: number) {
    return http.get<ApiResponse<CourseDetail>>(`/admin/courses/${id}`)
  },
  create(payload: CoursePayload) {
    return http.post<ApiResponse<Course>>('/admin/courses', payload)
  },
  update(id: number, payload: CoursePayload) {
    return http.put<ApiResponse<Course>>(`/admin/courses/${id}`, payload)
  },
  setStatus(id: number, status: string) {
    return http.put<ApiResponse<Course>>(`/admin/courses/${id}/status`, { status })
  },
  remove(id: number) {
    return http.delete<ApiResponse<void>>(`/admin/courses/${id}`)
  },
  addChapter(courseId: number, payload: ChapterPayload) {
    return http.post<ApiResponse<Chapter>>(`/admin/courses/${courseId}/chapters`, payload)
  },
  updateChapter(courseId: number, chapterId: number, payload: ChapterPayload) {
    return http.put<ApiResponse<Chapter>>(`/admin/courses/${courseId}/chapters/${chapterId}`, payload)
  },
  removeChapter(courseId: number, chapterId: number) {
    return http.delete<ApiResponse<void>>(`/admin/courses/${courseId}/chapters/${chapterId}`)
  },
  addLesson(courseId: number, chapterId: number, payload: LessonPayload) {
    return http.post<ApiResponse<Lesson>>(`/admin/courses/${courseId}/chapters/${chapterId}/lessons`, payload)
  },
  updateLesson(courseId: number, chapterId: number, lessonId: number, payload: LessonPayload) {
    return http.put<ApiResponse<Lesson>>(
      `/admin/courses/${courseId}/chapters/${chapterId}/lessons/${lessonId}`,
      payload,
    )
  },
  removeLesson(courseId: number, chapterId: number, lessonId: number) {
    return http.delete<ApiResponse<void>>(
      `/admin/courses/${courseId}/chapters/${chapterId}/lessons/${lessonId}`,
    )
  },
}

export const teacherAdminApi = {
  list(keyword?: string) {
    return http.get<ApiResponse<Teacher[]>>('/admin/teachers', { params: { keyword: keyword || undefined } })
  },
  create(payload: TeacherPayload) {
    return http.post<ApiResponse<Teacher>>('/admin/teachers', payload)
  },
  update(id: number, payload: TeacherPayload) {
    return http.put<ApiResponse<Teacher>>(`/admin/teachers/${id}`, payload)
  },
  disable(id: number) {
    return http.delete<ApiResponse<void>>(`/admin/teachers/${id}`)
  },
}
