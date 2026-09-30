import { CommandHero } from '@features/landing/CommandHero'
import { HowItWorks } from '@features/landing/HowItWorks'
import { FaqDownload } from '@features/landing/sections/explore/FaqDownload'
import { HudPlayground } from '@features/landing/sections/explore/HudPlayground'
import { ModuleWall } from '@features/landing/sections/explore/ModuleWall'
import { ShowcaseTeaser } from '@features/landing/sections/explore/ShowcaseTeaser'

/** Everything below the first screen; loaded as its own chunk after the hero has painted. */
export default function LowerSections() {
  return (
    <>
      <HudPlayground />
      <CommandHero />
      <HowItWorks />
      <ModuleWall />
      <ShowcaseTeaser />
      <FaqDownload />
    </>
  )
}
