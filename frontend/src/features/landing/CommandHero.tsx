import { useEffect, useRef, useState } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { prefersReducedMotion } from '@shared/motion/reducedMotion'
import { ButtonLink } from '@shared/ui/Button'
import { useSession } from '../auth/session/useSession'
import { useInView } from './effects/useInView'
import '@features/landing/styles/sections.css'

// A placeholder code, not a plausible real one — this types out a UI demo, not live data.
const COMMAND = '%config load ••••••••'
const TYPE_DELAY_MS = 60

/** Cloud configs, told through the one thing a player actually does: a command in the game chat. */
export function CommandHero() {
  const { t } = useTranslation('landing')
  const { status } = useSession()

  return (
    <section aria-labelledby="cloud-title" className="relative flex flex-col gap-8 pb-16 pt-12 sm:pt-20">
      <ChatBar />
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,0.9fr)] lg:gap-14">
        <h2 id="cloud-title" className="text-[1.75rem] leading-[1.1] sm:text-[2.4rem]">{t('cloud.title')}</h2>
        <div className="flex min-w-0 flex-col items-start gap-5">
          <p className="max-w-[52ch] text-lg text-frost">{t('cloud.subtitle')}</p>
          <div className="flex flex-wrap items-center gap-3">
            {status === 'authenticated' ? (
              <ButtonLink to="/configs" className="px-8 py-3 text-base">{t('cloud.ctaAuthenticated')}</ButtonLink>
            ) : (
              <ButtonLink to="/register" className="px-8 py-3 text-base">{t('cloud.ctaAnonymous')}</ButtonLink>
            )}
            <ButtonLink to="/showcase" variant="quiet" className="px-8 py-3 text-base">
              {t('cloud.ctaShowcase')}
            </ButtonLink>
          </div>
        </div>
      </div>
    </section>
  )
}

function ChatBar() {
  const { t } = useTranslation('landing')
  const [typed, setTyped] = useState(() => (prefersReducedMotion() ? COMMAND.length : 0))
  const done = typed >= COMMAND.length
  // Typing starts once the chat is on screen, so the player actually sees the command go in.
  const ref = useRef<HTMLDivElement>(null)
  const inView = useInView(ref, { once: true, rootMargin: '-80px' })

  useEffect(() => {
    if (done || !inView) return
    const t = setInterval(() => setTyped((n) => Math.min(n + 1, COMMAND.length)), TYPE_DELAY_MS)
    return () => clearInterval(t)
  }, [done, inView])

  return (
    <div
      ref={ref}
      role="img"
      aria-label={t('cloud.chatAriaLabel')}
      className="lv-chat flex min-h-[5.5em] w-full flex-col justify-end gap-1 px-4 py-3 font-code text-snow sm:px-6"
    >
      <p aria-hidden="true" className="whitespace-pre-wrap break-all">
        {COMMAND.slice(0, typed)}
        {done ? null : <span className="animate-pulse text-frost">▌</span>}
      </p>
      <p aria-hidden="true" className={done ? 'text-ice' : 'invisible'}>
        {t('cloud.chatConfirmation')}
      </p>
    </div>
  )
}
