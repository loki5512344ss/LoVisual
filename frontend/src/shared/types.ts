export type Me = {
  id: string
  email: string
  display_nick: string
  role: 'user' | 'admin'
  avatar_url: string | null
  /** False until the address is confirmed; drives the verify-email banner and device linking. */
  email_verified: boolean
  created_at: string
}
