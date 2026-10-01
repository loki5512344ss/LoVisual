import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from '@shared/i18n/react'
import { Link } from 'react-router'
import { Button } from '@shared/ui/Button'
import { listDevices, revokeDevice, type DeviceLink } from '../api'

function relativeDate(iso: string): string {
  return new Intl.DateTimeFormat('ru-RU', { dateStyle: 'long' }).format(new Date(iso))
}

function DeviceRow({ device }: { device: DeviceLink }) {
  const { t } = useTranslation('account')
  const client = useQueryClient()
  const [confirming, setConfirming] = useState(false)
  const mutation = useMutation({
    mutationFn: () => revokeDevice(device.id),
    onSuccess: () => client.invalidateQueries({ queryKey: ['devices'] }),
  })

  return (
    <li className="flex items-center justify-between gap-4 rounded-(--radius-panel) border border-shoal bg-slate/60 px-4 py-3">
      <div>
        <p className="text-sm text-snow">{t('devices.linkedAt', { date: relativeDate(device.linked_at) })}</p>
        <p className="text-sm text-frost">
          {t('devices.lastSeen', { when: device.last_seen ? relativeDate(device.last_seen) : t('devices.neverSeen') })}
        </p>
      </div>
      {confirming ? (
        <div className="flex items-center gap-2">
          <Button variant="danger" busy={mutation.isPending} onClick={() => mutation.mutate()}>
            {t('devices.confirmRevoke')}
          </Button>
          <Button variant="quiet" onClick={() => setConfirming(false)}>
            {t('devices.cancel')}
          </Button>
        </div>
      ) : (
        <Button variant="danger" onClick={() => setConfirming(true)}>
          {t('devices.revoke')}
        </Button>
      )}
    </li>
  )
}

/** ClickGui-like rows listing devices the mod linked, each with a two-step confirm to revoke. */
export function DeviceList() {
  const { t } = useTranslation('account')
  const query = useQuery({ queryKey: ['devices'], queryFn: listDevices })
  const devices = query.data ?? []

  return (
    <div className="rounded-(--radius-panel) border border-shoal bg-slate/40 p-5">
      <h2 className="font-display text-lg">{t('devices.heading')}</h2>
      {devices.length === 0 ? (
        <p className="mt-3 text-sm text-frost">
          {t('devices.empty')}{' '}
          <Link to="/link" className="text-ice underline-offset-4 hover:underline">
            {t('devices.emptyLink')}
          </Link>
        </p>
      ) : (
        <ul className="mt-3 flex flex-col gap-2">
          {devices.map((device) => (
            <DeviceRow key={device.id} device={device} />
          ))}
        </ul>
      )}
    </div>
  )
}
