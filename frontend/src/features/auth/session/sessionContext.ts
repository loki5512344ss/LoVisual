import { createContext } from 'react'
import type { Me } from '@shared/types'

export type Session = {
  status: 'loading' | 'anonymous' | 'authenticated'
  me: Me | null
  signIn(email: string, password: string): Promise<void>
  signUp(email: string, password: string, nick: string): Promise<void>
  signOut(): Promise<void>
}

export const SessionContext = createContext<Session | null>(null)
