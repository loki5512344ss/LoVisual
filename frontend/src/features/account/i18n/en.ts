import type { Translation } from '../../../shared/i18n/common.ru'
import type { accountRu } from './ru'

export const accountEn = {
  profile: {
    memberSince: 'With us since {{date}}',
    signOut: 'Sign out',
  },
  avatar: {
    change: 'Change avatar',
  },
  validation: {
    avatarType: 'PNG, JPG or WebP please.',
    avatarTooBig: 'The file is over 5 MB.',
  },
  devices: {
    heading: 'Linked games',
    linkedAt: 'Linked {{date}}',
    lastSeen: 'Last seen: {{when}}',
    neverSeen: 'never signed in yet',
    revoke: 'Unlink',
    confirmRevoke: 'Really unlink?',
    cancel: 'Cancel',
    empty: 'No linked games yet. Link the mod on the "Link the game" page.',
    emptyLink: 'Link the game',
  },
} satisfies Translation<typeof accountRu>
