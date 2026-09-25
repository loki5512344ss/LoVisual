import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

export default function NotFoundPage() {
  const { t } = useTranslation('common')
  return (
    <section className="flex flex-col gap-4 py-16">
      <h1 className="text-[2.4375rem] leading-tight">{t('notFound.title')}</h1>
      <p className="text-frost">
        {t('notFound.body')}{' '}
        <Link to="/" className="text-ice underline-offset-4 hover:underline">
          {t('notFound.home')}
        </Link>
      </p>
    </section>
  )
}
