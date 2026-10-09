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

/** Asks for a password reset email. The answer never reveals whether the address is registered. */
export const forgotPassword = (email: string) =>
  api.request('/auth/forgot-password', { method: 'POST', json: { email } })

/** Consumes a reset token and sets the new password. */
export const resetPassword = (token: string, password: string) =>
  api.request('/auth/reset-password', { method: 'POST', json: { token, new_password: password } })

/** Consumes a verification token from the email link. */
export const verifyEmail = (token: string) =>
  api.request('/auth/verify-email', { method: 'POST', json: { token } })

/** Sends the verification email again for the signed-in account. */
export const resendVerification = () =>
  api.request('/auth/resend-verification', { method: 'POST' })
