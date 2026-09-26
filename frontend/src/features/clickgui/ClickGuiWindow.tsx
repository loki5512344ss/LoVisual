import { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { prefersReducedMotion } from '../../shared/motion/reducedMotion'
import type { ThemeEntry } from '../themes/types'
import { useActiveTheme } from '../themes/useActiveTheme'
import { DEMO, useDemo } from './demo'
import './parts/clickgui.css'
import { Backdrop } from './parts/Backdrop'
import { DemoCursor, Icon } from './parts/Glyphs'
import { QUICK_THEMES, TABS } from './parts/modules'
import { ModulesScreen } from './screens/ModulesScreen'
import './screens/screens.css'
import { SettingsScreen } from './screens/SettingsScreen'
import { themeToCssVars } from './themeVars'

type Props = {
  /** Runs the scripted cursor demo (pauses while hovered or when `paused`). */
  demo?: boolean
  paused?: boolean
  /** Overrides the active theme — for previews. */
  theme?: ThemeEntry
  /**
   * Controlled theme picking: when set, theme cards in Settings call this instead of switching the
   * site-wide theme (the /themes editor loads the preset into its draft).
   */
  onPickTheme?: (id: string) => void
  /** Image URL painted over the procedural backdrop (e.g. a real in-game F1 screenshot). */
  backdrop?: string
  className?: string
}

const HINTS = {
  modules: ['toggle', 'settings', 'moduleList', 'search', 'close', 'hide'],
  settings: ['pick', 'edit', 'wheel', 'close', 'hide'],
} as const

function useClock(): string {
  const format = () => new Date().toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
  const [now, setNow] = useState(format)
  useEffect(() => {
    const id = window.setInterval(() => setNow(format()), 15_000)
    return () => window.clearInterval(id)
  }, [])
  return now
}

/**
 * A faithful replica of the in-game ClickGui: the top pill bar (clock + Modules/Settings switch),
 * the four glass category panels, and the Settings → Темы page whose cards re-theme everything.
 */
export function ClickGuiWindow({ demo = true, paused = false, theme: override, onPickTheme, backdrop, className = '' }: Props) {
  const { t } = useTranslation('clickgui')
  const { theme: active, setTheme } = useActiveTheme()
  const theme = override ?? active
  const rootRef = useRef<HTMLDivElement>(null)
  const [hovered, setHovered] = useState(false)
  const [reduced] = useState(prefersReducedMotion)
  const clock = useClock()
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
    onTheme: (id) => (onPickTheme ? onPickTheme(id) : setTheme(id, 'demo')),
  })

  const vars = useMemo(
    () => (backdrop ? { ...themeToCssVars(theme), '--gui-backdrop-image': `url(${JSON.stringify(backdrop)})` } : themeToCssVars(theme)),
    [theme, backdrop],
  )
  const pick = onPickTheme ?? ((id: string) => setTheme(id, 'user'))

  return (
    <div
      ref={rootRef}
      role="group"
      aria-label={t('windowLabel')}
      data-testid="clickgui-root"
      className={`lv-gui ${className}`}
      style={vars}
      onPointerEnter={() => setHovered(true)}
      onPointerLeave={() => setHovered(false)}
    >
      <div className="lv-gui-stage" data-tab={state.tab}>
        <div className="lv-gui-world" aria-hidden="true">
          <Backdrop />
          <div className="lv-gui-photo" />
        </div>
        <p className="lv-gui-watermark" aria-hidden="true">
          <b>LoVisual</b>·400 fps <span>0 ms</span>
        </p>

        <div className="lv-gui-bar">
          <time className="lv-gui-clock" aria-label={t('clock')}>
            {clock}
          </time>
          <div className="lv-gui-tabs" role="tablist" aria-label={t('tabsLabel')}>
            <span className="lv-gui-tab-thumb" aria-hidden="true" />
            {TABS.map((id) => (
              <button
                key={id}
                type="button"
                role="tab"
                className="lv-gui-tab"
                aria-selected={state.tab === id}
                data-demo={`tab:${id}`}
                onClick={() => dispatch({ type: 'tab', id })}
              >
                {t(`tabs.${id}`)}
              </button>
            ))}
          </div>
        </div>

        {state.tab === 'modules' && (
          <button type="button" className="lv-gui-search" aria-label={t('search')}>
            <Icon name="search" className="lv-gui-search-icon" />
          </button>
        )}

        <div className="lv-gui-screen" key={state.tab}>
          {state.tab === 'modules' ? (
            <ModulesScreen state={state} dispatch={dispatch} />
          ) : (
            <SettingsScreen activeId={theme.id} onPick={pick} />
          )}
        </div>

        <ul className="lv-gui-hints" aria-hidden="true">
          {HINTS[state.tab].map((k) => (
            <li key={k}>{t(`hints.${state.tab}.${k}` as `hints.modules.toggle`)}</li>
          ))}
        </ul>
      </div>
      <DemoCursor cursor={cursor} />
    </div>
  )
}
