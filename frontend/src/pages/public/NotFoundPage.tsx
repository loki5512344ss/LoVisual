import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { NOT_FOUND_ART } from '../../shared/easter/art.generated'

export default function NotFoundPage() {
  const { t } = useTranslation('common')
  return (
    <section className="flex flex-col gap-4 py-16">
      <pre aria-hidden="true" className="w-fit max-w-full overflow-x-auto rounded-panel border border-white/10 bg-black/30 p-4 font-code text-xs leading-tight text-ice">
{NOT_FOUND_ART}
{t('notFound.console')}
      </pre>
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
