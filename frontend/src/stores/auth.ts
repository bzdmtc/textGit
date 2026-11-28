import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from '@/api/http'
import type { MeResponse, TokenResponse } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem(ACCESS_TOKEN_KEY) || '')
  const refreshToken = ref(localStorage.getItem(REFRESH_TOKEN_KEY) || '')
  const me = ref<MeResponse | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(accessToken.value))
  const rights = computed(() => me.value?.rights ?? {})
  const menus = computed(() => me.value?.menus ?? [])
  const displayName = computed(() => me.value?.user.realname || me.value?.user.nickname || me.value?.user.account || '')

  function hasPriv(module: string, method: string) {
    return Boolean(rights.value[module]?.includes(method))
  }

  function setSession(data: TokenResponse) {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    me.value = data.me
    localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
  }

  function clearSession() {
    accessToken.value = ''
    refreshToken.value = ''
    me.value = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  async function fetchMe() {
    if (!accessToken.value) return null
    const response = await authApi.me()
    me.value = response.data.data
    return me.value
  }

  async function loginByAccount(account: string, password: string, captcha?: { id: string; code: string }) {
    loading.value = true
    try {
      const response = await authApi.login({
        account,
        password,
        captchaId: captcha?.id,
        captchaCode: captcha?.code,
      })
      setSession(response.data.data)
    } finally {
      loading.value = false
    }
  }

  async function loginBySms(phone: string, code: string, captcha?: { id: string; code: string }) {
    loading.value = true
    try {
      const response = await authApi.loginBySms({
        phone,
        code,
        captchaId: captcha?.id,
        captchaCode: captcha?.code,
      })
      setSession(response.data.data)
    } finally {
      loading.value = false
    }
  }

  async function loginByEmail(email: string, code: string, captcha?: { id: string; code: string }) {
    loading.value = true
    try {
      const response = await authApi.loginByEmail({
        email,
        code,
        captchaId: captcha?.id,
        captchaCode: captcha?.code,
      })
      setSession(response.data.data)
    } finally {
      loading.value = false
    }
  }

  async function register(payload: {
    target: string
    code: string
    password?: string
    nickname?: string
  }) {
    loading.value = true
    try {
      const response = await authApi.register(payload)
      setSession(response.data.data)
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      if (accessToken.value) {
        await authApi.logout()
      }
    } finally {
      clearSession()
    }
  }

  return {
    accessToken,
    refreshToken,
    me,
    loading,
    isLoggedIn,
    rights,
    menus,
    displayName,
    hasPriv,
    setSession,
    clearSession,
    fetchMe,
    loginByAccount,
    loginBySms,
    loginByEmail,
    register,
    logout,
  }
})
