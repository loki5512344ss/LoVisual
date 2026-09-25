import type { Dispatch } from 'react'
import type { GuiAction, GuiState } from '../demo'
import { type Setting, settingKey } from './modules'

const COLOR_CYCLE = ['var(--gui-accent)', '#ff6b9a', '#f2c14e', '#7cf29c', '#9d7bff']

type Props = { module: string; settings: Setting[]; state: GuiState; dispatch: Dispatch<GuiAction> }

/** Expanded settings of a module card: sliders, checkboxes and color dots like the in-game panel. */
export function SettingControls({ module, settings, state, dispatch }: Props) {
  return (
    <div className="lv-gui-settings">
      {settings.map((s) => {
        const key = settingKey(module, s.name)
        const value = state.values[key] ?? s.value
        if (s.kind === 'slider') {
          return <Slider key={key} id={key} setting={s} value={Number(value)} dragging={state.dragging} dispatch={dispatch} />
        }
        if (s.kind === 'check') {
          const on = Boolean(value)
          return (
            <button
              key={key}
              type="button"
              role="checkbox"
              aria-checked={on}
              data-demo={`check:${key}`}
              className="lv-gui-check-row"
              onClick={() => dispatch({ type: 'set', key, value: !on })}
            >
              <span>{s.name}</span>
              <span className="lv-gui-check" data-on={on}>
                <svg viewBox="0 0 12 12" aria-hidden="true">
                  <path d="M2.5 6.2 5 8.6l4.5-5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
                </svg>
              </span>
            </button>
          )
        }
        const current = String(value)
        const next = COLOR_CYCLE[(COLOR_CYCLE.indexOf(current) + 1) % COLOR_CYCLE.length]
        return (
          <button
            key={key}
            type="button"
            className="lv-gui-check-row"
            aria-label={s.name}
            onClick={() => dispatch({ type: 'set', key, value: next })}
          >
            <span>{s.name}</span>
            <span className="lv-gui-color" style={{ background: current, boxShadow: `0 0 10px ${current}` }} />
          </button>
        )
      })}
    </div>
  )
}

type SliderProps = {
  id: string
  setting: Extract<Setting, { kind: 'slider' }>
  value: number
  dragging: boolean
  dispatch: Dispatch<GuiAction>
}

function Slider({ id, setting, value, dragging, dispatch }: SliderProps) {
  const pct = ((value - setting.min) / (setting.max - setting.min)) * 100
  return (
    <label className="lv-gui-slider" data-dragging={dragging}>
      <span className="lv-gui-slider-head">
        <span>{setting.name}</span>
        <span className="lv-gui-slider-value">
          {Math.round(value)}
          {setting.unit ?? ''}
        </span>
      </span>
      <span className="lv-gui-slider-track" data-demo={`slider:${id}`}>
        <span className="lv-gui-slider-fill" style={{ width: `${pct}%` }} />
        <span className="lv-gui-slider-knob" data-demo={`knob:${id}`} style={{ left: `${pct}%` }} />
        <input
          type="range"
          min={setting.min}
          max={setting.max}
          value={value}
          aria-label={setting.name}
          onChange={(e) => dispatch({ type: 'set', key: id, value: Number(e.target.value) })}
        />
      </span>
    </label>
  )
}
