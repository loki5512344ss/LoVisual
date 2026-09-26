import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { useSession } from '../../../features/auth/session'
import type { Me } from '../../types'

/** The logo pill in the top bar; the in-game watermark's fps counter lives in the ClickGui replica. */
export function Watermark() {
  const { t } = useTranslation('common')
  return (
    <Link
      to="/"
      aria-label={t('nav.home')}
      className="group flex h-9 shrink-0 items-center gap-1.5 rounded-xl border border-white/10 bg-black/45 px-3 shadow-[inset_0_1px_0_rgb(255_255_255/0.06)] transition-colors hover:border-white/20"
    >
      <span className="font-display text-[15px] font-bold tracking-tight text-ice drop-shadow-[0_0_10px_var(--page-accent-glow)]">
        LoVisual
      </span>
    </Link>
  )
}

const SLOT = 'h-9 w-[8.5rem] shrink-0 rounded-full'

/** Login link or avatar + nick; a same-size skeleton while the session restores, so nothing jumps. */
export function AccountSlot({ className = '', onNavigate }: { className?: string; onNavigate?: () => void }) {
  const { t } = useTranslation('common')
  const { status, me } = useSession()
  if (status === 'loading') {
    return (
      <span role="status" aria-label={t('nav.sessionLoading')} className={`${SLOT} flex items-center gap-2 border border-white/5 bg-white/[0.04] px-1.5 ${className}`}>
        <span className="size-6 animate-pulse rounded-full bg-white/10" />
        <span className="h-2.5 flex-1 animate-pulse rounded-full bg-white/10" />
      </span>
    )
  }
  if (!me) {
    return (
      <Link
        to="/login"
        onClick={onNavigate}
        className={`${SLOT} flex items-center justify-center border border-white/12 bg-white/[0.04] text-sm font-semibold text-snow transition-colors hover:border-white/25 hover:bg-white/[0.08] ${className}`}
      >
        {t('nav.login')}
      </Link>
    )
  }
  return (
    <Link
      to="/account"
      onClick={onNavigate}
      className={`${SLOT} flex min-w-0 items-center gap-2 border border-white/10 bg-white/[0.04] pl-1 pr-3 text-sm text-snow transition-colors hover:border-white/25 ${className}`}
    >
      <Avatar me={me} />
      <span className="min-w-0 truncate">{me.display_nick}</span>
    </Link>
  )
}

function Avatar({ me }: { me: Me }) {
  if (me.avatar_url) return <img src={me.avatar_url} alt="" className="size-7 shrink-0 rounded-full object-cover" />
  return (
    <span aria-hidden="true" className="flex size-7 shrink-0 items-center justify-center rounded-full bg-ice/20 font-display text-xs font-bold text-ice">
      {me.display_nick.charAt(0).toUpperCase()}
    </span>
  )
}

export function DownloadIcon({ className = 'size-4' }: { className?: string }) {
  return (
    <svg viewBox="0 0 20 20" className={className} aria-hidden="true">
      <path d="M10 3v10m0 0-4-4m4 4 4-4M4 16h12" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

/** Three bars that fold into a cross while the menu is open. */
export function MenuIcon({ open }: { open: boolean }) {
  const bar = 'absolute left-0 h-[2px] w-full rounded-full bg-current transition-transform duration-300'
  return (
    <span aria-hidden="true" className="relative block h-3 w-4">
      <span className={`${bar} top-0 ${open ? 'translate-y-[5px] rotate-45' : ''}`} />
      <span className={`${bar} top-[5px] transition-opacity ${open ? 'opacity-0' : ''}`} />
      <span className={`${bar} top-[10px] ${open ? '-translate-y-[5px] -rotate-45' : ''}`} />
    </span>
  )
}
