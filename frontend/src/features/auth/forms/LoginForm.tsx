import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { describeError } from '../../../shared/api/errors'
import { Button } from '../../../shared/ui/Button'
import { Notice } from '../../../shared/ui/Notice'
import { TextField } from '../../../shared/ui/TextField'
import { useSession } from '../session'

export function LoginForm() {
  const { signIn } = useSession()
  const navigate = useNavigate()
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/configs'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(undefined)
    try {
      await signIn(email.trim(), password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(describeError(err, { 401: 'Неверная почта или пароль.' }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="flex w-full max-w-sm flex-col gap-4" noValidate>
      <TextField label="Почта" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      <TextField
        label="Пароль"
        type="password"
        autoComplete="current-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />
      {error ? <Notice tone="error">{error}</Notice> : null}
      <Button type="submit" busy={busy}>
        Войти
      </Button>
      <p className="text-sm text-frost">
        Нет аккаунта? <Link to="/register" className="text-ice underline-offset-4 hover:underline">Создать</Link>
      </p>
    </form>
  )
}
