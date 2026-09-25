import type { commonRu, Translation } from './common.ru'

export const commonEn = {
  skipToContent: 'Skip to content',
  nav: {
    mainNav: 'Main navigation',
    showcase: 'Showcase',
    myConfigs: 'My configs',
    linkGame: 'Link game',
    menu: 'Menu',
    login: 'Log in',
  },
  notFound: {
    title: 'Page not found',
    body: 'This link may be outdated.',
    home: 'Go home',
  },
  shareCode: {
    copy: 'Copy',
    copied: 'Copied',
  },
  languageSwitch: {
    ru: 'RU',
    en: 'EN',
  },
  footer: {
    about: 'A free visuals mod for Minecraft 26.2 on Fabric.',
    sectionProduct: 'Product',
    sectionProject: 'Project',
    github: 'GitHub',
    download: 'Download',
    legalTitle: 'Legal',
    disclaimer: 'Not affiliated with Mojang or Microsoft.',
  },
  time: {
    days_one: '{{count}} day',
    days_few: '{{count}} days',
    days_many: '{{count}} days',
    days_other: '{{count}} days',
  },
  errors: {
    badRequest: 'Check what you entered.',
    unauthorized: 'You need to log in.',
    forbidden: 'Not enough permissions.',
    notFound: 'Not found.',
    conflict: 'Already exists.',
    rateLimited: 'Too many attempts. Wait {{seconds}}s.',
    rateLimitedGeneric: 'Too many attempts. Wait a bit.',
    serverError: "The server didn't respond. Try again in a minute.",
    network: 'No connection to the server. Check your internet and try again.',
  },
} satisfies Translation<typeof commonRu>
