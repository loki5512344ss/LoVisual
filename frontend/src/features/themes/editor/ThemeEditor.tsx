import { type ReactNode, useEffect, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { themeToCssVars } from '@features/clickgui/themeVars'
import { PRESETS } from '../presets.generated'
import type { ThemeEntry } from '../types'
import { EditorPreview } from './EditorPreview'
import { useThemeHistory } from './model/useThemeHistory'
import { ExportActions } from './panel/ExportActions'
import { PaletteTabs } from './panel/PaletteTabs'
import { PanelHeader } from './panel/PanelHeader'
import { PresetPicker } from './panel/PresetPicker'

/** Pack the draft name into a slug id, mirroring the codec's `slug`. */
const nameToId = (name: string) => name.trim().toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') || 'custom'

/**
 * `intro` (page title + lead) sits on top of the controls column so the whole editor fits one screen.
 * The controls column is itself a ClickGui window painted with the draft, so every edit shows up
 * in the tool you are holding, not only in the preview.
 */
export function ThemeEditor({ initial, intro }: { initial: ThemeEntry; intro?: ReactNode }) {
  const { t } = useTranslation('themes')
  const { draft, set, reset, undo, redo, canUndo, canRedo } = useThemeHistory(initial)
  const vars = useMemo(() => themeToCssVars(draft), [draft])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (!(e.ctrlKey || e.metaKey) || e.key.toLowerCase() !== 'z') return
      e.preventDefault()
      if (e.shiftKey) redo()
      else undo()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [undo, redo])

  const pickPreset = (id: string) => {
    const preset = PRESETS.find((p) => p.id === id)
    if (preset) reset(preset)
  }

  return (
    <div className="grid grid-cols-[minmax(0,1fr)] gap-6 py-6 [grid-template-areas:'intro'_'preview'_'controls'] lg:h-[calc(100svh-var(--topbar-h))] lg:min-h-[560px] lg:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)] lg:grid-rows-[auto_minmax(0,1fr)] lg:gap-x-8 lg:gap-y-4 lg:py-4 lg:[grid-template-areas:'preview_intro'_'preview_controls']">
      {intro && <div className="[grid-area:intro]">{intro}</div>}
      <div className="flex min-h-0 flex-col [grid-area:preview]">
        <EditorPreview draft={draft} onPickPreset={pickPreset} />
      </div>

      <section aria-label={t('editor.panel')} className="lv-editor-panel [grid-area:controls]" style={vars}>
        <PanelHeader
          draft={draft}
          onRename={(name) => set({ ...draft, name, id: nameToId(name) })}
          history={{ undo, redo, canUndo, canRedo, reset: () => reset(initial), resetName: initial.name }}
        />
        <div className="lv-editor-scroll flex min-h-0 flex-1 flex-col gap-5 p-4 lg:overflow-y-auto">
          <PresetPicker activeId={draft.id} onPick={pickPreset} />
          <PaletteTabs draft={draft} onChange={set} />
        </div>
        <ExportActions draft={draft} onImport={reset} />
      </section>
    </div>
  )
}
