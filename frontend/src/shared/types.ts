export type Me = {
  id: string
  email: string
  display_nick: string
  role: 'user' | 'admin'
  avatar_url: string | null
  created_at: string
}
