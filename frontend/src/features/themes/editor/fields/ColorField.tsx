import { type CSSProperties, useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { argbToCss, parseArgb } from '../../types'
import './fields.css'

type Props = { label: string; value: string; onChange: (hex: string) => void }

const pad2 = (n: number) => n.toString(16).padStart(2, '0')
const toRgb6 = (hex: string) => {
  const { r, g, b } = parseArgb(hex)
  return `#${pad2(r)}${pad2(g)}${pad2(b)}`.toUpperCase()
}
/** Rebuild `#AARRGGBB` from a `#RRGGBB` picker value, keeping the current alpha. */
const withAlpha = (hex: string, rgb6: string) => `#${pad2(parseArgb(hex).a)}${rgb6.slice(1)}`.toUpperCase()
/** Rebuild `#AARRGGBB` from an alpha (0–255), keeping the current rgb. */
const withRgb = (hex: string, a: number) => `#${pad2(a)}${toRgb6(hex).slice(1)}`.toUpperCase()

/** One editable palette slot: native color picker + alpha slider + hex field. */
export function ColorField({ label, value, onChange }: Props) {
  const { t: tr } = useTranslation('themes')
  const id = useId()
  // Local mirror so a partially-typed hex isn't clobbered mid-edit; reset when the value changes
  // for another reason (React's "adjust state during render" pattern).
  const [editing, setEditing] = useState<{ value: string; text: string }>({ value, text: value })
  const text = editing.value === value ? editing.text : value

  const commitText = () => {
    const t = text.trim()
    if (/^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.test(t)) onChange(`#${t.replace(/^#/, '').toUpperCase()}`)
    else setEditing({ value, text: value })
  }

  return (
    <label htmlFor={id} className="lv-color-field flex w-full min-w-0 items-center gap-2">
      <span className="lv-alpha-check lv-swatch-well size-9 shrink-0 overflow-hidden rounded-md">
        <span className="lv-color-swatch" style={{ background: argbToCss(value) }}>
          <input
            type="color"
            aria-label={label}
            value={toRgb6(value)}
            onChange={(e) => onChange(withAlpha(value, e.target.value))}
            className="size-full cursor-pointer border-0 bg-transparent p-0 opacity-0"
          />
        </span>
      </span>
      <span className="flex min-w-0 flex-1 flex-col gap-1">
        <span className="truncate text-xs text-[var(--gui-text-muted)]">{label}</span>
        <span className="flex min-w-0 items-center gap-2">
          <input
            type="range"
            min={0}
            max={255}
            aria-label={tr('editor.alpha', { name: label })}
            value={parseArgb(value).a}
            onChange={(e) => onChange(withRgb(value, Number(e.target.value)))}
            className="lv-alpha-slider lv-alpha-ramp h-2 min-w-0 flex-1 cursor-pointer appearance-none rounded-full"
            style={{ '--ramp': toRgb6(value) } as CSSProperties}
          />
          <input
            id={id}
            type="text"
            spellCheck={false}
            value={text}
            onChange={(e) => setEditing({ value, text: e.target.value })}
            onBlur={commitText}
            onKeyDown={(e) => e.key === 'Enter' && commitText()}
            className="lv-hex-input lv-editor-input w-[6.25rem] shrink-0 px-2 py-1 font-code text-xs"
          />
        </span>
      </span>
    </label>
  )
}
