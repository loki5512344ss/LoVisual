import i18next, { type i18n } from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import { accountEn } from '@features/account/i18n/en'
import { accountRu } from '@features/account/i18n/ru'
import { authEn } from '@features/auth/i18n/en'
import { authRu } from '@features/auth/i18n/ru'
import { clickguiEn } from '@features/clickgui/i18n/en'
import { configsEn } from '@features/configs/i18n/en'
import { clickguiRu } from '@features/clickgui/i18n/ru'
import { configsRu } from '@features/configs/i18n/ru'
import { deviceLinkEn } from '@features/device-link/i18n/en'
import { deviceLinkRu } from '@features/device-link/i18n/ru'
import { downloadEn } from '@features/download/i18n/en'
import { profileEn } from '@features/profile/i18n/en'
import { profileRu } from '@features/profile/i18n/ru'
import { showcaseEn } from '@features/showcase/i18n/en'
import { downloadRu } from '@features/download/i18n/ru'
import { showcaseRu } from '@features/showcase/i18n/ru'
import { landingEn } from '@features/landing/i18n/en'
import { landingRu } from '@features/landing/i18n/ru'
import { themesEn } from '@features/themes/i18n/en'
import { themesRu } from '@features/themes/i18n/ru'
import { commonEn } from '@shared/i18n/common.en'
import { commonRu } from '@shared/i18n/common.ru'

/** Creates and initializes an i18next instance. Each call is independent, so tests can run in isolation. */
export function initI18n(lng?: 'ru' | 'en'): i18n {
  const instance = i18next.createInstance()
  instance
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
      lng,
      resources: {
        ru: {
          common: commonRu,
          auth: authRu,
          landing: landingRu,
          themes: themesRu,
          clickgui: clickguiRu,
          download: downloadRu,
          deviceLink: deviceLinkRu,
          account: accountRu,
          configs: configsRu,
          showcase: showcaseRu,
          profile: profileRu,
        },
        en: {
          common: commonEn,
          auth: authEn,
          landing: landingEn,
          themes: themesEn,
          clickgui: clickguiEn,
          download: downloadEn,
          deviceLink: deviceLinkEn,
          account: accountEn,
          configs: configsEn,
          showcase: showcaseEn,
          profile: profileEn,
        },
      },
      // Browser language decides the default: only an actual `ru` locale gets Russian,
      // every other system language falls back to English.
      fallbackLng: 'en',
      supportedLngs: ['ru', 'en'],
      ns: ['common', 'auth', 'landing', 'themes', 'clickgui', 'download', 'deviceLink', 'account', 'configs', 'showcase', 'profile'],
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
