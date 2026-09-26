import { api } from '../../shared/api/client'
import type { ListingDetail, ShowcasePage } from './types'

export function browse(sort: 'new' | 'popular', page: number, author?: string): Promise<ShowcasePage> {
  const params = new URLSearchParams({ sort, page: String(page) })
  if (author) params.set('author', author)
  return api.request(`/showcase?${params.toString()}`)
}

export function getListing(id: string): Promise<ListingDetail> {
  return api.request(`/showcase/${id}`)
}

export function copyToSlot(id: string, slot: number): Promise<unknown> {
  return api.request(`/showcase/${id}/copy`, { method: 'POST', json: { slot } })
}
