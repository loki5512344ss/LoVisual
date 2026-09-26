import { useTranslation } from 'react-i18next'
import { ParticleField } from '../../features/landing/effects/ParticleField'
import { VoxelScene } from '../../features/landing/effects/VoxelScene'
import { SlotGrid } from '../../features/configs/components/SlotGrid'

const ACCENT = 'rgb(92, 200, 231)'

export default function ConfigsPage() {
  const { t } = useTranslation('configs')
  return (
    <section className="lv-bleed relative isolate -mt-(--topbar-h) flex min-h-svh flex-col gap-8 overflow-hidden pt-(--topbar-h) pb-16">
      <div aria-hidden="true" className="absolute inset-0 -z-10 opacity-40">
        <VoxelScene accent={ACCENT} />
      </div>
      <div aria-hidden="true" className="absolute inset-0 -z-10 bg-abyss/70" />
      <ParticleField color={ACCENT} className="opacity-50" />

      <div className="relative mx-auto flex w-full max-w-[1120px] flex-col gap-8 px-4 pt-12 sm:px-6">
        <div>
          <h1 className="font-display text-3xl text-snow">{t('page.title')}</h1>
          <p className="mt-2 max-w-[60ch] text-frost">
            {t('page.leadBefore')} <code className="font-code text-ice">%config save &lt;слот&gt;</code>
            {t('page.leadAfter')}
          </p>
        </div>
        <SlotGrid />
      </div>
    </section>
  )
}
