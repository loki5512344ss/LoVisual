/** Fallback playfield size, used before the box is measured (and under jsdom). */
export const FIELD = { w: 680, h: 380 }
const WIDGET = { w: 150, h: 56 }

export type Pos = { x: number; y: number }
/** Real draggable HUD elements from the mod: features/gui/hud/draggable/impl. */
export type WidgetId = 'fps' | 'coords' | 'modules' | 'keys' | 'ping'
export type Layout = Record<WidgetId, Pos>

/**
 * Starting layout as fractions of the field (0 = left/top edge, 1 = right/bottom edge, measured
 * against the space the widget can move in), so the default HUD fills any screen size the way
 * it would in game: FPS top-left, module list top-right, coords bottom-left, keys bottom-right.
 */
const ANCHORS: Record<WidgetId, Pos> = {
  fps: { x: 0.03, y: 0.05 },
  modules: { x: 0.97, y: 0.05 },
  ping: { x: 0.97, y: 0.5 },
  coords: { x: 0.03, y: 0.95 },
  keys: { x: 0.8, y: 0.95 },
}

export const clamp = (v: number, max: number) => Math.max(0, Math.min(v, max))

type Widgets = Partial<Record<WidgetId, HTMLElement | null>>

function room(field: HTMLElement | null, el: HTMLElement | null | undefined) {
  const w = field?.clientWidth || FIELD.w
  const h = field?.clientHeight || FIELD.h
  return { maxX: Math.max(0, w - (el?.offsetWidth || WIDGET.w)), maxY: Math.max(0, h - (el?.offsetHeight || WIDGET.h)) }
}

/** Default layout placed inside the measured field. */
export function startLayout(field: HTMLElement | null, widgets: Widgets): Layout {
  const out = {} as Layout
  for (const id of Object.keys(ANCHORS) as WidgetId[]) {
    const { maxX, maxY } = room(field, widgets[id])
    out[id] = { x: Math.round(ANCHORS[id].x * maxX), y: Math.round(ANCHORS[id].y * maxY) }
  }
  return out
}

/** Pull every widget back inside the measured field (resize). */
export function fitToField(state: Layout, field: HTMLElement | null, widgets: Widgets): Layout {
  const next = { ...state }
  let changed = false
  for (const id of Object.keys(next) as WidgetId[]) {
    const { maxX, maxY } = room(field, widgets[id])
    const x = clamp(next[id].x, maxX)
    const y = clamp(next[id].y, maxY)
    if (x !== next[id].x || y !== next[id].y) {
      next[id] = { x, y }
      changed = true
    }
  }
  return changed ? next : state
}

/** Movement bounds for one widget element inside the field. */
export const bounds = (field: HTMLElement | null, el: HTMLElement) => room(field, el)
