import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { useSession } from '@features/auth/session'
import type { Me } from '../../types'

/** The pixel-heart mark, standalone (no badge background) — same shape as public/favicon.svg. */
function HeartMark({ className = 'size-6' }: { className?: string }) {
  return (
    <svg viewBox="0 0 31 27" className={className} shapeRendering="crispEdges" aria-hidden="true">
      <g>
        <path fill="#be185d" d="M12 0 12 4 14.9 6.9 14.9 2.9Z" />
        <path fill="#be185d" d="M24 0 24 4 26.9 6.9 26.9 2.9Z" />
        <path fill="#be185d" d="M28 4 28 12 30.9 14.9 30.9 6.9Z" />
        <path fill="#be185d" d="M24 12 24 16 26.9 18.9 26.9 14.9Z" />
        <path fill="#be185d" d="M20 16 20 20 22.9 22.9 22.9 18.9Z" />
        <path fill="#be185d" d="M16 20 16 24 18.9 26.9 18.9 22.9Z" />
        <path fill="#701a3e" d="M28 12 24 12 26.9 14.9 30.9 14.9Z" />
        <path fill="#701a3e" d="M24 16 20 16 22.9 18.9 26.9 18.9Z" />
        <path fill="#701a3e" d="M20 20 16 20 18.9 22.9 22.9 22.9Z" />
        <path fill="#701a3e" d="M16 24 12 24 14.9 26.9 18.9 26.9Z" />
        <path fill="#701a3e" d="M12 20 8 20 10.9 22.9 14.9 22.9Z" />
        <path fill="#701a3e" d="M8 16 4 16 6.9 18.9 10.9 18.9Z" />
        <path fill="#701a3e" d="M4 12 0 12 2.9 14.9 6.9 14.9Z" />
        <path
          fill="#f472b6"
          d="M4 0 12 0 12 4 16 4 16 0 24 0 24 4 28 4 28 12 24 12 24 16 20 16 20 20 16 20 16 24 12 24 12 20 8 20 8 16 4 16 4 12 0 12 0 4 4 4Z"
        />
        <path fill="#701a3e" d="M4 4H8V8H4Z" />
        <path fill="#701a3e" d="M4 8H12V12H4Z" />
        <path fill="#701a3e" d="M8 12H12V16H8Z" />
        <path fill="#701a3e" d="M20 4H24V8H20Z" />
        <path fill="#701a3e" d="M16 8H24V12H16Z" />
        <path fill="#701a3e" d="M16 12H20V16H16Z" />
      </g>
    </svg>
  )
}

/** The logo mark in the top bar; the in-game watermark's fps counter lives in the ClickGui replica. */
export function Watermark() {
  const { t } = useTranslation('common')
  return (
    <Link to="/" aria-label={t('nav.home')} className="group flex h-9 shrink-0 items-center gap-2 px-1">
      <HeartMark />
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
