import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from './Button'

export function ShareCode({ code, label }: { code: string; label?: string }) {
  const { t } = useTranslation('common')
  const [copied, setCopied] = useState(false)
  useEffect(() => {
    if (!copied) return
    const t = setTimeout(() => setCopied(false), 2000)
    return () => clearTimeout(t)
  }, [copied])

  async function copy() {
    await navigator.clipboard.writeText(code)
    setCopied(true)
  }

  return (
    <div className="flex flex-wrap items-center gap-3">
      {label ? <span className="text-sm text-frost">{label}</span> : null}
      <code className="font-code text-lg tracking-widest text-ice">{code}</code>
      <Button variant="quiet" onClick={copy}>
        {t('shareCode.copy')}
      </Button>
      <span aria-live="polite" className="text-sm text-frost">
        {copied ? t('shareCode.copied') : ''}
      </span>
    </div>
  )
}
