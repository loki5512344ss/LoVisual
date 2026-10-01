import { useCallback, useRef, useState, type DragEvent } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { useNavigate } from 'react-router'
import { Button } from '@shared/ui/Button'
import { useSession } from '@features/auth/session'
import { AvatarUpload } from './avatar/AvatarUpload'

/** The mod's profile panel: big avatar with an accent ring, nick, email and join date. The avatar circle doubles as a file drop target. */
export function ProfileCard() {
  const { t } = useTranslation('account')
  const { me, signOut } = useSession()
  const navigate = useNavigate()
  const [dragging, setDragging] = useState(false)
  // AvatarUpload owns pick/cropper state; the drop handler just forwards the file into its validation path.
  const acceptFile = useRef<(file: File) => void>(() => {})
  // Stable handlers so AvatarUpload's expose-effect does not re-run on every parent render.
  const handleReady = useCallback((accept: (file: File) => void) => {
    acceptFile.current = accept
  }, [])

  if (!me) return null

  const joined = new Intl.DateTimeFormat('ru-RU', { dateStyle: 'long' }).format(new Date(me.created_at))

  async function handleSignOut() {
    await signOut()
    navigate('/')
  }

  function onDrop(e: DragEvent<HTMLDivElement>) {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files?.[0]
    if (file) acceptFile.current(file)
  }

  return (
    <div className="flex flex-col items-center gap-4 rounded-(--radius-panel) border border-shoal bg-slate/70 p-6 text-center">
      <div
        data-testid="avatar-drop"
        className={`relative rounded-full ${dragging ? 'ring-2 ring-ice' : ''}`}
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
      >
        <div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full border-2 border-ice bg-abyss font-display text-3xl">
          {me.avatar_url ? (
            <img src={me.avatar_url} alt="" className="h-full w-full object-cover" />
          ) : (
            <span aria-hidden="true">{me.display_nick.slice(0, 1).toUpperCase()}</span>
          )}
        </div>
      </div>
      <AvatarUpload onReady={handleReady} />
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
