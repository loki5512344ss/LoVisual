import { type KeyboardEvent, useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { GradientSpec, ThemeColors, ThemeEntry } from '../../types'
import { ColorField } from '../fields/ColorField'
import { GradientField } from '../fields/GradientField'
import { GRADIENT_SLOTS } from '../model/codec'

// The 12 slots grouped by the part of the menu they paint. Together they cover every
// COLOR_SLOTS entry once; the export order is still the codec's, this is display only.
const GROUPS: { key: 'window' | 'modules' | 'text'; slots: (keyof ThemeColors)[] }[] = [
  { key: 'window', slots: ['windowBg', 'windowHeader', 'windowStroke', 'strokeSoft'] },
  { key: 'modules', slots: ['surface', 'surfaceHover', 'cardEnabled', 'cardDisabled'] },
  { key: 'text', slots: ['textPrimary', 'textMuted', 'accent', 'accentSoft'] },
]

const TABS = ['colors', 'gradients'] as const
type Tab = (typeof TABS)[number]

type Props = { draft: ThemeEntry; onChange: (next: ThemeEntry) => void }

/** Flat colors and gradients behind a two-way switch shaped like the mod's own tab pill. */
export function PaletteTabs({ draft, onChange }: Props) {
  const { t } = useTranslation('themes')
  const [tab, setTab] = useState<Tab>('colors')
  const base = useId()

  const setColor = (slot: keyof ThemeColors, hex: string) => onChange({ ...draft, theme: { ...draft.theme, [slot]: hex } })
  const setGradient = (key: keyof ThemeEntry, patch: Partial<GradientSpec>) =>
    onChange({ ...draft, [key]: { ...(draft[key] as GradientSpec), ...patch } })

  const onKey = (e: KeyboardEvent) => {
    if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return
    const next = tab === 'colors' ? 'gradients' : 'colors'
    setTab(next)
    document.getElementById(`${base}-${next}-tab`)?.focus()
  }

  return (
    <div className="flex flex-col gap-4">
      <div role="tablist" aria-label={t('editor.palette')} className="lv-editor-tabs" data-tab={tab} onKeyDown={onKey}>
        {TABS.map((key) => (
          <button
            key={key}
            id={`${base}-${key}-tab`}
            type="button"
            role="tab"
            aria-selected={tab === key}
            aria-controls={`${base}-${key}`}
            tabIndex={tab === key ? 0 : -1}
            onClick={() => setTab(key)}
          >
            {t(`editor.${key}`)}
          </button>
        ))}
      </div>

      <div id={`${base}-colors`} role="tabpanel" aria-labelledby={`${base}-colors-tab`} hidden={tab !== 'colors'} className="flex flex-col gap-5">
        {GROUPS.map((group) => (
          <fieldset key={group.key} className="flex min-w-0 flex-col gap-2.5">
            <legend className="lv-editor-group">{t(`editor.group.${group.key}`)}</legend>
            <div className="grid gap-x-4 gap-y-2.5 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
              {group.slots.map((slot) => (
                <ColorField key={slot} label={t(`colors.${slot}`)} value={draft.theme[slot]} onChange={(hex) => setColor(slot, hex)} />
              ))}
            </div>
          </fieldset>
        ))}
      </div>

      <div id={`${base}-gradients`} role="tabpanel" aria-labelledby={`${base}-gradients-tab`} hidden={tab !== 'gradients'} className="flex flex-col gap-3">
        {GRADIENT_SLOTS.map((g) => (
          <GradientField
            key={g.prefix}
            title={t(`editor.gradient.${g.prefix}`)}
            value={draft[g.key] as GradientSpec}
            onChange={(patch) => setGradient(g.key, patch)}
          />
        ))}
      </div>
    </div>
  )
}
