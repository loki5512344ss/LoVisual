import { type CSSProperties, useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { prefersReducedMotion } from '../../shared/motion/reducedMotion'
import { PRESETS } from '../themes/presets.generated'
import type { ThemeEntry } from '../themes/types'
import { useActiveTheme } from '../themes/useActiveTheme'
import { DEMO, useDemo } from './demo'
import { ModuleCard } from './ModuleCard'
import './parts/clickgui.css'
import { CategoryIcon, DemoCursor, LogoMark, SearchGlyph } from './parts/Glyphs'
import { CATEGORIES, QUICK_THEMES, TABS } from './parts/modules'
import { themeToCssVars } from './themeVars'

type Props = {
  /** Runs the scripted cursor demo (pauses while hovered or when `paused`). */
  demo?: boolean
  paused?: boolean
  /** Overrides the active theme — for previews. */
  theme?: ThemeEntry
  className?: string
}

export function ClickGuiWindow({ demo = true, paused = false, theme: override, className = '' }: Props) {
  const { t } = useTranslation('clickgui')
  const { theme: active, setTheme } = useActiveTheme()
  const theme = override ?? active
  const rootRef = useRef<HTMLDivElement>(null)
  const [hovered, setHovered] = useState(false)
  const [reduced] = useState(prefersReducedMotion)
  const themeIdRef = useRef(theme.id)
  useEffect(() => {
    themeIdRef.current = theme.id
  }, [theme.id])

  const { cursor, state, dispatch } = useDemo(DEMO, {
    paused: !demo || paused || hovered,
    reduced,
    rootRef,
    nextTheme: () => {
      const i = QUICK_THEMES.indexOf(themeIdRef.current as (typeof QUICK_THEMES)[number])
      return QUICK_THEMES[(i + 1) % QUICK_THEMES.length]
    },
    onTheme: (id) => setTheme(id, 'demo'),
  })

  const vars = useMemo(() => themeToCssVars(theme), [theme])
  const category = CATEGORIES.find((c) => c.id === state.category) ?? CATEGORIES[0]

  return (
    <div
      ref={rootRef}
      role="group"
      aria-label={t('windowLabel')}
      data-testid="clickgui-root"
      className={`lv-gui ${className}`}
      style={vars}
      onPointerEnter={(e) => e.pointerType === 'mouse' && setHovered(true)}
      onPointerLeave={() => setHovered(false)}
    >
      <div className="lv-gui-bloom" aria-hidden="true" />
      <div className="lv-gui-window">
        <header className="lv-gui-header">
          <span className="lv-gui-logo">
            <LogoMark />
            LoVisual
          </span>
          <nav className="lv-gui-tabs" aria-hidden="true">
            {TABS.map((tab, i) => (
              <span key={tab} className="lv-gui-tab" data-active={i === 0}>
                {tab}
              </span>
            ))}
          </nav>
          <span className="lv-gui-search" aria-hidden="true">
            <SearchGlyph />
          </span>
        </header>

        <div className="lv-gui-body">
          <aside className="lv-gui-cats" aria-label={t('categories')}>
            {CATEGORIES.map((c) => (
              <button
                key={c.id}
                type="button"
                className="lv-gui-cat"
                data-active={c.id === category.id}
                aria-pressed={c.id === category.id}
                data-demo={`cat:${c.id}`}
                aria-label={c.name}
                onClick={() => dispatch({ type: 'category', id: c.id })}
              >
                <CategoryIcon id={c.id} />
                <span className="lv-gui-cat-name">{c.name}</span>
                <span className="lv-gui-cat-count">{c.modules.length}</span>
              </button>
            ))}
            <div className="lv-gui-theme-dots">
              {QUICK_THEMES.map((id) => {
                const preset = PRESETS.find((p) => p.id === id)
                if (!preset) return null
                const dot = themeToCssVars(preset)
                return (
                  <button
                    key={id}
                    type="button"
                    className="lv-gui-theme-dot"
                    data-active={theme.id === id}
                    data-demo={`theme:${id}`}
                    aria-label={t('theme', { name: preset.name })}
                    style={{ '--dot': dot['--gui-header-gradient'], '--dot-accent': dot['--gui-accent'] } as CSSProperties}
                    onClick={() => setTheme(id, 'user')}
                  />
                )
              })}
            </div>
          </aside>

          <div className="lv-gui-main">
            <div className="lv-gui-grid" key={category.id}>
              {category.modules.map((m) => (
                <ModuleCard key={m.name} module={m} state={state} dispatch={dispatch} />
              ))}
            </div>
          </div>
        </div>
      </div>
      <DemoCursor cursor={cursor} />
    </div>
  )
}
