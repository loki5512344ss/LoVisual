import { useTranslation } from 'react-i18next'
import type { ThemeEntry } from '../../types'

type History = { undo: () => void; redo: () => void; reset: () => void; resetName: string; canUndo: boolean; canRedo: boolean }
type Props = { draft: ThemeEntry; onRename: (name: string) => void; history: History }

const ICONS = {
  undo: 'M9 14 4 9l5-5M4 9h10.5a5.5 5.5 0 0 1 0 11H11',
  redo: 'm15 14 5-5-5-5M20 9H9.5a5.5 5.5 0 0 0 0 11H13',
  reset: 'M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5',
} as const

/**
 * The panel's title bar, painted with the draft's header gradient like the mod's window header.
 * The theme name is the title itself and is edited in place; history controls sit on the right.
 */
export function PanelHeader({ draft, onRename, history }: Props) {
  const { t } = useTranslation('themes')
  const tools = [
    { key: 'undo', label: t('editor.undo'), keys: 'Control+Z', run: history.undo, enabled: history.canUndo },
    { key: 'redo', label: t('editor.redo'), keys: 'Control+Shift+Z', run: history.redo, enabled: history.canRedo },
    { key: 'reset', label: t('editor.reset', { name: history.resetName }), keys: undefined, run: history.reset, enabled: true },
  ] as const

  return (
    <div className="lv-editor-head">
      <span className="lv-editor-dot" aria-hidden="true" />
      <input
        value={draft.name}
        onChange={(e) => onRename(e.target.value)}
        aria-label={t('editor.nameLabel')}
        title={t('editor.nameLabel')}
        spellCheck={false}
        className="lv-editor-name"
      />
      <div className="flex shrink-0 items-center gap-1">
        {tools.map((tool) => (
          <button
            key={tool.key}
            type="button"
            onClick={tool.run}
            disabled={!tool.enabled}
            aria-label={tool.label}
            aria-keyshortcuts={tool.keys}
            title={tool.keys ? `${tool.label} (${tool.keys.replace('Control', 'Ctrl')})` : tool.label}
            className="lv-editor-tool"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d={ICONS[tool.key]} />
            </svg>
          </button>
        ))}
      </div>
    </div>
  )
}
