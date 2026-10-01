/**
 * Tiny i18n core (replaces i18next): namespaced dictionaries, `{{var}}` interpolation,
 * CLDR plural suffixes (`key_one` / `key_few` / `key_other`), fallback to English.
 * Typed keys are derived from the Russian dictionaries (English must match them).
 */

import type { accountRu } from '@features/account/i18n/ru'
import type { authRu } from '@features/auth/i18n/ru'
import type { clickguiRu } from '@features/clickgui/i18n/ru'
import type { configsRu } from '@features/configs/i18n/ru'
import type { deviceLinkRu } from '@features/device-link/i18n/ru'
import type { downloadRu } from '@features/download/i18n/ru'
import type { landingRu } from '@features/landing/i18n/ru'
import type { profileRu } from '@features/profile/i18n/ru'
import type { showcaseRu } from '@features/showcase/i18n/ru'
import type { themesRu } from '@features/themes/i18n/ru'
import type { commonRu } from '@shared/i18n/common.ru'

export type Lang = 'ru' | 'en'

export interface Resources {
  common: typeof commonRu
  auth: typeof authRu
  landing: typeof landingRu
  themes: typeof themesRu
  clickgui: typeof clickguiRu
  download: typeof downloadRu
  deviceLink: typeof deviceLinkRu
  account: typeof accountRu
  configs: typeof configsRu
  showcase: typeof showcaseRu
  profile: typeof profileRu
}

export type Namespace = keyof Resources & string

type PluralSuffix = 'zero' | 'one' | 'two' | 'few' | 'many' | 'other'
type Paths<T> = T extends string
  ? never
  : { [K in keyof T & string]: T[K] extends string ? (K extends `${infer B}_${PluralSuffix}` ? B : K) : `${K}.${Paths<T[K]>}` }[keyof T & string]

export type KeyOf<N extends Namespace> = Paths<Resources[N]>
type Qualified = { [N in Namespace]: `${N}:${KeyOf<N>}` }[Namespace]

export type TOptions = { count?: number } & Record<string, string | number | undefined>
export type TFunction<N extends Namespace = 'common'> = (key: KeyOf<N> | Qualified, options?: TOptions) => string

type Dict = { [k: string]: string | Dict }
type Bundles = Record<Lang, Record<string, Dict>>

const STORAGE_KEY = 'lv_lang'
const listeners = new Set<() => void>()

function lookup(dict: Dict | undefined, path: string): string | undefined {
  let node: string | Dict | undefined = dict
  for (const part of path.split('.')) {
    if (node === undefined || typeof node === 'string') return undefined
    node = node[part]
  }
  return typeof node === 'string' ? node : undefined
}

/** Only an actual `ru` locale gets Russian; every other language falls back to English. */
export function detectLanguage(): Lang {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved === 'ru' || saved === 'en') return saved
  } catch {
    // Storage blocked: fall through to the browser language.
  }
  return typeof navigator !== 'undefined' && navigator.language?.toLowerCase().startsWith('ru') ? 'ru' : 'en'
}

export class I18n {
  language: Lang
  private readonly bundles: Bundles

  constructor(bundles: Bundles, lng?: Lang) {
    this.bundles = bundles
    this.language = lng ?? detectLanguage()
    document.documentElement.lang = this.language
  }

  private resolve(lng: Lang, ns: string, key: string, count?: number): string | undefined {
    const dict = this.bundles[lng][ns]
    if (count !== undefined) {
      const plural = lookup(dict, `${key}_${new Intl.PluralRules(lng).select(count)}`) ?? lookup(dict, `${key}_other`)
      if (plural !== undefined) return plural
    }
    return lookup(dict, key)
  }

  getFixedT<N extends Namespace = 'common'>(lng: Lang, ns: N | readonly N[]): TFunction<N> {
    const first = (Array.isArray(ns) ? ns[0] : ns) as string
    return ((rawKey: string, options: TOptions = {}) => {
      const colon = rawKey.indexOf(':')
      const [space, key] = colon === -1 ? [first, rawKey] : [rawKey.slice(0, colon), rawKey.slice(colon + 1)]
      const text = this.resolve(lng, space, key, options.count) ?? this.resolve('en', space, key, options.count)
      if (text === undefined) return key
      return text.replace(/\{\{\s*(\w+)\s*\}\}/g, (_, name: string) => String(options[name] ?? ''))
    }) as TFunction<N>
  }

  t: TFunction<'common'> = (key, options) => this.getFixedT(this.language, 'common')(key, options)

  changeLanguage(lng: Lang): Promise<void> {
    if (lng !== this.language) {
      this.language = lng
      document.documentElement.lang = lng
      try {
        localStorage.setItem(STORAGE_KEY, lng)
      } catch {
        // Private mode: the choice just lasts for this tab.
      }
      for (const notify of listeners) notify()
    }
    return Promise.resolve()
  }

  subscribe = (notify: () => void): (() => void) => {
    listeners.add(notify)
    return () => listeners.delete(notify)
  }
}
