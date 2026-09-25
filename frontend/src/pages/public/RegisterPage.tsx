import { useTranslation } from 'react-i18next'
import { RegisterForm } from '../../features/auth/forms/RegisterForm'

export default function RegisterPage() {
  const { t } = useTranslation('auth')
  return (
    <section className="flex flex-col gap-6 py-12">
      <h1 className="text-3xl">{t('register.title')}</h1>
      <p className="max-w-sm text-frost">{t('register.subtitle')}</p>
      <RegisterForm />
    </section>
  )
}
