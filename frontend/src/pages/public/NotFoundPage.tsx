import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { BreakableBlock } from '@shared/easter/BreakableBlock'
import { PageTitle } from '@shared/layout/PageTitle'

export default function NotFoundPage() {
  const { t } = useTranslation('common')
  return (
    <section className="flex flex-1 flex-col items-start gap-6 py-16">
      <BreakableBlock />
      <p className="max-w-md text-sm text-frost">{t('notFound.blockHint')}</p>
      <PageTitle
        lead={
          <>
            {t('notFound.body')}{' '}
            <Link to="/" className="text-ice underline-offset-4 hover:underline">
              {t('notFound.home')}
            </Link>
          </>
        }
      >
        {t('notFound.title')}
      </PageTitle>
    </section>
  )
}
