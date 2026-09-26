import { useParams } from 'react-router'
import { PublicProfileView } from '../../features/profile/components/PublicProfileView'

export default function ProfilePage() {
  const { id = '' } = useParams()
  return (
    <section className="py-12">
      <PublicProfileView id={id} />
    </section>
  )
}
