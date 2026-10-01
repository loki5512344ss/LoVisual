import { createContext } from 'react'
import type { I18n } from './core'

export const I18nContext = createContext<I18n | null>(null)

let current: I18n | null = null

/** Registers the instance hooks fall back to when no provider is mounted (the app itself uses this). */
export function setGlobalI18n(instance: I18n): void {
  current = instance
}

export function getI18n(): I18n {
  if (!current) throw new Error('i18n is not initialised')
  return current
}
