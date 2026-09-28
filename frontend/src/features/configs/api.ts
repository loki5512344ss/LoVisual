import { api } from '@shared/api/client'
import type { SlotSummary } from './types'

export function listSlots(): Promise<SlotSummary[]> {
  return api.request('/configs')
}

export function regenerateCode(slot: number): Promise<{ share_code: string }> {
  return api.request(`/configs/${slot}/regenerate-code`, { method: 'POST' })
}

export function publish(slot: number, title: string, description: string): Promise<{ listing_id: string }> {
  return api.request(`/configs/${slot}/publish`, { method: 'POST', json: { title, description } })
}

export function unpublish(slot: number): Promise<void> {
  return api.request(`/configs/${slot}/publish`, { method: 'DELETE' })
}
