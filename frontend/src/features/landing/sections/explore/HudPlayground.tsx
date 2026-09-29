import { useLayoutEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '@shared/ui/Button'
import { themeToCssVars } from '@features/clickgui/themeVars'
import { parseArgb } from '@features/themes/types'
import { useActiveTheme } from '@features/themes/useActiveTheme'
import { VoxelScene } from '../../effects/VoxelScene'
import { bounds, clamp, fitToField, type Layout, startLayout, type WidgetId } from './hud/layout'
import { WidgetContent } from './hud/WidgetContent'
import '@features/landing/styles/landing.css'
import '@features/landing/styles/sections.css'

const STEP = 8

type Drag = { id: WidgetId; px: number; py: number; ox: number; oy: number }

/**
 * A full game screen with the mod's real HUD widgets on it. Holding a widget switches the
 * screen into "edit mode" (grid overlay, lifted widget), the same feedback the in-game editor gives.
 */
export function HudPlayground() {
  const { t } = useTranslation('landing')
  const { theme } = useActiveTheme()
  const { r, g, b } = parseArgb(theme.theme.accent)
  const fieldRef = useRef<HTMLDivElement>(null)
  const widgetRefs = useRef<Partial<Record<WidgetId, HTMLElement | null>>>({})
  const drag = useRef<Drag | null>(null)
  const [pos, setPos] = useState<Layout>(() => startLayout(null, {}))
  const [held, setHeld] = useState<WidgetId | null>(null)

  useLayoutEffect(() => {
    setPos(startLayout(fieldRef.current, widgetRefs.current))
    const field = fieldRef.current
    if (!field || typeof ResizeObserver === 'undefined') return
    const ro = new ResizeObserver(() => setPos((prev) => fitToField(prev, fieldRef.current, widgetRefs.current)))
    ro.observe(field)
    return () => ro.disconnect()
  }, [])

  const move = (id: WidgetId, x: number, y: number, el: HTMLElement) => {
    const { maxX, maxY } = bounds(fieldRef.current, el)
    setPos((prev) => ({ ...prev, [id]: { x: clamp(x, maxX), y: clamp(y, maxY) } }))
  }

  const onKeyDown = (id: WidgetId) => (e: React.KeyboardEvent<HTMLElement>) => {
    const deltas: Record<string, [number, number]> = {
      ArrowLeft: [-STEP, 0], ArrowRight: [STEP, 0], ArrowUp: [0, -STEP], ArrowDown: [0, STEP],
    }
    const d = deltas[e.key]
    if (!d) return
    e.preventDefault()
    move(id, pos[id].x + d[0], pos[id].y + d[1], e.currentTarget)
  }

  const onPointerDown = (id: WidgetId) => (e: React.PointerEvent<HTMLElement>) => {
    e.currentTarget.setPointerCapture(e.pointerId)
    drag.current = { id, px: e.clientX, py: e.clientY, ox: pos[id].x, oy: pos[id].y }
    setHeld(id)
  }

  const onPointerMove = (e: React.PointerEvent<HTMLElement>) => {
    const d = drag.current
    if (d) move(d.id, d.ox + e.clientX - d.px, d.oy + e.clientY - d.py, e.currentTarget)
  }

  const onPointerUp = () => {
    drag.current = null
    setHeld(null)
  }

  const widgets: { id: WidgetId; label: string }[] = [
    { id: 'fps', label: t('hud.widgetFps') },
    { id: 'coords', label: t('hud.widgetCoords') },
    { id: 'modules', label: t('hud.widgetModules') },
    { id: 'keys', label: t('hud.widgetKeys') },
    { id: 'ping', label: t('hud.widgetPing') },
  ]

  return (
    <section aria-labelledby="hud-title" className="flex flex-col gap-8 py-20 sm:py-28">
      <div className="grid items-end gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] lg:gap-14">
        <h2 id="hud-title" className="text-[2.2rem] leading-[1.05] tracking-[-0.02em] sm:text-[3.2rem]">{t('hud.title')}</h2>
        <p className="max-w-[46ch] text-lg text-frost">{t('hud.lead')}</p>
      </div>

      <div
        ref={fieldRef}
        style={themeToCssVars(theme)}
        className="lv-hud-field lv-hud-screen relative isolate overflow-hidden"
      >
        <VoxelScene accent={`rgb(${r}, ${g}, ${b})`} parallax={false} />
        <div aria-hidden="true" className="absolute inset-0 bg-black/20" />
        <div aria-hidden="true" className="lv-hud-grid" data-on={held !== null} />
        {widgets.map((w) => (
          <div
            key={w.id}
            ref={(el) => {
              widgetRefs.current[w.id] = el
            }}
            role="button"
            tabIndex={0}
            title={w.label}
            aria-label={t('hud.dragAria', { name: w.label })}
            data-held={held === w.id}
            onKeyDown={onKeyDown(w.id)}
            onPointerDown={onPointerDown(w.id)}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
            style={{ left: pos[w.id].x, top: pos[w.id].y }}
            className="lv-hud-widget absolute touch-none select-none"
          >
            <WidgetContent id={w.id} unitMs={t('hud.unitMs')} />
          </div>
        ))}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="flex items-center gap-2 text-sm text-frost">
          <span className="lv-live-dot" aria-hidden="true" />
          {t('hud.caption')}
        </p>
        <Button variant="quiet" onClick={() => setPos(startLayout(fieldRef.current, widgetRefs.current))}>
          {t('hud.reset')}
        </Button>
      </div>
    </section>
  )
}
