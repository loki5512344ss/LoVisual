import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, NavLink } from 'react-router'
import { useSession } from '../../features/auth/session'
import { LanguageSwitch } from '../i18n/LanguageSwitch'
import type { Me } from '../types'
import { ButtonLink } from '../ui/Button'

export function TopBar() {
  const { t } = useTranslation(['common', 'download'])
  const [open, setOpen] = useState(false)
  const nav = [
    { to: '/showcase', label: t('nav.showcase') },
    { to: '/configs', label: t('nav.myConfigs') },
    { to: '/link', label: t('nav.linkGame') },
  ]

  return (
    <header className="border-b border-shoal bg-slate">
      <div className="mx-auto flex w-full max-w-[1120px] flex-wrap items-center gap-x-6 px-4 sm:px-6">
        <Link to="/" className="py-4 font-display text-xl font-bold text-snow">
          LoVisual
        </Link>
        <nav
          id="site-nav"
          aria-label={t('nav.mainNav')}
          className={`${open ? 'flex' : 'hidden'} order-last w-full flex-col gap-1 pb-3 sm:order-none sm:flex sm:w-auto sm:flex-row sm:gap-6 sm:pb-0`}
        >
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                `py-2 text-sm transition-colors ${isActive ? 'text-snow' : 'text-frost hover:text-snow'}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="ml-auto flex items-center gap-3">
          <ButtonLink to="/download" variant="primary">
            {t('download:nav')}
          </ButtonLink>
          <LanguageSwitch />
          <button
            type="button"
            aria-expanded={open}
            aria-controls="site-nav"
            onClick={() => setOpen((v) => !v)}
            className="rounded-md px-2 py-2 text-sm text-frost hover:text-snow sm:hidden"
          >
            {t('nav.menu')}
          </button>
          <AccountButton />
        </div>
      </div>
    </header>
  )
}

function AccountButton() {
  const { t } = useTranslation('common')
  const { status, me } = useSession()
  if (status === 'loading') return null
  if (!me) {
    return (
      <ButtonLink to="/login" variant="quiet">
        {t('nav.login')}
      </ButtonLink>
    )
  }
  return (
    <Link to="/account" className="flex min-w-0 items-center gap-2 py-2 text-sm text-snow hover:text-ice">
      <Avatar me={me} />
      <span className="max-w-[8rem] truncate sm:max-w-[12rem]">{me.display_nick}</span>
    </Link>
  )
}

function Avatar({ me }: { me: Me }) {
  if (me.avatar_url) {
    return <img src={me.avatar_url} alt="" className="size-7 shrink-0 rounded-full object-cover" />
  }
  return (
    <span
      aria-hidden="true"
      className="flex size-7 shrink-0 items-center justify-center rounded-full bg-shoal font-display text-xs font-bold text-snow"
    >
      {me.display_nick.charAt(0).toUpperCase()}
    </span>
  )
}
