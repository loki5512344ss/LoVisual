import { useTranslation } from 'react-i18next'
import { LoginForm } from '../../features/auth/forms/LoginForm'
import { AuthSplit } from './auth/AuthSplit'

export default function LoginPage() {
  const { t } = useTranslation('auth')
  return (
    <AuthSplit title={t('login.title')}>
      <LoginForm />
    </AuthSplit>
  )
}
