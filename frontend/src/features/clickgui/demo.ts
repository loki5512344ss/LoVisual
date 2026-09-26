import { type Dispatch, type RefObject, useEffect, useReducer, useRef, useState } from 'react'
import { CATEGORIES, type TabId } from './parts/modules'

export type DemoStep = { at: number /* ms */; action: 'move' | 'click' | 'theme'; target: string }

export const LOOP_MS = 11_000

/** The scripted loop: toggle two modules, open Settings, pick a theme card, come back to Modules. */
export const DEMO: DemoStep[] = [
  { at: 400, action: 'move', target: 'module:AmbientParticles' },
  { at: 1100, action: 'click', target: 'module:AmbientParticles' },
  { at: 1800, action: 'move', target: 'module:DamageTint' },
  { at: 2500, action: 'click', target: 'module:DamageTint' },
  { at: 3300, action: 'move', target: 'tab:settings' },
  { at: 4000, action: 'click', target: 'tab:settings' },
  { at: 4800, action: 'move', target: 'theme:next' },
  { at: 5600, action: 'theme', target: 'theme:next' },
  { at: 7200, action: 'move', target: 'tab:modules' },
  { at: 7900, action: 'click', target: 'tab:modules' },
  { at: 8800, action: 'move', target: 'rest' },
]

/** Modules the demo switches on — also enabled in the static reduced-motion frame. */
const DEMO_TOGGLES = ['AmbientParticles', 'DamageTint']

export type GuiState = { tab: TabId; enabled: Record<string, boolean> }

export type GuiAction = { type: 'tab'; id: TabId } | { type: 'toggle'; module: string } | { type: 'reset' }

export function initialGuiState(finished = false): GuiState {
  const enabled: Record<string, boolean> = {}
  for (const c of CATEGORIES) for (const m of c.modules) enabled[m.name] = Boolean(m.on)
  // The resting "after the demo" frame, used under reduced motion.
  if (finished) for (const name of DEMO_TOGGLES) enabled[name] = true
  return { tab: 'modules', enabled }
}

export function guiReducer(state: GuiState, action: GuiAction): GuiState {
  switch (action.type) {
    case 'tab':
      return { ...state, tab: action.id }
    case 'toggle':
      return { ...state, enabled: { ...state.enabled, [action.module]: !state.enabled[action.module] } }
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

/** Visual position of `el` inside `root` in root-local pixels: rects see CSS transforms (the tab bar's
 *  `translateX(-50%)`, the screen's entrance animation) that the `offsetLeft` chain silently skips.
 *  `scale` undoes transforms applied by ancestors of the root (the hero's entrance scale). */
function positionWithin(el: HTMLElement, root: HTMLElement): { x: number; y: number } {
  const rr = root.getBoundingClientRect()
  const tr = el.getBoundingClientRect()
  const scale = root.offsetWidth && rr.width ? rr.width / root.offsetWidth : 1
  return {
    x: (tr.left - rr.left) / scale + (tr.width / scale) * 0.62,
    y: (tr.top - rr.top) / scale + (tr.height / scale) * 0.5,
  }
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
    /** When the pointer finishes its current travel; a press must not fire before that. */
    let travelEndsAt = 0

    const locate = (target: string) => {
      const root = optsRef.current.rootRef.current
      if (!root) return null
      if (target === 'rest') return { x: root.offsetWidth * 0.86, y: root.offsetHeight * 0.9 }
      const resolved = target === 'theme:next' ? (pendingTheme ||= `theme:${optsRef.current.nextTheme()}`) : target
      const el = root.querySelector<HTMLElement>(`[data-demo="${resolved}"]`)
      if (!el) return null
      return positionWithin(el, root)
    }

    const move = (target: string, duration: number) => {
      const p = locate(target)
      if (!p) return
      travelEndsAt = performance.now() + duration
      setCursor((c) => ({ ...c, ...p, visible: true, duration }))
    }

    /** Press `target` only once the pointer is actually on it: timers trail the CSS travel, and a
     *  target that moved or unmounted would mean a click on air. */
    const click = (target: string, fn: () => void) => {
      later(Math.max(0, travelEndsAt - performance.now()) + 60, () => {
        const p = locate(target)
        if (!p) return
        setCursor((c) => ({ ...c, ...p, visible: true, duration: 0 }))
        later(60, () => {
          setCursor((c) => ({ ...c, pressed: true }))
          later(160, () => {
            setCursor((c) => ({ ...c, pressed: false }))
            fn()
          })
        })
      })
    }

    const run = (step: DemoStep) => {
      const [kind, arg] = step.target.split(':')
      if (step.action === 'move') return move(step.target, 650)
      if (step.action === 'theme') {
        return click(step.target, () => optsRef.current.onTheme(pendingTheme.replace('theme:', '')))
      }
      click(step.target, () => {
        if (kind === 'tab') dispatch({ type: 'tab', id: arg as TabId })
        if (kind === 'module') dispatch({ type: 'toggle', module: arg })
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
