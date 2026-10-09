import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useMemo, type ReactNode } from 'react'
import { api } from '@shared/api/client'
import { fetchMe, login, logout, register } from '../api'
import { SessionContext, type Session } from './sessionContext'
import { hasSessionHint, setSessionHint } from './sessionHint'

export function SessionProvider({ children }: { children: ReactNode }) {
  const client = useQueryClient()
  // Restores the session after a reload: the refresh cookie survives, the
  // in-memory access token does not. Guests skip the call (see sessionHint).
  const restored = useQuery({
    queryKey: ['session-restore'],
    queryFn: async () => {
      if (!hasSessionHint()) return false
      const ok = await api.refresh()
      setSessionHint(ok)
      return ok
    },
    staleTime: Infinity,
  })
  const meQuery = useQuery({ queryKey: ['me'], queryFn: fetchMe, enabled: restored.data === true })

  const signIn = useCallback(
    async (email: string, password: string) => {
      await login(email, password)
      setSessionHint(true)
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
    setSessionHint(false)
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
