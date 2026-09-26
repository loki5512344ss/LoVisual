export type Author = { nick: string; avatar_url: string | null }

export type Listing = {
  id: string
  title: string
  description: string
  config_name: string
  copies_count: number
  published_at: string
  author: Author | null
  author_id?: string | null
}

export type ListingDetail = Listing & { data: unknown }

export type ShowcasePage = { items: Listing[]; page: number; has_more: boolean }
