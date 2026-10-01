import { type RefObject, useEffect, useRef } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { NavLink } from 'react-router'
import { usePresence } from '@shared/motion/usePresence'
import { LanguageSwitch } from '../../i18n/LanguageSwitch'
import type { NavItem } from './NavPill'
import { AccountSlot } from './parts'

type Props = {
  id: string
  items: NavItem[]
  open: boolean
  onClose: () => void
  /** The menu button: focus returns there on close, and it stays inside the Tab loop. */
  toggleRef: RefObject<HTMLButtonElement | null>
}

const FOCUSABLE = 'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])'

/** The phone menu: a glass sheet under the bar with the nav, the language toggle and the account slot. */
export function MobileMenu({ id, items, open, onClose, toggleRef }: Props) {
  const { t } = useTranslation('common')
  const sheetRef = useRef<HTMLDivElement>(null)
  const { mounted, state } = usePresence(open, 220)

  useEffect(() => {
    if (!open) return
    const toggle = toggleRef.current
    sheetRef.current?.querySelector<HTMLElement>(FOCUSABLE)?.focus()
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault()
        onClose()
        toggle?.focus()
        return
      }
      if (e.key !== 'Tab' || !sheetRef.current) return
      const loop = [toggle, ...sheetRef.current.querySelectorAll<HTMLElement>(FOCUSABLE)].filter((el): el is HTMLElement => !!el)
      const first = loop[0]
      const last = loop[loop.length - 1]
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault()
        last.focus()
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault()
        first.focus()
      } else if (!loop.includes(document.activeElement as HTMLElement)) {
        e.preventDefault()
        first.focus()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose, toggleRef])

  return (
    <>
      {mounted && (
          <>
            <div
              aria-hidden="true"
              data-state={state}
              onClick={onClose}
              className="lv-scrim fixed inset-0 -z-10 bg-abyss/70 backdrop-blur-sm"
            />
            <div
              id={id}
              ref={sheetRef}
              data-state={state}
              className="lv-sheet mt-2 origin-top rounded-3xl border border-white/10 bg-[rgb(12_20_22/0.95)] p-2 shadow-[0_24px_60px_-20px_rgb(0_0_0/0.8),inset_0_1px_0_rgb(255_255_255/0.06)] backdrop-blur-xl"
            >
              <nav aria-label={t('nav.mainNav')}>
                <ul className="flex flex-col gap-1">
                  {items.map((item) => (
                    <li key={item.to}>
                      <NavLink
                        to={item.to}
                        onClick={onClose}
                        className={({ isActive }) =>
                          `flex items-center justify-between rounded-2xl px-4 py-3 text-base font-medium transition-colors ${
                            isActive
                              ? 'bg-ice/15 text-snow shadow-[inset_0_0_0_1px_color-mix(in_srgb,var(--color-ice)_45%,transparent)]'
                              : 'text-frost hover:bg-white/5 hover:text-snow'
                          }`
                        }
                      >
                        {({ isActive }) => (
                          <>
                            {item.label}
                            {isActive && <span aria-hidden="true" className="size-1.5 rounded-full bg-ice shadow-[0_0_8px_var(--color-ice)]" />}
                          </>
                        )}
                      </NavLink>
                    </li>
                  ))}
                </ul>
              </nav>
              <div className="mt-2 flex items-center justify-between gap-3 border-t border-white/10 px-2 pt-3 pb-1">
                <LanguageSwitch />
                <AccountSlot onNavigate={onClose} />
              </div>
            </div>
          </>
        )}
    </>
  )
}
