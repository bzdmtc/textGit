import http from './http'
import type {
  ApiResponse,
  CourseReview,
  FavoriteCourse,
  LearnCourse,
  NoteItem,
  PageResult,
  ProgressResult,
  ReviewSummary,
} from '@/types'

export interface ProgressPayload {
  watchedSec: number
  completed?: boolean
}

export const learnApi = {
  course(courseId: number) {
    return http.get<ApiResponse<LearnCourse>>(`/learn/courses/${courseId}`)
  },
  reportProgress(courseId: number, lessonId: number, payload: ProgressPayload) {
    return http.post<ApiResponse<ProgressResult>>(
      `/learn/courses/${courseId}/lessons/${lessonId}/progress`,
      payload,
    )
  },
  notes(courseId: number, lessonId: number) {
    return http.get<ApiResponse<NoteItem[]>>(`/learn/courses/${courseId}/lessons/${lessonId}/notes`)
  },
  createNote(courseId: number, lessonId: number, content: string) {
    return http.post<ApiResponse<NoteItem>>(
      `/learn/courses/${courseId}/lessons/${lessonId}/notes`,
      { content },
    )
  },
  updateNote(noteId: number, content: string) {
    return http.put<ApiResponse<NoteItem>>(`/learn/notes/${noteId}`, { content })
  },
  deleteNote(noteId: number) {
    return http.delete<ApiResponse<void>>(`/learn/notes/${noteId}`)
  },
  favorites() {
    return http.get<ApiResponse<FavoriteCourse[]>>('/learn/favorites')
  },
  favoriteStatus(courseId: number) {
    return http.get<ApiResponse<{ courseId: number; favorited: boolean }>>(
      `/learn/favorites/${courseId}/status`,
    )
  },
  addFavorite(courseId: number) {
    return http.post<ApiResponse<{ courseId: number; favorited: boolean }>>(`/learn/favorites/${courseId}`)
  },
  removeFavorite(courseId: number) {
    return http.delete<ApiResponse<{ courseId: number; favorited: boolean }>>(`/learn/favorites/${courseId}`)
  },
  submitReview(courseId: number, payload: { rating: number; content: string }) {
    return http.post<ApiResponse<CourseReview>>(`/learn/courses/${courseId}/reviews`, payload)
  },
}

export const reviewPublicApi = {
  forCourse(courseId: number) {
    return http.get<ApiResponse<ReviewSummary>>(`/public/courses/${courseId}/reviews`)
  },
}

export const reviewAdminApi = {
  page(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<CourseReview>>>('/admin/reviews', { params })
  },
  moderate(id: number, status: 'APPROVED' | 'REJECTED', adminNote?: string) {
    return http.put<ApiResponse<CourseReview>>(`/admin/reviews/${id}/status`, { status, adminNote })
  },
}
