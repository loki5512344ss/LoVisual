import type { Translation } from '../../../shared/i18n/common.ru'
import type { clickguiRu } from './ru'

export const clickguiEn = {
  windowLabel: 'Interactive replica of the LoVisual ClickGui',
  tabsLabel: 'ClickGui sections',
  tabs: { modules: 'Modules', settings: 'Settings' },
  clock: 'Time',
  search: 'Search',
  categories: 'Module categories',
  module: '{{name}}: LMB toggles it',
  theme: '{{name}} theme',
  themes: 'Themes',
  settingsNav: 'Settings sections',
  sidebar: { hud: 'HUD', ui: 'UI', themes: 'Themes', layout: 'Layout', account: 'Account', configs: 'Configs' },
  hints: {
    modules: { toggle: 'LMB - toggle module', settings: 'RMB - settings', moduleList: 'E - edit ModuleList', search: 'Ctrl+F - search', close: 'Esc - close ClickGUI', hide: 'Alt+H - hide hints' },
    settings: { pick: 'LMB - pick a theme', edit: 'Edit - open the theme editor', wheel: 'Wheel - scroll themes', close: 'Esc - close ClickGUI', hide: 'Alt+H - hide hints' },
  },
} satisfies Translation<typeof clickguiRu>
