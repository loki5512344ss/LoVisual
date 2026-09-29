import { AnimatePresence, MotionConfig, motion } from 'motion/react'
import { type MouseEvent, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ButtonLink } from '@shared/ui/Button'
import { prefersReducedMotion } from '@shared/motion/reducedMotion'
import { themeToCssVars } from '@features/clickgui/themeVars'
import { PRESETS } from '@features/themes/presets.generated'
import { argbToCss, type ThemeColors } from '@features/themes/types'
import { useActiveTheme } from '@features/themes/useActiveTheme'
import '@features/landing/styles/landing.css'
import '@features/landing/styles/sections.css'

const PALETTE_KEYS: (keyof ThemeColors)[] = [
  'windowBg', 'windowHeader', 'surface', 'cardEnabled', 'accent', 'accentSoft', 'textPrimary', 'textMuted',
]

/** Where the last click landed, so the repaint wave starts under the pointer. */
type Wave = { key: number; x: number; y: number }

/**
 * The whole section is a blown-up ClickGui window painted with the active theme: header strip,
 * window gradient, accent glow. Picking a swatch repaints it (and the hero GUI, and the page
 * accent) with a wave that starts at the clicked swatch.
 */
export function ThemeStrip() {
  const { t } = useTranslation('landing')
  const { t: tt } = useTranslation('themes')
  const { theme, setTheme } = useActiveTheme()
  const sectionRef = useRef<HTMLElement>(null)
  const [wave, setWave] = useState<Wave | null>(null)

  const pick = (id: string) => (e: MouseEvent<HTMLButtonElement>) => {
    setTheme(id, 'user')
    const box = sectionRef.current?.getBoundingClientRect()
    if (!box || prefersReducedMotion()) return
    const b = e.currentTarget.getBoundingClientRect()
    setWave({ key: Date.now(), x: b.left + b.width / 2 - box.left, y: b.top + b.height / 2 - box.top })
  }

  return (
    <MotionConfig reducedMotion="user">
      <section
        ref={sectionRef}
        aria-labelledby="themes-title"
        style={themeToCssVars(theme)}
        className="lv-bleed lv-paint relative isolate overflow-hidden"
      >
        <AnimatePresence>
          {wave ? (
            <motion.span
              key={wave.key}
              aria-hidden="true"
              className="lv-paint-wave"
              style={{ left: wave.x, top: wave.y }}
              initial={{ scale: 0, opacity: 0.8 }}
              animate={{ scale: 1, opacity: [0.8, 0.6, 0] }}
              transition={{ duration: 0.9, ease: 'easeOut' }}
              onAnimationComplete={() => setWave((w) => (w?.key === wave.key ? null : w))}
            />
          ) : null}
        </AnimatePresence>

        <div className="lv-paint-header" aria-hidden="true">
          <div className="mx-auto flex h-full max-w-[1120px] items-center gap-3 px-4 sm:px-6">
            <span className="lv-swatch-dot" />
            <span className="font-display text-sm font-bold" style={{ color: 'var(--gui-text)' }}>LoVisual</span>
            <span className="text-sm" style={{ color: 'var(--gui-text-muted)' }}>/ {theme.name}</span>
          </div>
        </div>

        <div className="relative mx-auto grid max-w-[1120px] gap-10 px-4 pb-20 pt-14 sm:px-6 sm:pb-24 sm:pt-20">
          <div className="grid items-end gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] lg:gap-14">
            <div className="flex min-w-0 flex-col items-start gap-5">
              <h2 id="themes-title" className="lv-paint-title">{t('themeStrip.title')}</h2>
              <p className="max-w-[44ch] text-lg" style={{ color: 'var(--gui-text-muted)' }}>{t('themeStrip.lead')}</p>
              <ButtonLink to="/themes" className="mt-1 px-8 py-3 text-base">{t('themeStrip.cta')}</ButtonLink>
            </div>

            <div className="flex min-w-0 flex-col gap-3">
              <p className="text-sm" style={{ color: 'var(--gui-text-muted)' }}>{t('themeStrip.current')}</p>
              <div className="relative h-[clamp(3.6rem,9vw,7.5rem)] overflow-hidden" aria-live="polite">
                <AnimatePresence mode="popLayout" initial={false}>
                  <motion.p
                    key={theme.id}
                    className="lv-paint-name"
                    initial={{ y: '100%' }}
                    animate={{ y: 0 }}
                    exit={{ y: '-100%' }}
                    transition={{ duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
                  >
                    {theme.name}
                  </motion.p>
                </AnimatePresence>
              </div>
              <ul aria-label={t('themeStrip.palette')} className="grid grid-cols-8 overflow-hidden rounded-ctl ring-1 ring-white/10">
                {PALETTE_KEYS.map((k) => (
                  <li key={k} title={tt(`colors.${k}`)} className="lv-paint-chip" style={{ background: argbToCss(theme.theme[k]) }}>
                    <span className="sr-only">{`${tt(`colors.${k}`)}: ${theme.theme[k]}`}</span>
                  </li>
                ))}
              </ul>
              <p aria-hidden="true" className="hidden grid-cols-8 font-code text-[0.7rem] sm:grid" style={{ color: 'var(--gui-text-muted)' }}>
                {PALETTE_KEYS.map((k) => (
                  <span key={k} className="truncate">{theme.theme[k].slice(3)}</span>
                ))}
              </p>
            </div>
          </div>

          <ul aria-label={t('themeStrip.swatches')} className="grid min-w-0 grid-cols-3 gap-2 sm:gap-3 lg:grid-cols-6">
            {PRESETS.map((preset) => {
              const active = preset.id === theme.id
              return (
                <li key={preset.id}>
                  <button
                    type="button"
                    aria-pressed={active}
                    aria-label={tt('apply', { name: preset.name })}
                    onClick={pick(preset.id)}
                    className="lv-swatch"
                    data-active={active}
                    style={themeToCssVars(preset)}
                  >
                    <span className="lv-swatch-header" aria-hidden="true">
                      <span className="lv-swatch-dot" />
                      <span className="lv-swatch-bar" />
                    </span>
                    <span className="lv-swatch-body" aria-hidden="true">
                      <span className="lv-swatch-card" />
                      <span className="lv-swatch-card lv-swatch-card-on" />
                    </span>
                    <span className="lv-swatch-name">{preset.name}</span>
                  </button>
                </li>
              )
            })}
          </ul>
        </div>
      </section>
    </MotionConfig>
  )
}
