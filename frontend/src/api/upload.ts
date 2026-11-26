import http from './http'
import type { ApiResponse } from '@/types'

export interface StoredFile {
  url: string
  originalName: string
  size: number
}

export function uploadFile(file: File, category: string) {
  const form = new FormData()
  form.append('file', file)
  form.append('category', category)
  return http.post<ApiResponse<StoredFile>>('/admin/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
