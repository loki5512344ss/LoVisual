import { api } from '@shared/api/client'
import type { Me } from '@shared/types'

export async function login(email: string, password: string): Promise<void> {
  const res = await api.request<{ access_token: string }>('/auth/login', { method: 'POST', json: { email, password } })
  api.setAccessToken(res.access_token)
}

export async function register(email: string, password: string, nick: string): Promise<void> {
  await api.request('/auth/register', { method: 'POST', json: { email, password, nick } })
}

export async function logout(): Promise<void> {
  try {
    await api.request('/auth/logout', { method: 'POST' })
  } finally {
    api.setAccessToken(null)
  }
}

export const fetchMe = () => api.request<Me>('/me')
