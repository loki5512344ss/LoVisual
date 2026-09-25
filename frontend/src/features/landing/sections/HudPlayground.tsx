import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '../../../shared/ui/Button'
import { themeToCssVars } from '../../clickgui/themeVars'
import { parseArgb } from '../../themes/types'
import { useActiveTheme } from '../../themes/useActiveTheme'
import { VoxelScene } from '../effects/VoxelScene'
import '../styles/landing.css'

const STEP = 8
/** Fallback playfield size, used before the box is measured (and under jsdom). */
const FIELD = { w: 680, h: 380 }
const WIDGET = { w: 150, h: 56 }

type Pos = { x: number; y: number }
/** Real draggable HUD elements from the mod: features/gui/hud/draggable/impl. */
type WidgetId = 'fps' | 'coords' | 'modules' | 'keys' | 'ping'

const START: Record<WidgetId, Pos> = {
  fps: { x: 20, y: 20 },
  coords: { x: 20, y: 310 },
  modules: { x: FIELD.w - 190, y: 20 },
  ping: { x: FIELD.w - 170, y: 150 },
  keys: { x: FIELD.w - 140, y: FIELD.h - 120 },
}

const clamp = (v: number, max: number) => Math.max(0, Math.min(v, max))

/** Enabled modules as they would appear in the mod's ModuleList panel. */
const MODULES_ON = ['TargetESP', 'PvpCooldowns', 'HitBubbles', 'ShaderSky']

/** Widgets read the accent through --gui-accent so it cross-fades with the active theme. */
const ACCENT = 'var(--gui-accent)'

function WidgetContent({ id, accent, unitMs }: { id: WidgetId; accent: string; unitMs: string }) {
  switch (id) {
    case 'fps':
      return (
        <div className="flex items-baseline gap-1.5">
          <span className="font-code text-2xl leading-none font-bold" style={{ color: accent }}>
            240
          </span>
          <span className="text-[0.6rem] tracking-[0.15em] text-frost uppercase">FPS</span>
        </div>
      )
    case 'coords':
      return (
        <div className="font-code text-sm leading-none text-snow">
          <span style={{ color: accent }}>X</span> 128&nbsp; <span style={{ color: accent }}>Y</span> 74&nbsp;{' '}
          <span style={{ color: accent }}>Z</span> -204
        </div>
      )
    case 'modules':
      return (
        <div className="flex flex-col items-end gap-1">
          {MODULES_ON.map((m) => (
            <span
              key={m}
              className="rounded-[5px] border-r-2 bg-black/25 px-2 py-[3px] font-code text-[0.7rem] leading-none text-snow"
              style={{ borderRightColor: accent }}
            >
              {m}
            </span>
          ))}
        </div>
      )
    case 'keys': {
      const cap = (k: string, pressed = false) => (
        <span
          key={k}
          className="grid size-7 place-items-center rounded-[6px] border bg-white/5 font-code text-xs leading-none"
          style={{
            borderColor: pressed ? accent : 'rgb(255 255 255 / 0.12)',
            color: pressed ? accent : undefined,
            background: pressed ? `color-mix(in srgb, ${accent} 18%, transparent)` : undefined,
          }}
        >
          {k}
        </span>
      )
      return (
        <div className="flex flex-col items-center gap-1">
          {cap('W', true)}
          <div className="flex gap-1">
            {cap('A')}
            {cap('S', true)}
            {cap('D')}
          </div>
        </div>
      )
    }
    case 'ping':
      return (
        <div className="flex items-center gap-2">
          <span className="font-code text-sm leading-none text-snow">
            32&nbsp;<span className="text-frost">{unitMs}</span>
          </span>
          <span aria-hidden="true" className="flex items-end gap-[3px]">
            {[6, 10, 14, 18].map((h, i) => (
              <span
                key={h}
                className="w-[4px] rounded-sm"
                style={{ height: h, background: i < 2 ? accent : 'rgb(255 255 255 / 0.18)' }}
              />
            ))}
          </span>
        </div>
      )
  }
}

