import { CommandHero } from '../../features/landing/CommandHero'
import { HowItWorks } from '../../features/landing/HowItWorks'
import { FaqDownload } from '../../features/landing/sections/FaqDownload'
import { Hero } from '../../features/landing/sections/Hero'
import { HudPlayground } from '../../features/landing/sections/HudPlayground'
import { ModuleWall } from '../../features/landing/sections/ModuleWall'
import { ShowcaseTeaser } from '../../features/landing/sections/ShowcaseTeaser'
import { ThemeStrip } from '../../features/landing/sections/ThemeStrip'

export default function HomePage() {
  return (
    <>
      <Hero />
      <ThemeStrip />
      <HudPlayground />
      <CommandHero />
      <HowItWorks />
      <ModuleWall />
      <ShowcaseTeaser />
      <FaqDownload />
    </>
  )
}
