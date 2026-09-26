import type { accountRu } from '../features/account/i18n/ru'
import type { authRu } from '../features/auth/i18n/ru'
import type { clickguiRu } from '../features/clickgui/i18n/ru'
import type { configsRu } from '../features/configs/i18n/ru'
import type { deviceLinkRu } from '../features/device-link/i18n/ru'
import type { downloadRu } from '../features/download/i18n/ru'
import type { landingRu } from '../features/landing/i18n/ru'
import type { profileRu } from '../features/profile/i18n/ru'
import type { showcaseRu } from '../features/showcase/i18n/ru'
import type { themesRu } from '../features/themes/i18n/ru'
import type { commonRu } from '../shared/i18n/common.ru'

declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'common'
    resources: {
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
      // Later tasks extend this with their own feature namespaces.
    }
  }
}
