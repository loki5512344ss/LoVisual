import { I18n, type Lang } from '@shared/i18n/core'
import { setGlobalI18n } from '@shared/i18n/react'
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

/** Creates an i18n instance and makes it the global one. Each call is independent, so tests can run in isolation. */
export function initI18n(lng?: Lang): I18n {
  const instance = new I18n(
    {
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
    lng,
  )
  setGlobalI18n(instance)
  return instance
}
