import type { ReactNode } from 'react'
import { I18nContext } from './context'
import type { I18n } from './core'

/** Scopes an instance to a subtree (tests render the same page in both languages side by side). */
export function I18nProvider({ i18n, children }: { i18n: I18n; children: ReactNode }) {
  return <I18nContext value={i18n}>{children}</I18nContext>
}
