import type { authRu } from '../features/auth/i18n/ru'
import type { clickguiRu } from '../features/clickgui/i18n/ru'
import type { downloadRu } from '../features/download/i18n/ru'
import type { landingRu } from '../features/landing/i18n/ru'
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
      // Later tasks extend this with their own feature namespaces.
    }
  }
}
