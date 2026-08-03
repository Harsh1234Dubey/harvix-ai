import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ApiErrorBody, ApiResponse } from '@/lib/types'

const TOKEN_KEY = 'iva_access'
const REFRESH_KEY = 'iva_refresh'
const USER_KEY = 'iva_user'

export const tokenStore = {
  getAccess: () => localStorage.getItem(TOKEN_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  setTokens(access: string, refresh: string) {
    localStorage.setItem(TOKEN_KEY, access)
    localStorage.setItem(REFRESH_KEY, refresh)
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(USER_KEY)
  },
  setUser(user: unknown) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  },
  getUser(): unknown {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw)
    } catch {
      return null
    }
  },
}

export const client = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
})

client.interceptors.request.use((config) => {
  const token = tokenStore.getAccess()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshing: Promise<boolean> | null = null

async function doRefresh(): Promise<boolean> {
  const refresh = tokenStore.getRefresh()
  if (!refresh) return false
  try {
    const res = await axios.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
      '/api/v1/auth/refresh',
      { refreshToken: refresh },
    )
    tokenStore.setTokens(res.data.data.accessToken, res.data.data.refreshToken)
    return true
  } catch {
    tokenStore.clear()
    return false
  }
}

client.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as (AxiosRequestConfig & { _retried?: boolean }) | undefined
    if (error.response?.status === 401 && original && !original._retried) {
      original._retried = true
      refreshing = refreshing ?? doRefresh()
      const ok = await refreshing
      refreshing = null
      if (ok) {
        const access = tokenStore.getAccess()
        if (access) {
          original.headers = { ...original.headers, Authorization: `Bearer ${access}` }
        }
        return client(original)
      }
    }
    return Promise.reject(error)
  },
)

export function errorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const body = err.response?.data as ApiErrorBody | undefined
    if (body?.fieldErrors?.length) {
      return body.fieldErrors.map((f) => `${f.field}: ${f.message}`).join('; ')
    }
    if (body?.message) return body.message
    if (body?.error) return body.error
    return err.message
  }
  return err instanceof Error ? err.message : 'Unexpected error'
}

export async function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await client.get<ApiResponse<T>>(url, config)
  return res.data.data
}

export async function post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await client.post<ApiResponse<T>>(url, body, config)
  return res.data.data
}

export async function put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await client.put<ApiResponse<T>>(url, body, config)
  return res.data.data
}

export async function patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await client.patch<ApiResponse<T>>(url, body, config)
  return res.data.data
}

export async function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await client.delete<ApiResponse<T>>(url, config)
  return res.data.data
}

export function apiUrl(path?: string): string {
  if (!path) return ''
  if (path.startsWith('http') || path.startsWith('/')) return path
  return `/uploads/${path}`
}

export async function download(url: string, filename: string) {
  const res = await client.get(url, { responseType: 'blob' })
  const blobUrl = window.URL.createObjectURL(res.data as Blob)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(blobUrl)
}
