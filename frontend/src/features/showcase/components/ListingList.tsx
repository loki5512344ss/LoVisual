import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button } from '../../../shared/ui/Button'
import { Notice } from '../../../shared/ui/Notice'
import { browse } from '../api'
import { ListingItem } from './ListingItem'

function readSort(params: URLSearchParams): 'new' | 'popular' {
  return params.get('sort') === 'popular' ? 'popular' : 'new'
}

function readPage(params: URLSearchParams): number {
  const n = Number(params.get('page'))
  return Number.isInteger(n) && n >= 0 ? n : 0
}

export function ListingList({ author }: { author?: string } = {}) {
  const { t } = useTranslation('showcase')
  const [params, setParams] = useSearchParams()
  const sort = readSort(params)
  const page = readPage(params)

  const { data, isError, isPending } = useQuery({
    queryKey: ['showcase', sort, page, author ?? null],
    queryFn: () => browse(sort, page, author),
  })

  function setSort(next: 'new' | 'popular') {
    setParams((p) => {
      const q = new URLSearchParams(p)
      q.set('sort', next)
      q.set('page', '0')
      return q
    })
  }

  function setPage(next: number) {
    setParams((p) => {
      const q = new URLSearchParams(p)
      q.set('page', String(next))
      return q
    })
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex gap-2" role="group" aria-label={t('page.title')}>
        <Button variant={sort === 'new' ? 'primary' : 'quiet'} aria-pressed={sort === 'new'} onClick={() => setSort('new')}>
          {t('page.sortNew')}
        </Button>
        <Button variant={sort === 'popular' ? 'primary' : 'quiet'} aria-pressed={sort === 'popular'} onClick={() => setSort('popular')}>
          {t('page.sortPopular')}
        </Button>
      </div>

      {isError ? <Notice tone="error">{t('page.error')}</Notice> : null}

      {!isError && !isPending && data && data.items.length === 0 ? <Notice tone="info">{t('page.empty')}</Notice> : null}

      {data && data.items.length > 0 ? (
        <ul className="grid grid-cols-1 gap-4 min-[720px]:grid-cols-2">
          {data.items.map((item) => (
            <li key={item.id}>
              <ListingItem listing={item} />
            </li>
          ))}
        </ul>
      ) : null}

      <div className="flex items-center gap-3">
        <Button variant="quiet" disabled={page === 0} onClick={() => setPage(page - 1)}>
          {t('page.back')}
        </Button>
        <Button variant="quiet" disabled={!data?.has_more} onClick={() => setPage(page + 1)}>
          {t('page.next')}
        </Button>
      </div>
    </div>
  )
}
