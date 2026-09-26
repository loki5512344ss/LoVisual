import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button } from '../../../shared/ui/Button'
import { useSession } from '../../auth/session'
import { AvatarUpload } from './AvatarUpload'

/** The mod's profile panel: big avatar with an accent ring, nick, email and join date. */
export function ProfileCard() {
  const { t } = useTranslation('account')
  const { me, signOut } = useSession()
  const navigate = useNavigate()

  if (!me) return null

  const joined = new Intl.DateTimeFormat('ru-RU', { dateStyle: 'long' }).format(new Date(me.created_at))

  async function handleSignOut() {
    await signOut()
    navigate('/')
  }

  return (
    <div className="flex flex-col items-center gap-4 rounded-(--radius-panel) border border-shoal bg-slate/70 p-6 text-center">
      <div className="relative">
        <div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full border-2 border-ice bg-abyss font-display text-3xl">
          {me.avatar_url ? (
            <img src={me.avatar_url} alt="" className="h-full w-full object-cover" />
          ) : (
            <span aria-hidden="true">{me.display_nick.slice(0, 1).toUpperCase()}</span>
          )}
        </div>
      </div>
      <AvatarUpload />
      <div>
        <p className="font-display text-xl">{me.display_nick}</p>
        <p className="text-sm text-frost">{me.email}</p>
        <p className="mt-1 text-sm text-frost">{t('profile.memberSince', { date: joined })}</p>
      </div>
      <Button variant="quiet" onClick={handleSignOut}>
        {t('profile.signOut')}
      </Button>
    </div>
  )
}
