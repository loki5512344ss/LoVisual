import i18next, { type i18n } from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import { authEn } from '../features/auth/i18n/en'
import { authRu } from '../features/auth/i18n/ru'
import { clickguiEn } from '../features/clickgui/i18n/en'
import { clickguiRu } from '../features/clickgui/i18n/ru'
import { downloadEn } from '../features/download/i18n/en'
import { downloadRu } from '../features/download/i18n/ru'
import { landingEn } from '../features/landing/i18n/en'
import { landingRu } from '../features/landing/i18n/ru'
import { themesEn } from '../features/themes/i18n/en'
import { themesRu } from '../features/themes/i18n/ru'
import { commonEn } from '../shared/i18n/common.en'
import { commonRu } from '../shared/i18n/common.ru'

/** Creates and initializes an i18next instance. Each call is independent, so tests can run in isolation. */
export function initI18n(lng?: 'ru' | 'en'): i18n {
  const instance = i18next.createInstance()
  instance
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
      lng,
      resources: {
        ru: { common: commonRu, auth: authRu, landing: landingRu, themes: themesRu, clickgui: clickguiRu, download: downloadRu },
        en: { common: commonEn, auth: authEn, landing: landingEn, themes: themesEn, clickgui: clickguiEn, download: downloadEn },
      },
      fallbackLng: 'ru',
      supportedLngs: ['ru', 'en'],
      ns: ['common', 'auth', 'landing', 'themes', 'clickgui', 'download'],
      defaultNS: 'common',
      interpolation: { escapeValue: false },
      detection: { order: ['localStorage', 'navigator'], caches: ['localStorage'], lookupLocalStorage: 'lv_lang' },
    })

  document.documentElement.lang = instance.language
  instance.on('languageChanged', (next) => {
    document.documentElement.lang = next
  })

  return instance
}
