import { useMemo } from 'react'
import { ClickGuiWindow } from '../../clickgui/ClickGuiWindow'
import { themeToCssVars } from '../../clickgui/themeVars'
import { VoxelScene } from '../../landing/effects/VoxelScene'
import { parseArgb, type ThemeEntry } from '../types'

/** The live left pane: a themed ClickGui window plus two HUD tiles over the voxel scene. */
export function EditorPreview({ draft }: { draft: ThemeEntry }) {
  const vars = useMemo(() => themeToCssVars(draft), [draft])
  const { r, g, b } = parseArgb(draft.theme.accent)

  return (
    <div className="lv-editor-preview relative min-h-[420px] overflow-hidden rounded-panel border border-white/10">
      <VoxelScene accent={`rgb(${r}, ${g}, ${b})`} className="absolute inset-0" />
      <div className="relative flex h-full flex-col items-center justify-center gap-6 p-6" style={vars}>
        <div className="flex w-full justify-center gap-4">
          <span className="lv-hud-tile" style={{ background: 'var(--gui-card-gradient)' }}>
            <b>FPS</b> 240
          </span>
          <span className="lv-hud-tile" style={{ background: 'var(--gui-surface-gradient)' }}>
            <b>XYZ</b> 12 · 64 · -8
          </span>
        </div>
        <ClickGuiWindow theme={draft} demo={false} className="w-full max-w-[520px]" />
      </div>
    </div>
  )
}
