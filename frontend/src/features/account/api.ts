import { api } from '../../shared/api/client'
import type { TFunction } from 'i18next'

export type DeviceLink = { id: string; linked_at: string; last_seen: string | null }

const ALLOWED_TYPES = new Set(['image/png', 'image/jpeg', 'image/webp'])
const MAX_BYTES = 5 * 1024 * 1024

/** Client-side avatar checks so a bad file never reaches the server. */
export function checkAvatarFile(file: File, t: TFunction<'account'>): string | undefined {
  if (!ALLOWED_TYPES.has(file.type)) return t('validation.avatarType')
  if (file.size > MAX_BYTES) return t('validation.avatarTooBig')
  return undefined
}

export async function uploadAvatar(file: File): Promise<{ avatar_url: string }> {
  const body = new FormData()
  body.set('file', file)
  return api.request('/avatars', { method: 'POST', body })
}

export async function listDevices(): Promise<DeviceLink[]> {
  return api.request('/device/links')
}

export async function revokeDevice(id: string): Promise<void> {
  await api.request(`/device/links/${id}`, { method: 'DELETE' })
}
