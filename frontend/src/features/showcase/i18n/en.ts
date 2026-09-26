export const showcaseEn = {
  page: {
    title: 'Showcase',
    lead: 'Configs shared by other players. Like one? Copy it into your own slot.',
    sortNew: 'Newest',
    sortPopular: 'Popular',
    back: 'Back',
    next: 'Next',
    empty: 'The showcase is empty for now. Publish your config on the My configs page.',
    error: 'The showcase is unavailable. Try refreshing the page later.',
  },
  item: {
    authorHidden: 'Author hidden',
    copiesOne: 'Copied {{count}} time',
    copiesFew: 'Copied {{count}} times',
    copiesMany: 'Copied {{count}} times',
  },
  detail: {
    back: 'Back to showcase',
    notFound: 'This config does not exist or is hidden.',
  },
  copy: {
    heading: 'Copy to my slot',
    slot: 'Slot {{slot}}',
    occupied: 'Occupied',
    loginPrompt: 'Log in to copy this config to your account',
    login: 'Log in',
    success: 'Copied to slot {{slot}}',
    openConfigs: 'Open my configs',
    conflict: 'That slot is already taken. Pick a free one or clear a slot in the mod.',
  },
} as const
