import { MotionConfig, motion } from 'motion/react'
import { useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { ClickGuiWindow } from '../../../clickgui/ClickGuiWindow'
import { DOWNLOAD_URL } from '../../../download/api'
import { PRESETS } from '../../../themes/presets.generated'
import { parseArgb } from '../../../themes/types'
import { useActiveTheme } from '../../../themes/useActiveTheme'
import { ParticleField } from '../../effects/ParticleField'
import { useInView } from '../../effects/useInView'
import { VoxelScene } from '../../effects/VoxelScene'
import '../../styles/landing.css'

const rise = (delay: number) => ({
  initial: { opacity: 0, y: 24 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.7, delay, ease: [0.22, 1, 0.36, 1] as const },
})

function DownloadIcon() {
  return (
    <svg viewBox="0 0 20 20" className="size-5" aria-hidden="true">
      <path d="M10 3v10m0 0-4-4m4 4 4-4M4 16h12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function Hero() {
  const { t } = useTranslation('landing')
  const { theme } = useActiveTheme()
  const sectionRef = useRef<HTMLElement>(null)
  const inView = useInView(sectionRef)
  const { r, g, b } = parseArgb(theme.theme.accent)
  const accent = `rgb(${r}, ${g}, ${b})`
  const facts = [
    { n: '88', label: t('hero.facts.modules') },
    { n: '52', label: t('hero.facts.visuals') },
    { n: String(PRESETS.length), label: t('hero.facts.themes') },
  ]

  return (
    <MotionConfig reducedMotion="user">
      <section
        ref={sectionRef}
        aria-labelledby="hero-title"
        className="lv-bleed relative isolate -mt-(--topbar-h) flex min-h-svh items-center overflow-hidden pt-(--topbar-h)"
      >
        <VoxelScene accent={accent} />
        <div aria-hidden="true" className="lv-hero-shade absolute inset-0" />
        <ParticleField color={accent} />

        <div className="relative mx-auto grid w-full max-w-[1120px] items-center gap-12 px-4 pb-20 pt-12 sm:px-6 lg:py-16">
          <div className="flex min-w-0 flex-col items-start gap-6">
            <motion.span
              {...rise(0)}
              className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-black/30 px-3 py-1 font-code text-xs text-frost backdrop-blur"
            >
              <span className="lv-live-dot" aria-hidden="true" />
              {t('hero.badge')}
            </motion.span>
            <motion.h1 {...rise(0.08)} id="hero-title" className="lv-hero-title font-display font-bold">
              {t('hero.headlineLead')} <span className="lv-hero-accent">{t('hero.headlineAccent')}</span>
            </motion.h1>
            <motion.p {...rise(0.16)} className="max-w-[34rem] text-lg leading-relaxed text-snow/80">
              {t('hero.lead')}
            </motion.p>
            <motion.div {...rise(0.24)} className="flex flex-wrap items-center gap-3">
              <a href={DOWNLOAD_URL} download className="lv-cta-primary lv-cta-lg">
                <DownloadIcon />
                {t('hero.download')}
              </a>
              <a href="/register" className="lv-cta-quiet-lg">
                {t('hero.register')}
              </a>
            </motion.div>
            <motion.p {...rise(0.3)} className="-mt-2 font-code text-xs text-frost">
              {t('hero.downloadHint')}
            </motion.p>
            <motion.dl {...rise(0.36)} className="mt-2 flex flex-wrap gap-x-8 gap-y-3">
              {facts.map((f) => (
                <div key={f.label} className="flex flex-col">
                  <dt className="order-2 text-sm text-frost">{f.label}</dt>
                  <dd className="lv-fact font-display text-3xl font-bold text-snow">{f.n}</dd>
                </div>
              ))}
            </motion.dl>
          </div>

          <motion.div
            initial={{ opacity: 0, scale: 0.94, y: 30 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.9, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
            className="flex min-w-0 flex-col items-center gap-4"
          >
            <ClickGuiWindow paused={!inView} />
            <p className="flex items-center gap-2 text-sm text-frost">
              <span className="lv-live-dot" aria-hidden="true" />
              {t('hero.tryIt')}
            </p>
          </motion.div>
        </div>
      </section>
    </MotionConfig>
  )
}
