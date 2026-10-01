import type { CSSProperties } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { PRESETS } from '@features/themes/presets.generated'
import { argbToCss, type GradientSpec, type ThemeColors, type ThemeEntry } from '@features/themes/types'
import { Icon } from '../parts/Glyphs'
import { SIDEBAR } from '../parts/modules'

type Props = { activeId: string; onPick: (id: string) => void }

/** `MenuScreen` on its "Темы" page: icon sidebar + a two-column grid of theme cards. */
export function SettingsScreen({ activeId, onPick }: Props) {
  const { t } = useTranslation('clickgui')
  return (
    <div className="lv-gui-menu">
      <nav className="lv-gui-side" aria-label={t('settingsNav')}>
        {SIDEBAR.map((id, i) =>
          id === null ? (
            <span key={`split-${i}`} className="lv-gui-side-split" />
          ) : (
            <span
              key={id}
              className="lv-gui-side-item"
              data-active={id === 'themes'}
              aria-current={id === 'themes' ? 'page' : undefined}
              aria-label={t(`sidebar.${id}`)}
              role="img"
            >
              {id === 'hud' || id === 'ui' ? t(`sidebar.${id}`) : <Icon name={id} className="lv-gui-side-icon" />}
            </span>
          ),
        )}
      </nav>
      <div className="lv-gui-menu-body">
        <header className="lv-gui-menu-head">
          <Icon name="themes" className="lv-gui-menu-head-icon" />
          <h3>{t('themes')}</h3>
          <span className="lv-gui-menu-search" aria-hidden="true">
            {t('search')}
            <i />
            <Icon name="search" className="lv-gui-menu-search-icon" />
          </span>
        </header>
        <div className="lv-gui-themes" role="listbox" aria-label={t('themes')}>
          {PRESETS.map((p) => (
            <ThemeCard key={p.id} entry={p} selected={p.id === activeId} onPick={onPick} />
          ))}
        </div>
      </div>
    </div>
  )
}

/** `ThemeCardRenderer.fillSwatchPalette`: the 12 chips, each a start→end pair of palette slots. */
const CHIPS: [keyof ThemeColors, keyof ThemeColors][] = [
  ['windowBg', 'windowHeader'],
  ['cardEnabled', 'accentSoft'],
  ['windowHeader', 'windowStroke'],
  ['surface', 'surfaceHover'],
  ['surfaceHover', 'windowHeader'],
  ['accent', 'accentSoft'],
  ['accentSoft', 'strokeSoft'],
  ['windowStroke', 'strokeSoft'],
  ['strokeSoft', 'textMuted'],
  ['textPrimary', 'textMuted'],
  ['textMuted', 'surface'],
  ['windowBg', 'accentSoft'],
]

const chipGradient = (from: string, to: string, spec?: GradientSpec) => {
  const [a, b] = spec?.enabled ? [spec.start, spec.end] : [from, to]
  return `linear-gradient(135deg, ${argbToCss(a)}, ${argbToCss(b)})`
}

function ThemeCard({ entry, selected, onPick }: { entry: ThemeEntry; selected: boolean; onPick: (id: string) => void }) {
  const { t } = useTranslation('clickgui')
  const c = entry.theme
  const style = {
    '--card-accent': argbToCss(c.accent),
    '--card-accent-soft': argbToCss(c.accentSoft),
    '--card-bg': argbToCss(c.windowBg),
  } as CSSProperties
  return (
    <button
      type="button"
      role="option"
      aria-selected={selected}
      aria-label={t('theme', { name: entry.name })}
      className="lv-gui-theme"
      data-selected={selected}
      data-demo={`theme:${entry.id}`}
      style={style}
      onClick={() => onPick(entry.id)}
    >
      <span className="lv-gui-theme-name">{entry.name}</span>
      <span className="lv-gui-theme-chips" aria-hidden="true">
        {CHIPS.map(([from, to], i) => {
          const spec = i === 0 ? entry.windowGradient : i === 1 ? entry.cardGradient : undefined
          return <i key={i} style={{ background: chipGradient(c[from], c[to], spec) }} />
        })}
      </span>
    </button>
  )
}
