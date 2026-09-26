import { useParams } from 'react-router'
import { ParticleField } from '../../features/landing/effects/ParticleField'
import { VoxelScene } from '../../features/landing/effects/VoxelScene'
import { ListingDetail } from '../../features/showcase/components/ListingDetail'

const ACCENT = 'rgb(92, 200, 231)'

export default function ListingPage() {
  const { id } = useParams<{ id: string }>()
  return (
    <section className="lv-bleed relative isolate -mt-(--topbar-h) flex min-h-svh flex-col gap-8 overflow-hidden pt-(--topbar-h) pb-16">
      <div aria-hidden="true" className="absolute inset-0 -z-10 opacity-40">
        <VoxelScene accent={ACCENT} />
      </div>
      <div aria-hidden="true" className="absolute inset-0 -z-10 bg-abyss/70" />
      <ParticleField color={ACCENT} className="opacity-50" />

      <div className="relative mx-auto w-full max-w-[720px] px-4 pt-12 sm:px-6">{id ? <ListingDetail id={id} /> : null}</div>
    </section>
  )
}