export function HudPlayground() {
  const { t } = useTranslation('landing')
  const { theme } = useActiveTheme()
  const { r, g, b } = parseArgb(theme.theme.accent)
  const fieldRef = useRef<HTMLDivElement>(null)
  const drag = useRef<{ id: WidgetId; px: number; py: number; ox: number; oy: number } | null>(null)
  const [pos, setPos] = useState<Record<WidgetId, Pos>>(START)

  const bounds = (el: HTMLElement) => {
    const field = fieldRef.current
    const w = field?.clientWidth || FIELD.w
    const h = field?.clientHeight || FIELD.h
    return { maxX: Math.max(0, w - (el.offsetWidth || WIDGET.w)), maxY: Math.max(0, h - (el.offsetHeight || WIDGET.h)) }
  }

  const nudge = (id: WidgetId, dx: number, dy: number, el: HTMLElement) => {
    const { maxX, maxY } = bounds(el)
    setPos((prev) => ({ ...prev, [id]: { x: clamp(prev[id].x + dx, maxX), y: clamp(prev[id].y + dy, maxY) } }))
  }

  const onKeyDown = (id: WidgetId) => (e: React.KeyboardEvent<HTMLElement>) => {
    const deltas: Record<string, [number, number]> = {
      ArrowLeft: [-STEP, 0], ArrowRight: [STEP, 0], ArrowUp: [0, -STEP], ArrowDown: [0, STEP],
    }
    const d = deltas[e.key]
    if (!d) return
    e.preventDefault()
    nudge(id, d[0], d[1], e.currentTarget)
  }

  const onPointerDown = (id: WidgetId) => (e: React.PointerEvent<HTMLElement>) => {
    e.currentTarget.setPointerCapture(e.pointerId)
    drag.current = { id, px: e.clientX, py: e.clientY, ox: pos[id].x, oy: pos[id].y }
  }

  const onPointerMove = (e: React.PointerEvent<HTMLElement>) => {
    const d = drag.current
    if (!d) return
    const { maxX, maxY } = bounds(e.currentTarget)
    setPos((prev) => ({ ...prev, [d.id]: { x: clamp(d.ox + e.clientX - d.px, maxX), y: clamp(d.oy + e.clientY - d.py, maxY) } }))
  }

  const onPointerUp = () => {
    drag.current = null
  }

  const widgets: { id: WidgetId; label: string }[] = [
    { id: 'fps', label: t('hud.widgetFps') },
    { id: 'coords', label: t('hud.widgetCoords') },
    { id: 'modules', label: t('hud.widgetModules') },
    { id: 'keys', label: t('hud.widgetKeys') },
    { id: 'ping', label: t('hud.widgetPing') },
  ]

  return (
    <section aria-labelledby="hud-title" className="grid gap-10 py-20 sm:py-28 lg:grid-cols-[minmax(0,0.85fr)_minmax(0,1.15fr)] lg:gap-14">
      <div className="flex min-w-0 flex-col items-start gap-4">
        <span className="font-code text-xs uppercase tracking-[0.2em] text-ice">{t('hud.eyebrow')}</span>
        <h2 id="hud-title" className="text-[2rem] leading-[1.1] sm:text-[2.6rem]">{t('hud.title')}</h2>
        <p className="max-w-[42ch] text-lg text-frost">{t('hud.lead')}</p>
        <Button variant="quiet" className="mt-2" onClick={() => setPos(START)}>
          {t('hud.reset')}
        </Button>
      </div>

      <div className="min-w-0">
        <div
          ref={fieldRef}
          style={themeToCssVars(theme)}
          className="lv-hud-field relative isolate overflow-hidden rounded-panel border border-white/10"
        >
          <VoxelScene accent={`rgb(${r}, ${g}, ${b})`} />
          <div aria-hidden="true" className="absolute inset-0 bg-black/25" />
          {widgets.map((w) => (
            <div
              key={w.id}
              role="button"
              tabIndex={0}
              title={w.label}
              aria-label={t('hud.dragAria', { name: w.label })}
              onKeyDown={onKeyDown(w.id)}
              onPointerDown={onPointerDown(w.id)}
              onPointerMove={onPointerMove}
              onPointerUp={onPointerUp}
              style={{ left: pos[w.id].x, top: pos[w.id].y }}
              className="lv-hud-widget absolute touch-none select-none"
            >
              <WidgetContent id={w.id} accent={ACCENT} unitMs={t('hud.unitMs')} />
            </div>
          ))}
        </div>
        <p className="mt-3 flex items-center gap-2 text-sm text-frost">
          <span className="lv-live-dot" aria-hidden="true" />
          {t('hud.caption')}
        </p>
      </div>
    </section>
  )
}
