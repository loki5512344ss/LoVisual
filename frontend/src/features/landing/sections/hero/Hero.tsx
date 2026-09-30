import { lazy, Suspense, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { ClickGuiWindow } from '@features/clickgui/ClickGuiWindow'
import { PRESETS } from '@features/themes/presets.generated'
import { parseArgb } from '@features/themes/types'
import { useActiveTheme } from '@features/themes/useActiveTheme'
import { ParticleField } from '../../effects/ParticleField'
import { useInView } from '../../effects/useInView'
import { useIdleReady } from '../../effects/useIdleReady'
import '@shared/motion/motion.css'
import '@features/landing/styles/landing.css'

// The backdrop is decorative and its first draw is heavy, so it loads and paints after the headline.
const VoxelScene = lazy(() => import('../../effects/VoxelScene').then((m) => ({ default: m.VoxelScene })))

/** Entrance: the `lv-rise` keyframes, staggered by `delay` seconds. */
const rise = (delay: number) => ({ ['--d' as string]: `${delay}s` })

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
  const backdropReady = useIdleReady(300)
  const { r, g, b } = parseArgb(theme.theme.accent)
  const accent = `rgb(${r}, ${g}, ${b})`
  const facts = [
    { n: '86', label: t('hero.facts.modules') },
    { n: '50', label: t('hero.facts.visuals') },
    { n: String(PRESETS.length), label: t('hero.facts.themes') },
  ]

  return (
    <>
      <section
        ref={sectionRef}
        aria-labelledby="hero-title"
        className="lv-bleed relative isolate -mt-(--topbar-h) flex min-h-svh items-center overflow-hidden pt-(--topbar-h)"
      >
        {backdropReady && (
          <Suspense fallback={null}>
            <VoxelScene accent={accent} />
          </Suspense>
        )}
        <div aria-hidden="true" className="lv-hero-shade absolute inset-0" />
        <ParticleField color={accent} />

        <div className="relative mx-auto grid w-full max-w-[1120px] items-center gap-12 px-4 pb-20 pt-12 sm:px-6 lg:py-16">
          <div className="flex min-w-0 flex-col items-start gap-6">
            <span style={rise(0)} className="lv-rise inline-flex items-center gap-2 rounded-full border border-white/10 bg-black/30 px-3 py-1 font-code text-xs text-frost backdrop-blur"
            >
              <span className="lv-live-dot" aria-hidden="true" />
              {t('hero.badge')}
            </span>
            <h1 style={rise(0.08)} id="hero-title" className="lv-rise lv-hero-title font-display font-bold">
              {t('hero.headlineLead')} <span className="lv-hero-accent">{t('hero.headlineAccent')}</span>
            </h1>
            <p style={rise(0.16)} className="lv-rise max-w-[34rem] text-lg leading-relaxed text-snow/80">
              {t('hero.lead')}
            </p>
            <div style={rise(0.24)} className="lv-rise flex flex-wrap items-center gap-3">
              <Link to="/download" className="lv-cta-primary lv-cta-lg">
                <DownloadIcon />
                {t('hero.download')}
              </Link>
              <a href="/register" className="lv-cta-quiet-lg">
                {t('hero.register')}
              </a>
            </div>
            <p style={rise(0.3)} className="lv-rise -mt-2 font-code text-xs text-frost">
              {t('hero.downloadHint')}
            </p>
            <dl style={rise(0.36)} className="lv-rise mt-2 flex flex-wrap gap-x-8 gap-y-3">
              {facts.map((f) => (
                <div key={f.label} className="flex flex-col">
                  <dt className="order-2 text-sm text-frost">{f.label}</dt>
                  <dd className="lv-fact font-display text-3xl font-bold text-snow">{f.n}</dd>
                </div>
              ))}
            </dl>
          </div>

          <div className="lv-rise-panel flex min-w-0 flex-col items-center gap-4">
            <ClickGuiWindow paused={!inView} />
            <p className="flex items-center gap-2 text-sm text-frost">
              <span className="lv-live-dot" aria-hidden="true" />
              {t('hero.tryIt')}
            </p>
          </div>
        </div>
      </section>
    </>
  )
}
