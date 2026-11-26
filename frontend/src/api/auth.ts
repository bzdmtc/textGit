import http from './http'
import type { ApiResponse, MeResponse, TokenResponse } from '@/types'

export interface LoginPayload {
  account: string
  password: string
}

export interface SmsLoginPayload {
  phone: string
  code: string
}

export interface EmailLoginPayload {
  email: string
  code: string
}

export interface CodeSentResponse {
  expiresIn: number
  mock: boolean
}

export const authApi = {
  login(payload: LoginPayload) {
    return http.post<ApiResponse<TokenResponse>>('/auth/login', payload)
  },
  sendSmsCode(phone: string) {
    return http.post<ApiResponse<CodeSentResponse>>('/auth/sms/code', { phone })
  },
  loginBySms(payload: SmsLoginPayload) {
    return http.post<ApiResponse<TokenResponse>>('/auth/sms/login', payload)
  },
  sendEmailCode(email: string) {
    return http.post<ApiResponse<CodeSentResponse>>('/auth/email/code', { email })
  },
  loginByEmail(payload: EmailLoginPayload) {
    return http.post<ApiResponse<TokenResponse>>('/auth/email/login', payload)
  },
  me() {
    return http.get<ApiResponse<MeResponse>>('/auth/me')
  },
  logout() {
    return http.post<ApiResponse<void>>('/auth/logout')
  },
}
