import { useMemo } from 'react'
import { ClickGuiWindow } from '@features/clickgui/ClickGuiWindow'
import { themeToCssVars } from '@features/clickgui/themeVars'
import { VoxelScene } from '@features/landing/effects/VoxelScene'
import { parseArgb, type ThemeEntry } from '../types'

type Props = {
  draft: ThemeEntry
  /** A theme card clicked inside the preview's Settings screen: load it into the draft, not the site. */
  onPickPreset: (id: string) => void
}

/** The live left pane: a themed ClickGui window plus two HUD tiles over the voxel scene, sized to fit its column. */
export function EditorPreview({ draft, onPickPreset }: Props) {
  const vars = useMemo(() => themeToCssVars(draft), [draft])
  const { r, g, b } = parseArgb(draft.theme.accent)

  return (
    <div className="lv-editor-preview relative flex min-h-0 flex-1 flex-col overflow-hidden rounded-panel border border-white/10">
      <VoxelScene accent={`rgb(${r}, ${g}, ${b})`} className="absolute inset-0" />
      <div className="relative flex min-h-0 flex-1 flex-col gap-3 p-3 sm:p-4" style={vars}>
        <div className="flex shrink-0 justify-center gap-3">
          <span className="lv-hud-tile lv-hud-tile-card" style={{ background: 'var(--gui-card-gradient)' }}>
            <b>FPS</b> 240
          </span>
          <span className="lv-hud-tile" style={{ background: 'var(--gui-surface-gradient)' }}>
            <b>XYZ</b> 12 · 64 · -8
          </span>
        </div>
        <div className="lv-editor-stage">
          <ClickGuiWindow theme={draft} demo={false} onPickTheme={onPickPreset} className="lv-editor-gui" />
        </div>
      </div>
    </div>
  )
}
