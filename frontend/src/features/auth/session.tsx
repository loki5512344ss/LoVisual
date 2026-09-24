import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useMemo, type ReactNode } from 'react'
import { api } from '../../shared/api/client'
import type { Me } from '../../shared/types'
import { fetchMe, login, logout, register } from './api'

type Session = {
  status: 'loading' | 'anonymous' | 'authenticated'
  me: Me | null
  signIn(email: string, password: string): Promise<void>
  signUp(email: string, password: string, nick: string): Promise<void>
  signOut(): Promise<void>
}

const SessionContext = createContext<Session | null>(null)

export function SessionProvider({ children }: { children: ReactNode }) {
  const client = useQueryClient()
  // Restores the session after a reload: the refresh cookie survives, the
  // in-memory access token does not.
  const restored = useQuery({ queryKey: ['session-restore'], queryFn: () => api.refresh(), staleTime: Infinity })
  const meQuery = useQuery({ queryKey: ['me'], queryFn: fetchMe, enabled: restored.data === true })

  const signIn = useCallback(
    async (email: string, password: string) => {
      await login(email, password)
      client.setQueryData(['session-restore'], true)
      await client.fetchQuery({ queryKey: ['me'], queryFn: fetchMe })
    },
    [client],
  )

  const signUp = useCallback(
    async (email: string, password: string, nick: string) => {
      await register(email, password, nick)
      await signIn(email, password)
    },
    [signIn],
  )

  const signOut = useCallback(async () => {
    await logout()
    client.clear()
    client.setQueryData(['session-restore'], false)
  }, [client])

  const value = useMemo<Session>(() => {
    const me = meQuery.data ?? null
    const status = restored.isPending || (restored.data && meQuery.isPending) ? 'loading' : me ? 'authenticated' : 'anonymous'
    return { status, me, signIn, signUp, signOut }
  }, [restored.isPending, restored.data, meQuery.isPending, meQuery.data, signIn, signUp, signOut])

  return <SessionContext value={value}>{children}</SessionContext>
}

export function useSession(): Session {
  const session = useContext(SessionContext)
  if (!session) throw new Error('useSession outside SessionProvider')
  return session
}
