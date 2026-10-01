import type { CSSProperties } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { themeToCssVars } from '@features/clickgui/themeVars'
import { PRESETS } from '../../presets.generated'

// Each chip's swatch is painted with its own preset, so the list reads as a palette before anything is
// clicked. The chip's text and outline still follow the draft, like the rest of the panel.
// A flat color can only be the last background layer, so the card is always passed as a gradient.
const asLayer = (paint: string) => (paint.includes('gradient(') ? paint : `linear-gradient(${paint}, ${paint})`)

const CHIPS = PRESETS.map((p) => {
  const v = themeToCssVars(p)
  const vars = {
    '--chip-window': v['--gui-window-gradient'],
    '--chip-card': asLayer(v['--gui-card-gradient']),
    '--chip-accent': v['--gui-accent'],
  }
  return { id: p.id, name: p.name, vars: vars as CSSProperties }
})

type Props = { activeId: string; onPick: (id: string) => void }

/** The mod's built-in themes as starting points: a tiny window swatch plus the name. */
export function PresetPicker({ activeId, onPick }: Props) {
  const { t } = useTranslation('themes')

  return (
    <ul aria-label={t('editor.presets')} className="flex flex-wrap gap-1.5">
      {CHIPS.map((chip) => (
        <li key={chip.id}>
          <button
            type="button"
            onClick={() => onPick(chip.id)}
            aria-pressed={chip.id === activeId}
            className="lv-preset-chip"
            style={chip.vars}
          >
            <span className="lv-preset-mini" aria-hidden="true" />
            {chip.name}
          </button>
        </li>
      ))}
    </ul>
  )
}
