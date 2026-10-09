import { useState, type FormEvent } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { Link } from 'react-router'
import { forgotPassword } from '@features/auth/api'
import { describeError } from '@shared/api/errors'
import { Button } from '@shared/ui/Button'
import { Notice } from '@shared/ui/Notice'
import { TextField } from '@shared/ui/text-field/TextField'

/** /forgot-password: asks for a reset email. The answer is the same whether or not the address exists. */
export default function ForgotPasswordPage() {
  const { t } = useTranslation('auth')
  const { t: tc } = useTranslation('common')
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string>()
  const [sent, setSent] = useState(false)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(undefined)
    try {
      await forgotPassword(email.trim())
      setSent(true)
    } catch (err) {
      setError(describeError(tc, err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="mx-auto mt-16 w-full max-w-md">
      <h1 className="mb-4 text-2xl font-semibold text-frost">{t('forgot.title')}</h1>
      {sent ? (
        <>
          <Notice tone="success">{t('forgot.sent')}</Notice>
          <Link to="/login" className="mt-4 inline-block text-ice hover:underline">
            {t('forgot.backToLogin')}
          </Link>
        </>
      ) : (
        <form onSubmit={submit} className="flex w-full flex-col gap-4" noValidate>
          <TextField
            label={t('forgot.emailLabel')}
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          {error ? <Notice tone="error">{error}</Notice> : null}
          <Button type="submit" busy={busy}>
            {t('forgot.submit')}
          </Button>
          <Link to="/login" className="text-sm text-ice hover:underline">
            {t('forgot.backToLogin')}
          </Link>
        </form>
      )}
    </section>
  )
}
