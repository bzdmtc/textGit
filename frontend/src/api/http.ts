import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse, TokenResponse } from '@/types'

const baseURL = '/api/v1'

export const ACCESS_TOKEN_KEY = 'smart_learning_access_token'
export const REFRESH_TOKEN_KEY = 'smart_learning_refresh_token'

const http = axios.create({ baseURL, timeout: 15000 })

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshing: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  if (!refreshToken) return null
  if (!refreshing) {
    refreshing = axios
      .post<ApiResponse<TokenResponse>>(`${baseURL}/auth/refresh`, { refreshToken })
      .then((response) => {
        const data = response.data.data
        localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
        localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
        return data.accessToken
      })
      .catch(() => {
        localStorage.removeItem(ACCESS_TOKEN_KEY)
        localStorage.removeItem(REFRESH_TOKEN_KEY)
        return null
      })
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined
    const isAuthEndpoint = original?.url?.includes('/auth/login')
      || original?.url?.includes('/auth/refresh')
      || original?.url?.includes('/auth/sms')
      || original?.url?.includes('/auth/email')

    if (status === 401 && original && !original._retried && !isAuthEndpoint) {
      original._retried = true
      const token = await refreshAccessToken()
      if (token) {
        original.headers.Authorization = `Bearer ${token}`
        return http(original)
      }
      window.location.href = '/login'
    }

    const message = error.response?.data?.message || error.message || '请求失败'
    if (status !== 401 || isAuthEndpoint) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

export default http
