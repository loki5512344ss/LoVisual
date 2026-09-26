import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { prefersReducedMotion } from '../../shared/motion/reducedMotion'
import { ButtonLink } from '../../shared/ui/Button'
import { useSession } from '../auth/session'

const COMMAND = '%config load 7KQ3M9XA'
const TYPE_DELAY_MS = 60


export function CommandHero() {
  const { t } = useTranslation('landing')
  const { status } = useSession()

  return (
    <section className="relative flex flex-col gap-6 pb-16 pt-20 sm:pt-28">
      <h2 className="text-[2rem] leading-[1.1] sm:text-[2.8rem]">
        {t('cloud.headlinePart1')}
        {' '}<br className="hidden lg:inline" />
        <span className="text-ice">{t('cloud.headlinePart2')}</span>
      </h2>
      <p className="max-w-[60ch] text-lg text-frost">{t('cloud.subtitle')}</p>
      <div className="flex flex-wrap items-center gap-3">
        {status === 'authenticated' ? (
          <ButtonLink to="/configs">{t('cloud.ctaAuthenticated')}</ButtonLink>
        ) : (
          <ButtonLink to="/register">{t('cloud.ctaAnonymous')}</ButtonLink>
        )}
        <ButtonLink to="/showcase" variant="quiet">
          {t('cloud.ctaShowcase')}
        </ButtonLink>
      </div>
      <ChatBar />
    </section>
  )
}

function ChatBar() {
  const { t } = useTranslation('landing')
  const [typed, setTyped] = useState(() => (prefersReducedMotion() ? COMMAND.length : 0))
  const done = typed >= COMMAND.length

  useEffect(() => {
    if (done) return
    const t = setInterval(() => setTyped((n) => Math.min(n + 1, COMMAND.length)), TYPE_DELAY_MS)
    return () => clearInterval(t)
  }, [done])

  return (
    <div
      role="img"
      aria-label={t('cloud.chatAriaLabel')}
      className="mt-6 flex min-h-[4.5rem] w-full max-w-xl flex-col justify-end gap-1 rounded-md border border-white/5 bg-black/55 px-3 py-2 font-code text-base text-snow shadow-[0_0_40px_-12px_var(--page-accent-glow)]"
    >
      <p aria-hidden="true" className="whitespace-pre-wrap break-all">
        {COMMAND.slice(0, typed)}
        {done ? null : <span className="animate-pulse text-frost">▌</span>}
      </p>
      {done ? (
        <p aria-hidden="true" className="text-ice">
          {t('cloud.chatConfirmation')}
        </p>
      ) : null}
    </div>
  )
}
