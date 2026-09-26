import { CommandHero } from '../../features/landing/CommandHero'
import { HowItWorks } from '../../features/landing/HowItWorks'
import { FaqDownload } from '../../features/landing/sections/explore/FaqDownload'
import { Hero } from '../../features/landing/sections/hero/Hero'
import { HudPlayground } from '../../features/landing/sections/explore/HudPlayground'
import { ModuleWall } from '../../features/landing/sections/explore/ModuleWall'
import { ShowcaseTeaser } from '../../features/landing/sections/explore/ShowcaseTeaser'
import { ThemeStrip } from '../../features/landing/sections/hero/ThemeStrip'

export default function HomePage() {
  return (
    <>
      <Hero />
      <ThemeStrip />
      <div className="lv-lower">
        <div aria-hidden="true" className="lv-lower-bg" />
        <HudPlayground />
        <CommandHero />
        <HowItWorks />
        <ModuleWall />
        <ShowcaseTeaser />
        <FaqDownload />
      </div>
    </>
  )
}
