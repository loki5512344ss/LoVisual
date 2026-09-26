import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { describeError } from '../../shared/api/errors'
import { Button } from '../../shared/ui/Button'
import { Notice } from '../../shared/ui/Notice'
import { confirmDevice, normalizeUserCode } from './api'

/** The device-code confirmation form: styled like typing a command into the in-game chat bar. */
export function LinkDeviceForm() {
  const { t } = useTranslation('deviceLink')
  const { t: tc } = useTranslation('common')
  const [code, setCode] = useState('')
  const [error, setError] = useState<string>()
  const mutation = useMutation({
    mutationFn: (userCode: string) => confirmDevice(userCode),
    onError: (err) => setError(describeError(tc, err, { 404: t('form.expired') })),
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    setError(undefined)
    mutation.mutate(normalizeUserCode(code))
  }

  if (mutation.isSuccess) {
    return <Notice tone="info">{t('form.success')}</Notice>
  }

  return (
    <form onSubmit={submit} className="flex w-full max-w-sm flex-col gap-4" noValidate>
      <div className="flex flex-col gap-1.5">
        <label htmlFor="device-code" className="text-sm text-frost">
          {t('form.codeLabel')}
        </label>
        <div className="flex items-center gap-2 rounded-md border border-shoal bg-abyss px-3 py-2 focus-within:border-ice">
          <span aria-hidden="true" className="font-code text-ice">
            %
          </span>
          <input
            id="device-code"
            autoComplete="off"
            autoCapitalize="characters"
            inputMode="text"
            maxLength={9}
            placeholder="ABCD-1234"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            className="w-full bg-transparent font-code text-lg uppercase tracking-widest text-snow placeholder:text-frost/40 focus:outline-none"
          />
          <span aria-hidden="true" className="font-code animate-pulse text-ice">
            ▌
          </span>
        </div>
      </div>
      {error ? <Notice tone="error">{error}</Notice> : null}
      <Button type="submit" busy={mutation.isPending}>
        {t('form.submit')}
      </Button>
    </form>
  )
}
