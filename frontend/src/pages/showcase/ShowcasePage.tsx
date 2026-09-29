import { useTranslation } from 'react-i18next'
import { ParticleField } from '@features/landing/effects/ParticleField'
import { VoxelScene } from '@features/landing/effects/VoxelScene'
import { ListingList } from '@features/showcase/components/ListingList'
import { PageTitle } from '@shared/layout/PageTitle'

const ACCENT = 'rgb(52, 211, 153)'

export default function ShowcasePage() {
  const { t } = useTranslation('showcase')
  return (
    <section className="lv-bleed relative isolate -mt-(--topbar-h) flex min-h-svh flex-col gap-8 overflow-hidden pt-(--topbar-h) pb-16">
      <div aria-hidden="true" className="absolute inset-0 -z-10 opacity-40">
        <VoxelScene accent={ACCENT} />
      </div>
      <div aria-hidden="true" className="absolute inset-0 -z-10 bg-abyss/70" />
      <ParticleField color={ACCENT} className="opacity-50" />

      <div className="relative mx-auto flex w-full max-w-[1120px] flex-col gap-8 px-4 pt-12 sm:px-6">
        <PageTitle lead={t('page.lead')}>{t('page.title')}</PageTitle>
        <ListingList />
      </div>
    </section>
  )
}
