import { useEffect, useState } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { Link, useSearchParams } from 'react-router'
import { verifyEmail } from '@features/auth/api'
import { Notice } from '@shared/ui/Notice'

type Outcome = 'checking' | 'done' | 'bad'

/** Landing for the link in the verification email: /verify?token=... */
export default function VerifyPage() {
  const { t } = useTranslation('auth')
  const [params] = useSearchParams()
  const token = params.get('token')
  // A missing token is already the final answer; a present one starts as
  // "checking" and resolves from the endpoint call below.
  const [outcome, setOutcome] = useState<Outcome>(token ? 'checking' : 'bad')

  useEffect(() => {
    if (!token) return
    let cancelled = false
    verifyEmail(token)
      .then(() => {
        if (!cancelled) setOutcome('done')
      })
      .catch(() => {
        if (!cancelled) setOutcome('bad')
      })
    return () => {
      cancelled = true
    }
  }, [token])

  return (
    <section className="mx-auto mt-16 w-full max-w-md">
      <h1 className="mb-4 text-2xl font-semibold text-frost">{t('verify.title')}</h1>
      {outcome === 'checking' ? <p className="text-frost">{t('verify.checking')}</p> : null}
      {outcome === 'done' ? (
        <>
          <Notice tone="success">{t('verify.done')}</Notice>
          <Link to="/account" className="mt-4 inline-block text-ice hover:underline">
            {t('verify.goAccount')}
          </Link>
        </>
      ) : null}
      {outcome === 'bad' ? <Notice tone="error">{t('verify.badToken')}</Notice> : null}
    </section>
  )
}
