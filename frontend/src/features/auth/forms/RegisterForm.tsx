import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { describeError } from '../../../shared/api/errors'
import { Button } from '../../../shared/ui/Button'
import { Notice } from '../../../shared/ui/Notice'
import { TextField } from '../../../shared/ui/TextField'
import { useSession } from '../session'
import { validateEmail, validateNick, validatePassword } from '../validation'

export function RegisterForm() {
  const { signUp } = useSession()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [nick, setNick] = useState('')
  const [password, setPassword] = useState('')
  const [emailError, setEmailError] = useState<string>()
  const [nickError, setNickError] = useState<string>()
  const [passwordError, setPasswordError] = useState<string>()
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(undefined)

    const emailErr = validateEmail(email)
    const nickErr = validateNick(nick)
    const passwordErr = validatePassword(password)
    setEmailError(emailErr)
    setNickError(nickErr)
    setPasswordError(passwordErr)
    if (emailErr || nickErr || passwordErr) return

    setBusy(true)
    try {
      await signUp(email.trim(), password, nick.trim())
      navigate('/configs', { replace: true })
    } catch (err) {
      setError(describeError(err, { 409: 'Эта почта уже занята. Войди или используй другую.' }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="flex w-full max-w-sm flex-col gap-4" noValidate>
      <TextField
        label="Почта"
        type="email"
        autoComplete="email"
        value={email}
        error={emailError}
        onChange={(e) => setEmail(e.target.value)}
      />
      <TextField
        label="Ник на сайте"
        autoComplete="nickname"
        value={nick}
        error={nickError}
        hint={nickError ? undefined : 'Не обязательно совпадает с ником в Minecraft.'}
        onChange={(e) => setNick(e.target.value)}
      />
      <TextField
        label="Пароль"
        type="password"
        autoComplete="new-password"
        value={password}
        error={passwordError}
        hint={passwordError ? undefined : 'Минимум 8 символов.'}
        onChange={(e) => setPassword(e.target.value)}
      />
      {error ? <Notice tone="error">{error}</Notice> : null}
      <Button type="submit" busy={busy}>
        Создать аккаунт
      </Button>
      <p className="text-sm text-frost">
        Уже есть аккаунт? <Link to="/login" className="text-ice underline-offset-4 hover:underline">Войти</Link>
      </p>
    </form>
  )
}
