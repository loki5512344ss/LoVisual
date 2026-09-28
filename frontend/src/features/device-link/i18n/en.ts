import type { Translation } from '@shared/i18n/common.ru'
import type { deviceLinkRu } from './ru'

export const deviceLinkEn = {
  page: {
    title: 'Link the game',
    lead: "No password needed in the mod: it shows a short code, and you confirm it here.",
  },
  form: {
    codeLabel: 'Code from the game',
    submit: 'Link',
    success: 'Game linked. You can go back to Minecraft, the mod is already signed in.',
    expired: "Code not found or expired. Codes last 10 minutes, grab a new one in the mod.",
  },
  steps: {
    one: 'Open the ClickGui in-game and switch to the account tab.',
    two: 'The mod shows a short code, type it here.',
    three: 'Confirm, the mod signs itself into your account.',
  },
} satisfies Translation<typeof deviceLinkRu>
