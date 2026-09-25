import { type Dispatch, type RefObject, useEffect, useReducer, useRef, useState } from 'react'
import { CATEGORIES, type CategoryId, settingKey } from './parts/modules'

export type DemoStep = { at: number /* ms */; action: 'move' | 'click' | 'drag' | 'theme'; target: string; value?: number | string }

export const LOOP_MS = 12_500

/** The scripted loop: pick Visuals, switch Trails on, open it, drag Length, tick Rainbow, change theme, tidy up. */
export const DEMO: DemoStep[] = [
  { at: 400, action: 'move', target: 'cat:visuals' },
  { at: 1100, action: 'click', target: 'cat:visuals' },
  { at: 1700, action: 'move', target: 'toggle:Trails' },
  { at: 2400, action: 'click', target: 'toggle:Trails' },
  { at: 3000, action: 'move', target: 'expand:Trails' },
  { at: 3600, action: 'click', target: 'expand:Trails' },
  { at: 4300, action: 'move', target: 'knob:Trails.Length' },
  { at: 4900, action: 'drag', target: 'slider:Trails.Length', value: 85 },
  { at: 6300, action: 'move', target: 'check:Trails.Rainbow' },
  { at: 6900, action: 'click', target: 'check:Trails.Rainbow' },
  { at: 7700, action: 'move', target: 'theme:next' },
  { at: 8400, action: 'theme', target: 'theme:next' },
  { at: 9400, action: 'move', target: 'expand:Trails' },
  { at: 10000, action: 'click', target: 'expand:Trails' },
  { at: 10800, action: 'move', target: 'rest' },
]

export type GuiState = {
  category: CategoryId
  enabled: Record<string, boolean>
  expanded: string | null
  values: Record<string, number | boolean | string>
  dragging: boolean
}

export type GuiAction =
  | { type: 'category'; id: CategoryId }
  | { type: 'toggle'; module: string }
  | { type: 'expand'; module: string }
  | { type: 'set'; key: string; value: number | boolean | string }
  | { type: 'dragging'; on: boolean }
  | { type: 'reset' }

export function initialGuiState(finished = false): GuiState {
  const enabled: Record<string, boolean> = {}
  const values: GuiState['values'] = {}
  for (const c of CATEGORIES) {
    for (const m of c.modules) {
      enabled[m.name] = Boolean(m.on)
      for (const s of m.settings ?? []) values[settingKey(m.name, s.name)] = s.value
    }
  }
  if (!finished) return { category: 'combat', enabled, expanded: null, values, dragging: false }
  // The resting "after the demo" frame, used under reduced motion.
  enabled.Trails = true
  values[settingKey('Trails', 'Length')] = 85
  return { category: 'visuals', enabled, expanded: 'Trails', values, dragging: false }
}

export function guiReducer(state: GuiState, action: GuiAction): GuiState {
  switch (action.type) {
    case 'category':
      return { ...state, category: action.id, expanded: null }
    case 'toggle':
      return { ...state, enabled: { ...state.enabled, [action.module]: !state.enabled[action.module] } }
    case 'expand':
      return { ...state, expanded: state.expanded === action.module ? null : action.module }
    case 'set':
      return { ...state, values: { ...state.values, [action.key]: action.value } }
    case 'dragging':
      return { ...state, dragging: action.on }
    case 'reset':
      return initialGuiState()
  }
}

export type Cursor = { x: number; y: number; visible: boolean; pressed: boolean; duration: number }

type Options = {
  paused: boolean
  reduced: boolean
  rootRef: RefObject<HTMLElement | null>
  /** Resolves `theme:next` to a concrete `theme:<id>` target and applies it. */
  nextTheme: () => string
  onTheme: (id: string) => void
}

/** Layout position of `el` inside `root`, ignoring CSS transforms (the window is tilted in 3D). */
function offsetWithin(el: HTMLElement, root: HTMLElement): { x: number; y: number } {
  let x = 0
  let y = 0
  let node: HTMLElement | null = el
  while (node && node !== root) {
    x += node.offsetLeft
    y += node.offsetTop
    node = node.offsetParent as HTMLElement | null
  }
  return { x, y }
}

export function useDemo(steps: DemoStep[], opts: Options): { cursor: Cursor; state: GuiState; dispatch: Dispatch<GuiAction> } {
  const [state, dispatch] = useReducer(guiReducer, opts.reduced, initialGuiState)
  const [cursor, setCursor] = useState<Cursor>({ x: 0, y: 0, visible: false, pressed: false, duration: 0 })
  const [cycle, setCycle] = useState(0)
  const optsRef = useRef(opts)
  useEffect(() => {
    optsRef.current = opts
  })

  useEffect(() => {
    if (opts.paused || opts.reduced) return
    const timers: number[] = []
    const later = (ms: number, fn: () => void) => timers.push(window.setTimeout(fn, ms))
    let pendingTheme = ''

    const locate = (target: string, fraction?: number) => {
      const root = optsRef.current.rootRef.current
      if (!root) return null
      if (target === 'rest') return { x: root.offsetWidth * 0.86, y: root.offsetHeight * 0.9 }
      const resolved = target === 'theme:next' ? (pendingTheme ||= `theme:${optsRef.current.nextTheme()}`) : target
      const el = root.querySelector<HTMLElement>(`[data-demo="${resolved}"]`)
      if (!el) return null
      const { x, y } = offsetWithin(el, root)
      const fx = fraction ?? 0.5
      return { x: x + el.offsetWidth * fx, y: y + el.offsetHeight * 0.5 }
    }

    const move = (target: string, duration: number, fraction?: number) => {
      const p = locate(target, fraction)
      if (p) setCursor((c) => ({ ...c, ...p, visible: true, duration }))
    }

    const click = (fn: () => void) => {
      setCursor((c) => ({ ...c, pressed: true }))
      later(160, () => {
        setCursor((c) => ({ ...c, pressed: false }))
        fn()
      })
    }

    const run = (step: DemoStep) => {
      const [kind, arg] = step.target.split(':')
      if (step.action === 'move') return move(step.target, 650)
      if (step.action === 'theme') {
        return click(() => optsRef.current.onTheme(pendingTheme.replace('theme:', '')))
      }
      if (step.action === 'drag') {
        const [module, setting] = arg.split('.')
        const value = Number(step.value ?? 0)
        setCursor((c) => ({ ...c, pressed: true }))
        dispatch({ type: 'dragging', on: true })
        move(step.target, 900, value / 100)
        dispatch({ type: 'set', key: settingKey(module, setting), value })
        return later(950, () => {
          setCursor((c) => ({ ...c, pressed: false }))
          dispatch({ type: 'dragging', on: false })
        })
      }
      click(() => {
        if (kind === 'cat') dispatch({ type: 'category', id: arg as CategoryId })
        if (kind === 'toggle') dispatch({ type: 'toggle', module: arg })
        if (kind === 'expand') dispatch({ type: 'expand', module: arg })
        if (kind === 'check') dispatch({ type: 'set', key: arg, value: true })
      })
    }

    dispatch({ type: 'reset' })
    const lead = cycle === 0 ? 600 : 0
    for (const step of steps) later(step.at + lead, () => run(step))
    later(LOOP_MS + lead, () => setCycle((n) => n + 1))
    return () => timers.forEach((t) => window.clearTimeout(t))
  }, [steps, opts.paused, opts.reduced, cycle])

  const idle = opts.paused || opts.reduced
  return { cursor: idle ? { ...cursor, visible: false, pressed: false } : cursor, state, dispatch }
}
