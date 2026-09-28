import type { Translation } from '@shared/i18n/common.ru'
import type { downloadRu } from './ru'

export const downloadEn = {
  nav: 'Download',
  button: 'Download for free',
  hint: 'One .jar straight from GitHub',
  versionHint: 'v{version} · {size}',
  page: {
    title: 'Download LoVisual',
    lead: 'A free visuals mod for Minecraft 26.2 on Fabric. One file: no installers, no ads.',
    reqTitle: 'What you need',
    reqMc: 'Minecraft',
    reqLoader: 'Fabric Loader',
    reqApi: 'Fabric API',
    reqOptionalTitle: 'Optional, for maximum FPS',
    reqOptional: 'Sodium and Iris: the mod does not require them but plays nicely.',
    installTitle: 'Install in three steps',
    step1: 'Download lovisual.jar with the button above.',
    step2: 'Drop the file into your mods folder (create it if it is missing).',
    step3: 'Launch Minecraft with the Fabric profile, the mod is active right away.',
  },
  notes: {
    title: "What's new",
    allReleases: 'All releases on GitHub',
    empty: 'The changelog will appear once the first public release is out.',
    downloads: '{count} downloads',
  },
} satisfies Translation<typeof downloadRu>
