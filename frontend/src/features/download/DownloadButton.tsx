import { useTranslation } from '@shared/i18n/react'
import { DOWNLOAD_URL, formatSize } from './api'
import '../landing/styles/landing.css'

type Props = {
  size?: 'md' | 'lg'
  /** Latest release tag (e.g. `0.1.1`); with `bytes` it renders a size hint under the label. */
  version?: string
  bytes?: number
  className?: string
}

/** The primary one-click download link. Kept query-free so the landing stays light. */
export function DownloadButton({ size = 'md', version, bytes, className = '' }: Props) {
  const { t, i18n } = useTranslation('download')
  const hint = version && bytes ? t('versionHint', { version, size: formatSize(bytes, i18n.language) }) : null
  const pad = size === 'lg' ? 'lv-cta-lg' : ''

  return (
    <span className={`inline-flex flex-col items-start gap-2 ${className}`}>
      <a href={DOWNLOAD_URL} download className={`lv-cta-primary ${pad}`}>
        {t('button')}
      </a>
      <span className="font-code text-xs text-frost">{hint ?? t('hint')}</span>
    </span>
  )
}
