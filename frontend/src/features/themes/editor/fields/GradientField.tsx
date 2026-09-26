import { useTranslation } from 'react-i18next'
import type { GradientSpec } from '../../types'
import { ColorField } from './ColorField'

type Props = { title: string; value: GradientSpec; onChange: (patch: Partial<GradientSpec>) => void }

/** A gradient slot: enable toggle, start/end colors and a 0–360° angle control. */
export function GradientField({ title, value, onChange }: Props) {
  const { t } = useTranslation('themes')

  return (
    <div className="lv-gradient-field flex min-w-0 flex-col gap-3 rounded-panel border border-white/10 bg-black/25 p-3">
      <label className="flex items-center justify-between gap-2">
        <span className="text-sm font-semibold text-snow">{title}</span>
        <span className="flex items-center gap-2 text-xs text-frost">
          {t('editor.enabled')}
          <input
            type="checkbox"
            aria-label={`${title} · ${t('editor.enabled')}`}
            checked={value.enabled}
            onChange={(e) => onChange({ enabled: e.target.checked })}
            className="size-4 accent-[var(--color-ice)]"
          />
        </span>
      </label>

      <ColorField label={t('editor.start')} value={value.start} onChange={(start) => onChange({ start })} />
      {value.enabled && (
        <>
          <ColorField label={t('editor.end')} value={value.end} onChange={(end) => onChange({ end })} />
          <label className="flex items-center gap-2 text-xs text-frost">
            <span className="w-12 shrink-0">{t('editor.angle')}</span>
            <input
              type="range"
              min={0}
              max={359}
              value={Math.round(value.angle)}
              onChange={(e) => onChange({ angle: Number(e.target.value) })}
              className="h-1.5 flex-1 cursor-pointer appearance-none rounded-full bg-white/20"
            />
            <span className="w-10 shrink-0 text-right font-code text-snow">{Math.round(value.angle)}°</span>
          </label>
        </>
      )}
    </div>
  )
}
