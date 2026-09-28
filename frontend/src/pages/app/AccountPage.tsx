import { DeviceList } from '@features/account/components/DeviceList'
import { ProfileCard } from '@features/account/components/ProfileCard'

export default function AccountPage() {
  return (
    <section className="grid grid-cols-1 gap-6 py-12 min-[900px]:grid-cols-[320px_1fr]">
      <ProfileCard />
      <DeviceList />
    </section>
  )
}
