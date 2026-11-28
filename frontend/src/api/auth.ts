import http from './http'
import type { ApiResponse, MeResponse, TokenResponse } from '@/types'

export interface LoginPayload {
  account: string
  password: string
  captchaId?: string
  captchaCode?: string
}

export interface SmsLoginPayload {
  phone: string
  code: string
  captchaId?: string
  captchaCode?: string
}

export interface EmailLoginPayload {
  email: string
  code: string
  captchaId?: string
  captchaCode?: string
}

export interface RegisterPayload {
  target: string
  code: string
  password?: string
  nickname?: string
}

export interface CaptchaData {
  id: string
  image: string
}

export interface CodeSentResponse {
  expiresIn: number
  mock: boolean
}

export const authApi = {
  captcha() {
    return http.get<ApiResponse<CaptchaData>>('/auth/captcha')
  },
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
  register(payload: RegisterPayload) {
    return http.post<ApiResponse<TokenResponse>>('/auth/register', payload)
  },
  me() {
    return http.get<ApiResponse<MeResponse>>('/auth/me')
  },
  logout() {
    return http.post<ApiResponse<void>>('/auth/logout')
  },
}
