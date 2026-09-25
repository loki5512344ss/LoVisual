import type { Dispatch } from 'react'
import { useTranslation } from 'react-i18next'
import type { GuiAction, GuiState } from './demo'
import { Chevron } from './parts/Glyphs'
import type { GuiModule } from './parts/modules'
import { SettingControls } from './parts/SettingControls'

type Props = { module: GuiModule; state: GuiState; dispatch: Dispatch<GuiAction> }

/** One module card: bullet title, description, divider, bind + settings chevron + status toggle. */
export function ModuleCard({ module, state, dispatch }: Props) {
  const { t } = useTranslation('clickgui')
  const { name, settings } = module
  const on = Boolean(state.enabled[name])
  const open = state.expanded === name && Boolean(settings)

  return (
    <article className="lv-gui-card" data-on={on} data-open={open}>
      <span className="lv-gui-card-glow" aria-hidden="true" />
      <h3 className="lv-gui-card-title">
        <span aria-hidden="true">• </span>
        {name}
      </h3>
      <p className="lv-gui-card-desc">{t(`desc.${name}`)}</p>
      <div className="lv-gui-card-divider" aria-hidden="true" />
      <div className="lv-gui-card-foot">
        <span className="lv-gui-bind">
          {t('bind')}: <span>{t('bindNone')}</span>
        </span>
        {settings ? (
          <button
            type="button"
            className="lv-gui-expand"
            aria-expanded={open}
            aria-label={t('settings', { name })}
            data-demo={`expand:${name}`}
            onClick={() => dispatch({ type: 'expand', module: name })}
          >
            <Chevron open={open} />
          </button>
        ) : (
          <span className="lv-gui-bind lv-gui-nosettings">{t('noSettings')}</span>
        )}
        <button
          type="button"
          role="switch"
          aria-checked={on}
          aria-label={t('toggle', { name })}
          data-demo={`toggle:${name}`}
          className="lv-gui-toggle"
          onClick={() => dispatch({ type: 'toggle', module: name })}
        >
          <span className="lv-gui-toggle-knob" />
        </button>
      </div>
      {open && settings ? <SettingControls module={name} settings={settings} state={state} dispatch={dispatch} /> : null}
    </article>
  )
}
