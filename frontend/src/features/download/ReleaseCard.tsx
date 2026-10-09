import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from '@shared/i18n/react'
import { CHECKSUM_URL, DOWNLOAD_URL, fetchChecksum, formatSize, type Release, releaseSize } from './api'
import './download.css'

/** Pixel heart from the logo, one rect per cell so it stays crisp at any size. */
const HEART = ['.XX.XX.', 'XXXXXXX', 'XXXXXXX', '.XXXXX.', '..XXX..', '...X...']

function PixelHeart() {
  const cells = HEART.flatMap((row, y) =>
    [...row].flatMap((c, x) => (c === 'X' ? [{ x, y, hi: x + y < 3 && x < 3 }] : [])),
  )
  return (
    <svg viewBox="0 0 7 6" shapeRendering="crispEdges" aria-hidden="true">
      <defs>
        <clipPath id="lv-heart-clip">
          {cells.map((c) => <rect key={`${c.x}-${c.y}`} x={c.x} y={c.y} width="1" height="1" />)}
        </clipPath>
      </defs>
      {cells.map((c) => (
        <rect key={`${c.x}-${c.y}`} x={c.x} y={c.y} width="1" height="1" fill={c.hi ? '#f472b6' : '#be185d'} />
      ))}
      <g clipPath="url(#lv-heart-clip)">
        <rect className="lv-glint" x="0" y="-1" width="2" height="8" fill="#fff" opacity="0.55" />
      </g>
    </svg>
  )
}

/**
 * The download as an in-game item: heart in an inventory slot, a vanilla-style
 * tooltip with version, size and SHA-256, and the download button underneath.
 */
export function ReleaseCard({ release }: { release?: Release }) {
  const { t, i18n } = useTranslation('download')
  const [copied, setCopied] = useState(false)
  const { data: hash } = useQuery({ queryKey: ['checksum'], queryFn: fetchChecksum, staleTime: 10 * 60 * 1000 })
  const version = release?.tag_name.replace(/^v/, '')
  const bytes = release ? releaseSize(release) : 0

  const copy = () => {
    if (!hash) return
    void navigator.clipboard?.writeText(hash).then(() => {
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1800)
    })
  }

  return (
    <div className="lv-item">
      <div className="flex items-start gap-4">
        <div className="lv-slot"><PixelHeart /></div>
        <div className="lv-tip flex-1">
          <div className="lv-tip-name">LoVisual{version ? ` ${version}` : ''}</div>
          <div>{t('card.platform')}</div>
          {bytes > 0 ? <div>{t('card.size', { size: formatSize(bytes, i18n.language) })}</div> : null}
          {hash ? (
            <div className="mt-1">
              <span className="lv-tip-key">SHA-256</span>
              <div className="lv-tip-hash">{hash}</div>
            </div>
          ) : null}
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-x-5 gap-y-3">
        <a href={DOWNLOAD_URL} download className="lv-cta-primary lv-cta-lg">{t('button')}</a>
        {hash ? (
          <button type="button" onClick={copy} className="text-sm text-ice underline-offset-4 hover:underline">
            {copied ? t('card.copied') : t('card.copyHash')}
          </button>
        ) : null}
        <a href={CHECKSUM_URL} className="sr-only">{t('card.checksumFile')}</a>
      </div>
    </div>
  )
}
