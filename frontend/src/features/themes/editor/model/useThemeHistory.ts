import { useCallback, useRef, useState } from 'react'
import type { ThemeEntry } from '../../types'

const LIMIT = 50
/** Changes closer than this share one undo step, so a slider drag is a single revert. */
const COALESCE_MS = 500

export type ThemeHistory = {
  draft: ThemeEntry
  /** Live edit; coalesced with recent edits of the same interaction. */
  set: (next: ThemeEntry) => void
  /** Replace the draft and clear history (preset pick, import, share-link load). */
  reset: (next: ThemeEntry) => void
  undo: () => void
  redo: () => void
  canUndo: boolean
  canRedo: boolean
}

/** Undo/redo ring buffer (max 50 steps) for the theme editor draft. */
export function useThemeHistory(initial: ThemeEntry): ThemeHistory {
  const [present, setPresent] = useState(initial)
  const presentRef = useRef(present)
  const past = useRef<ThemeEntry[]>([])
  const future = useRef<ThemeEntry[]>([])
  const lastEdit = useRef(0)
  const [flags, setFlags] = useState({ canUndo: false, canRedo: false })

  const commit = useCallback((next: ThemeEntry) => {
    presentRef.current = next
    setPresent(next)
    setFlags({ canUndo: past.current.length > 0, canRedo: future.current.length > 0 })
  }, [])

  const set = useCallback(
    (next: ThemeEntry) => {
      const now = Date.now()
      if (now - lastEdit.current > COALESCE_MS) {
        past.current = [...past.current, presentRef.current].slice(-LIMIT)
        future.current = []
      }
      lastEdit.current = now
      commit(next)
    },
    [commit],
  )

  const reset = useCallback(
    (next: ThemeEntry) => {
      past.current = []
      future.current = []
      lastEdit.current = 0
      commit(next)
    },
    [commit],
  )

  const undo = useCallback(() => {
    if (past.current.length === 0) return
    future.current = [presentRef.current, ...future.current].slice(0, LIMIT)
    lastEdit.current = 0
    commit(past.current.at(-1) as ThemeEntry)
    past.current = past.current.slice(0, -1)
    setFlags({ canUndo: past.current.length > 0, canRedo: true })
  }, [commit])

  const redo = useCallback(() => {
    if (future.current.length === 0) return
    past.current = [...past.current, presentRef.current].slice(-LIMIT)
    lastEdit.current = 0
    commit(future.current[0] as ThemeEntry)
    future.current = future.current.slice(1)
    setFlags({ canUndo: true, canRedo: future.current.length > 0 })
  }, [commit])

  return {
    draft: present,
    set,
    reset,
    undo,
    redo,
    canUndo: flags.canUndo,
    canRedo: flags.canRedo,
  }
}
