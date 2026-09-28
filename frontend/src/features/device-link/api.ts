import { api } from '@shared/api/client'

/** Uppercases, strips spaces, and inserts the dash after 4 chars if missing (`abcd1234` -> `ABCD-1234`). */
export function normalizeUserCode(raw: string): string {
  const cleaned = raw.trim().toUpperCase().replace(/\s+/g, '').replace(/-/g, '')
  if (cleaned.length <= 4) return cleaned
  return `${cleaned.slice(0, 4)}-${cleaned.slice(4, 8)}`
}

/** Confirms a device code the mod displayed, linking that game session to the signed-in account. */
export async function confirmDevice(userCode: string): Promise<void> {
  await api.request('/device/confirm', { method: 'POST', json: { user_code: userCode } })
}
