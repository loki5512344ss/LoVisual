import { useTranslation } from 'react-i18next'
import { LoginForm } from '../../features/auth/forms/LoginForm'

export default function LoginPage() {
  const { t } = useTranslation('auth')
  return (
    <section className="flex flex-col gap-6 py-12">
      <h1 className="text-3xl">{t('login.title')}</h1>
      <LoginForm />
    </section>
  )
}
