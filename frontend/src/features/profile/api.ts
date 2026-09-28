import { api } from '@shared/api/client'

export type PublicProfile = {
  id: string
  display_nick: string
  avatar_url: string | null
  created_at: string
  badges: string[]
}

export function getProfile(id: string): Promise<PublicProfile> {
  return api.request(`/users/${id}`)
}
