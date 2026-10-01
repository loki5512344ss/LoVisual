import type { Dispatch } from 'react'
import { useTranslation } from '@shared/i18n/react'
import type { GuiAction, GuiState } from '../demo'
import { Icon } from '../parts/Glyphs'
import { type Category, CATEGORIES } from '../parts/modules'

type Props = { state: GuiState; dispatch: Dispatch<GuiAction> }

/** `ModulesMenuScreen`: the four liquid-glass category panels, side by side. */
export function ModulesScreen({ state, dispatch }: Props) {
  const { t } = useTranslation('clickgui')
  return (
    <div className="lv-gui-panels" role="list" aria-label={t('categories')}>
      {CATEGORIES.map((c) => (
        <Panel key={c.id} category={c} state={state} dispatch={dispatch} />
      ))}
    </div>
  )
}

function Panel({ category, state, dispatch }: { category: Category } & Props) {
  const { t } = useTranslation('clickgui')
  const titleId = `lv-gui-panel-${category.id}`
  return (
    <div role="listitem" className="lv-gui-panel" data-category={category.id} aria-labelledby={titleId}>
      <header className="lv-gui-panel-head">
        <h2 id={titleId} className="lv-gui-panel-title">
          {category.name}
        </h2>
        <Icon name={category.id} className="lv-gui-panel-icon" />
      </header>
      <ul className="lv-gui-rows">
        {category.modules.map((m) => {
          const on = Boolean(state.enabled[m.name])
          return (
            <li key={m.name}>
              <button
                type="button"
                className="lv-gui-row"
                data-on={on}
                data-demo={`module:${m.name}`}
                aria-pressed={on}
                title={t('module', { name: m.name })}
                onClick={() => dispatch({ type: 'toggle', module: m.name })}
                onContextMenu={(e) => e.preventDefault()}
              >
                <span className="lv-gui-row-name">{m.name}</span>
                {on && <Icon name="check" className="lv-gui-row-check" />}
              </button>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
