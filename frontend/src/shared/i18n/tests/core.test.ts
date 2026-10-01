import { afterEach, expect, test, vi } from 'vitest'
import { I18n } from '../core'

const bundles = {
  ru: { common: { hi: 'Привет, {{name}}', only_ru: 'только ру', n_one: '{{count}} шт', n_other: '{{count}} шт.' } },
  en: { common: { hi: 'Hi, {{name}}', n_one: '{{count}} item', n_other: '{{count}} items' } },
} as never

afterEach(() => {
  localStorage.clear()
  vi.unstubAllGlobals()
})

test('interpolates variables and picks plural forms', () => {
  const i18n = new I18n(bundles, 'en')
  const t = i18n.getFixedT('en', 'common' as never) as (k: string, o?: object) => string
  expect(t('hi', { name: 'Loki' })).toBe('Hi, Loki')
  expect(t('n', { count: 1 })).toBe('1 item')
  expect(t('n', { count: 4 })).toBe('4 items')
})

test('missing Russian keys fall back to English, unknown keys echo the key', () => {
  const i18n = new I18n(bundles, 'ru')
  const t = i18n.getFixedT('ru', 'common' as never) as (k: string) => string
  expect(t('only_ru')).toBe('только ру')
  expect(t('nope')).toBe('nope')
})

test('the saved language wins over the browser, and only ru browsers default to Russian', () => {
  vi.stubGlobal('navigator', { language: 'ru-RU' })
  expect(new I18n(bundles).language).toBe('ru')
  localStorage.setItem('lv_lang', 'en')
  expect(new I18n(bundles).language).toBe('en')
  localStorage.clear()
  vi.stubGlobal('navigator', { language: 'de-DE' })
  expect(new I18n(bundles).language).toBe('en')
})

test('changeLanguage persists the choice and notifies subscribers', async () => {
  const i18n = new I18n(bundles, 'en')
  const seen = vi.fn()
  const off = i18n.subscribe(seen)
  await i18n.changeLanguage('ru')
  expect(seen).toHaveBeenCalledOnce()
  expect(localStorage.getItem('lv_lang')).toBe('ru')
  expect(document.documentElement.lang).toBe('ru')
  off()
})
