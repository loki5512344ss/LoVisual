import { useContext, useMemo, useSyncExternalStore } from 'react'
import { getI18n, I18nContext } from './context'
import type { I18n, Namespace, TFunction } from './core'

export type { Lang, TFunction } from './core'
export { getI18n, setGlobalI18n } from './context'

/** Returns `t` bound to the first namespace (others via `ns:key`) plus the instance; re-renders on language change. */
export function useTranslation<N extends Namespace = 'common'>(ns: N | readonly N[] = 'common' as N): { t: TFunction<N>; i18n: I18n } {
  const i18n = useContext(I18nContext) ?? getI18n()
  const language = useSyncExternalStore(i18n.subscribe, () => i18n.language)
  const first = Array.isArray(ns) ? ns[0] : ns
  const t = useMemo(() => i18n.getFixedT(language, first as N), [i18n, language, first])
  return { t: t as TFunction<N>, i18n }
}
