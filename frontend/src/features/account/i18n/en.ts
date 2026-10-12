import type { Translation } from '@shared/i18n/common.ru'
import type { accountRu } from './ru'

export const accountEn = {
  profile: {
    memberSince: 'With us since {{date}}',
    signOut: 'Sign out',
  },
  avatar: {
    change: 'Change avatar',
    recrop: 'Reposition',
    uploaded: 'Avatar updated',
    fetchFailed: 'Could not load the current avatar.',
    historyHint: 'Recent on this device',
    useHistory: 'Restore this avatar',
    reposition: 'Drag to reposition',
    zoom: 'Zoom',
    confirm: 'Confirm',
    cancel: 'Cancel',
    remove: 'Remove avatar',
    removeConfirm: 'Really remove?',
    removed: 'Avatar removed',
    removeFailed: 'Could not remove the avatar.',
    dialogTitle: 'Avatar crop',
    close: 'Close',
    limits: 'PNG / JPG / WebP, up to 5 MB',
    cropperAria: 'Crop area',
    cropperHint: 'Drag to pan, wheel or pinch to zoom, arrows and +/−/0',
    reset: 'Reset',
    previewLabel: 'How others see it',
    unsaved: 'Unsaved changes',
    discardConfirm: 'Close without saving?',
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
