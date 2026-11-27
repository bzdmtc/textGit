import http from './http'
import type {
  AnswerFeedback,
  ApiResponse,
  ExamRecord,
  ExamResult,
  ExamSession,
  PageResult,
  Paper,
  PaperDetail,
  PracticeOverview,
  QuestionItem,
  QuestionOption,
  WrongQuestion,
} from '@/types'

export interface QuestionPayload {
  subject: string
  chapter: string
  type: string
  stem: string
  analysis?: string | null
  difficulty?: number
  options: QuestionOption[]
}

export interface PaperPayload {
  title: string
  type: string
  subject: string
  chapter?: string | null
  description?: string | null
  durationMin: number
  questions: { questionId: number; score: number }[]
}

export const practiceApi = {
  overview() {
    return http.get<ApiResponse<PracticeOverview>>('/practice/overview')
  },
  startPractice(payload: { subject: string; chapter?: string | null; count?: number; source?: 'NORMAL' | 'WRONG' }) {
    return http.post<ApiResponse<ExamSession>>('/practice/sessions', payload)
  },
}

export const examApi = {
  start(paperId: number) {
    return http.post<ApiResponse<ExamSession>>('/exams', { paperId })
  },
  session(recordId: number) {
    return http.get<ApiResponse<ExamSession>>(`/exams/${recordId}`)
  },
  saveAnswer(recordId: number, questionId: number, answer: string) {
    return http.put<ApiResponse<AnswerFeedback>>(`/exams/${recordId}/answers`, { questionId, answer })
  },
  submit(recordId: number) {
    return http.post<ApiResponse<ExamResult>>(`/exams/${recordId}/submit`)
  },
  result(recordId: number) {
    return http.get<ApiResponse<ExamResult>>(`/exams/${recordId}/result`)
  },
  records(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<ExamRecord>>>('/exams/records', { params })
  },
}

export const wrongQuestionApi = {
  page(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<WrongQuestion>>>('/wrong-questions', { params })
  },
  setMastered(id: number, mastered: boolean) {
    return http.put<ApiResponse<WrongQuestion>>(`/wrong-questions/${id}/mastered`, { mastered })
  },
  remove(id: number) {
    return http.delete<ApiResponse<void>>(`/wrong-questions/${id}`)
  },
}

export const questionAdminApi = {
  page(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<QuestionItem>>>('/admin/questions', { params })
  },
  detail(id: number) {
    return http.get<ApiResponse<QuestionItem>>(`/admin/questions/${id}`)
  },
  create(payload: QuestionPayload) {
    return http.post<ApiResponse<QuestionItem>>('/admin/questions', payload)
  },
  update(id: number, payload: QuestionPayload) {
    return http.put<ApiResponse<QuestionItem>>(`/admin/questions/${id}`, payload)
  },
  setStatus(id: number, status: string) {
    return http.put<ApiResponse<QuestionItem>>(`/admin/questions/${id}/status`, { status })
  },
}

export const paperAdminApi = {
  page(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<Paper>>>('/admin/papers', { params })
  },
  detail(id: number) {
    return http.get<ApiResponse<PaperDetail>>(`/admin/papers/${id}`)
  },
  create(payload: PaperPayload) {
    return http.post<ApiResponse<PaperDetail>>('/admin/papers', payload)
  },
  update(id: number, payload: PaperPayload) {
    return http.put<ApiResponse<PaperDetail>>(`/admin/papers/${id}`, payload)
  },
  setStatus(id: number, status: string) {
    return http.put<ApiResponse<Paper>>(`/admin/papers/${id}/status`, { status })
  },
  remove(id: number) {
    return http.delete<ApiResponse<void>>(`/admin/papers/${id}`)
  },
}
