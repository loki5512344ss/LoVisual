import { memo } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { cssAngle } from '@features/clickgui/themeVars'
import { argbToCss, type GradientSpec } from '../../types'
import { ColorField } from './ColorField'

type Props = { title: string; value: GradientSpec; onChange: (patch: Partial<GradientSpec>) => void }

/**
 * A gradient slot. Its title bar is the gradient itself (at the mod's angle), so every tweak
 * below is visible right where it is made; the switch turns the slot flat or back.
 */
export const GradientField = memo(function GradientField({ title, value, onChange }: Props) {
  const { t } = useTranslation('themes')
  const strip = value.enabled
    ? `linear-gradient(${cssAngle(value.angle)}deg, ${argbToCss(value.start)}, ${argbToCss(value.end)})`
    : argbToCss(value.start)

  return (
    <div className="lv-gradient-field" data-on={value.enabled}>
      <label className="lv-gradient-strip lv-alpha-check">
        <span className="lv-gradient-paint" style={{ background: strip }} aria-hidden="true" />
        <span className="lv-gradient-title">{title}</span>
        <input
          type="checkbox"
          role="switch"
          aria-label={t('editor.enabledFor', { name: title })}
          checked={value.enabled}
          onChange={(e) => onChange({ enabled: e.target.checked })}
          className="lv-switch"
        />
      </label>

      <div className="flex flex-col gap-2.5 p-3">
        <ColorField label={t('editor.start')} value={value.start} onChange={(start) => onChange({ start })} />
        {value.enabled && (
          <>
            <ColorField label={t('editor.end')} value={value.end} onChange={(end) => onChange({ end })} />
            <label className="flex items-center gap-2 text-xs text-[var(--gui-text-muted)]">
              <span className="w-12 shrink-0">{t('editor.angle')}</span>
              <input
                type="range"
                min={0}
                max={359}
                value={Math.round(value.angle)}
                onChange={(e) => onChange({ angle: Number(e.target.value) })}
                className="lv-alpha-slider lv-track h-1.5 flex-1 cursor-pointer appearance-none rounded-full"
              />
              <span className="w-10 shrink-0 text-right font-code text-[var(--gui-text)]">{Math.round(value.angle)}°</span>
            </label>
          </>
        )}
      </div>
    </div>
  )
})
