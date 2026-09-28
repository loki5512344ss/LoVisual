import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { listAvatarHistory, type AvatarHistoryEntry } from '@features/account/api'

type Props = {
  /** Bumped by the parent after each successful upload so the store is re-read. */
  tick: number
  pending: boolean
  onPick: (blob: Blob) => void
}

/** The last few avatars cropped on this device (IndexedDB only, not the account's full history). */
export function AvatarHistoryStrip({ tick, pending, onPick }: Props) {
  const { t } = useTranslation('account')
  const [entries, setEntries] = useState<AvatarHistoryEntry[]>([])

  useEffect(() => {
    let active = true
    listAvatarHistory()
      .then((list) => active && setEntries(list))
      .catch(() => active && setEntries([]))
    return () => {
      active = false
    }
  }, [tick])

  if (entries.length === 0) return null

  return (
    <div className="flex flex-col items-center gap-1 pt-1">
      <p className="text-xs text-frost">{t('avatar.historyHint')}</p>
      <ul className="flex list-none gap-2">
        {entries.map((entry) => (
          <li key={entry.id}>
            <HistoryThumb entry={entry} disabled={pending} onPick={onPick} />
          </li>
        ))}
      </ul>
    </div>
  )
}

function HistoryThumb({ entry, disabled, onPick }: { entry: AvatarHistoryEntry; disabled: boolean; onPick: (blob: Blob) => void }) {
  const { t } = useTranslation('account')
  const url = useMemo(() => URL.createObjectURL(entry.blob), [entry])
  useEffect(() => () => URL.revokeObjectURL(url), [url])

  return (
    <button
      type="button"
      disabled={disabled}
      title={t('avatar.useHistory')}
      aria-label={t('avatar.useHistory')}
      onClick={() => onPick(entry.blob)}
      className="h-10 w-10 cursor-pointer overflow-hidden rounded-full border border-shoal opacity-80 hover:border-ice hover:opacity-100 disabled:cursor-default"
    >
      {url ? <img src={url} alt="" className="h-full w-full object-cover" /> : null}
    </button>
  )
}
