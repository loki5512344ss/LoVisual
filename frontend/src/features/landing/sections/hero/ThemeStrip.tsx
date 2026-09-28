import { motion } from 'motion/react'
import { useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { ButtonLink } from '../../../../shared/ui/Button'
import { themeToCssVars } from '../../../clickgui/themeVars'
import { PRESETS } from '../../../themes/presets.generated'
import { argbToCss, type ThemeColors } from '../../../themes/types'
import { useActiveTheme } from '../../../themes/useActiveTheme'
import { useInView } from '../../effects/useInView'
import '../../styles/landing.css'

const PALETTE_KEYS: (keyof ThemeColors)[] = [
  'windowBg', 'windowHeader', 'surface', 'cardEnabled', 'accent', 'accentSoft', 'textPrimary', 'textMuted',
]

/** Every built-in preset as a swatch; picking one re-themes the hero ClickGui and the page accent. */
export function ThemeStrip() {
  const { t } = useTranslation('landing')
  const { t: tt } = useTranslation('themes')
  const { theme, setTheme } = useActiveTheme()
  const listRef = useRef<HTMLUListElement>(null)
  const inView = useInView(listRef, { once: true, rootMargin: '-40px' })

  return (
    <section aria-labelledby="themes-title" className="relative grid gap-10 py-20 sm:py-28 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.3fr)] lg:gap-14">
      <div className="flex min-w-0 flex-col items-start gap-5">
        <span className="font-code text-xs uppercase tracking-[0.2em] text-ice">{t('themeStrip.eyebrow')}</span>
        <h2 id="themes-title" className="text-[2rem] leading-[1.1] sm:text-[2.6rem]">
          {t('themeStrip.title')}
        </h2>
        <p className="max-w-[40ch] text-lg text-frost">{t('themeStrip.lead')}</p>

        <div className="lv-theme-now mt-2 w-full max-w-sm rounded-panel p-4" style={themeToCssVars(theme)}>
          <div className="flex items-baseline justify-between gap-3">
            <span className="text-sm text-frost">{t('themeStrip.current')}</span>
            <span className="font-display text-lg font-bold" style={{ color: 'var(--gui-accent)' }}>
              {theme.name}
            </span>
          </div>
          <ul aria-label={t('themeStrip.palette')} className="mt-3 grid grid-cols-8 gap-1.5">
            {PALETTE_KEYS.map((k) => (
              <li
                key={k}
                title={tt(`colors.${k}`)}
                className="lv-chip aspect-square rounded-md"
                style={{ background: argbToCss(theme.theme[k]) }}
              >
                <span className="sr-only">{`${tt(`colors.${k}`)}: ${theme.theme[k]}`}</span>
              </li>
            ))}
          </ul>
        </div>

        <ButtonLink to="/themes" className="mt-2 px-8 py-3 text-base">
          {t('themeStrip.cta')}
        </ButtonLink>
      </div>

      <ul ref={listRef} aria-label={t('themeStrip.swatches')} className="grid min-w-0 grid-cols-2 gap-3 sm:grid-cols-3">
        {PRESETS.map((preset, i) => {
          const vars = themeToCssVars(preset)
          const active = preset.id === theme.id
          return (
            <motion.li
              key={preset.id}
              initial={{ opacity: 0, y: 16 }}
              animate={inView ? { opacity: 1, y: 0 } : undefined}
              transition={{ duration: 0.45, delay: (i % 6) * 0.05 }}
            >
              <button
                type="button"
                aria-pressed={active}
                aria-label={tt('apply', { name: preset.name })}
                onClick={() => setTheme(preset.id, 'user')}
                className="lv-swatch group"
                data-active={active}
                style={vars}
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
            </motion.li>
          )
        })}
      </ul>
    </section>
  )
}
