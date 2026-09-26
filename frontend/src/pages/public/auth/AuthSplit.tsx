import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { ClickGuiWindow } from '../../../features/clickgui/ClickGuiWindow'
import { ParticleField } from '../../../features/landing/effects/ParticleField'
import { VoxelScene } from '../../../features/landing/effects/VoxelScene'
import { useActiveTheme } from '../../../features/themes/useActiveTheme'
import { parseArgb } from '../../../features/themes/types'

/** Two-pane auth layout: a ClickGui-styled glass form panel, and a decorative scene (hidden < 900px). */
export function AuthSplit({ title, children }: { title: string; children: ReactNode }) {
  const { t } = useTranslation('auth')
  const { theme } = useActiveTheme()
  const { r, g, b } = parseArgb(theme.theme.accent)
  const particleColor = `rgb(${r}, ${g}, ${b})`

  return (
    <div className="flex min-h-[calc(100vh-var(--topbar-h)-4rem)] items-center justify-center py-10 min-[900px]:justify-between min-[900px]:gap-12">
      <div className="w-full max-w-md rounded-(--radius-panel) border border-shoal bg-slate/80 shadow-[0_0_60px_-20px_var(--page-accent-glow,transparent)] backdrop-blur">
        <div className="flex items-center gap-2 border-b border-shoal px-5 py-4">
          <span aria-hidden="true" className="h-2.5 w-2.5 rounded-full bg-ice" />
          <h1 className="font-display text-xl">{title}</h1>
        </div>
        <div className="px-5 py-6">{children}</div>
      </div>

      <div className="relative hidden min-[900px]:flex min-[900px]:h-[520px] min-[900px]:w-[440px] min-[900px]:shrink-0 min-[900px]:items-end min-[900px]:overflow-hidden min-[900px]:rounded-(--radius-panel) min-[900px]:border min-[900px]:border-shoal">
        <VoxelScene accent={particleColor} />
        <ParticleField color={particleColor} className="mix-blend-screen" />
        <div className="relative z-10 w-full p-5">
          <ClickGuiWindow demo className="w-full max-w-[300px] drop-shadow-2xl" />
          <p className="mt-4 max-w-[300px] font-display text-sm text-snow drop-shadow">{t('splitPitch')}</p>
        </div>
      </div>
    </div>
  )
}
