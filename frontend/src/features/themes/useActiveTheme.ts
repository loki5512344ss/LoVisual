import { createContext, createElement, type ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { PRESETS } from './presets.generated'
import { parseArgb, type ThemeEntry, withAlpha } from './types'

const STORAGE_KEY = 'lv_theme'
const FALLBACK = PRESETS[0]

type ActiveTheme = {
  theme: ThemeEntry
  /** True once the visitor picked a theme themselves — demos must stop switching it then. */
  userPicked: boolean
  /** `source: 'user'` persists the choice; `'demo'` is a transient showcase switch. */
  setTheme: (id: string, source?: 'user' | 'demo') => void
}

const ActiveThemeContext = createContext<ActiveTheme | null>(null)

function readStored(): string | null {
  try {
    return window.localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

function store(id: string) {
  try {
    window.localStorage.setItem(STORAGE_KEY, id)
  } catch {
    // Storage can be blocked (private mode, quota); the theme still applies for this visit.
  }
}

const findPreset = (id: string | null) => PRESETS.find((p) => p.id === id)

/** Opaque accent for page chrome (buttons, links, focus rings). */
function pageAccent(entry: ThemeEntry): string {
  const { r, g, b } = parseArgb(entry.theme.accent)
  return `rgb(${r}, ${g}, ${b})`
}

export function ActiveThemeProvider({ children, applyToPage = true }: { children: ReactNode; applyToPage?: boolean }) {
  const [state, setState] = useState(() => {
    const stored = findPreset(readStored())
    return { theme: stored ?? FALLBACK, userPicked: Boolean(stored) }
  })

  const setTheme = useCallback((id: string, source: 'user' | 'demo' = 'user') => {
    const next = findPreset(id)
    if (!next) return
    if (source === 'user') store(id)
    setState((prev) => {
      if (source === 'demo' && prev.userPicked) return prev
      return { theme: next, userPicked: prev.userPicked || source === 'user' }
    })
  }, [])

  useEffect(() => {
    if (!applyToPage) return
    const root = document.documentElement.style
    root.setProperty('--color-ice', pageAccent(state.theme))
    root.setProperty('--page-accent-glow', withAlpha(state.theme.theme.accent, 0.35))
  }, [state.theme, applyToPage])

  const value = useMemo(() => ({ ...state, setTheme }), [state, setTheme])
  return createElement(ActiveThemeContext.Provider, { value }, children)
}

/** The active ClickGui theme. Outside a provider it falls back to Classic with a no-op setter. */
export function useActiveTheme(): ActiveTheme {
  return useContext(ActiveThemeContext) ?? { theme: FALLBACK, userPicked: false, setTheme: () => {} }
}
