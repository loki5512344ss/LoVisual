const DEFAULT_REPO = 'boba/LoVisual'

/** `owner/name` of the Forgejo repository that publishes mod releases. */
export const FORGEJO_REPO: string = import.meta.env.VITE_FORGEJO_REPO || DEFAULT_REPO

/** Project home and its releases page (the repository lives on our own Forgejo). */
export const SOURCE_URL = `https://git.wihuk.pro/${FORGEJO_REPO}`
export const RELEASES_URL = `${SOURCE_URL}/releases`

/**
 * One-click download of the newest build. Served by our own gateway
 * (`GET /downloads/lovisual.jar`, reached as `/api/...` through the site origin's
 * nginx). The file on the server is NOT rebuilt automatically: whoever cuts a mod
 * release re-uploads `lovisual.jar` and `lovisual.jar.sha256` there by hand
 * (the mod is a separate Gradle project, deliberately not part of `deploy.sh`).
 */
export const DOWNLOAD_URL = '/api/downloads/lovisual.jar'
export const CHECKSUM_URL = '/api/downloads/lovisual.jar.sha256'

/**
 * Forgejo's API sends no CORS headers, so the browser reads releases through
 * our own nginx (`location = /api/releases`, cached for a few minutes).
 */
export const RELEASES_API = '/api/releases'

/** A published release, narrowed to the fields the download page renders. */
export type Release = {
  tag_name: string
  name: string
  published_at: string
  body: string
  html_url: string
  assets: { name: string; size: number; download_count: number }[]
}

/** The most recent releases, newest first. */
export async function fetchReleases(): Promise<Release[]> {
  const res = await fetch(RELEASES_API, { headers: { accept: 'application/json' } })
  if (!res.ok) throw new Error(`Releases API responded ${res.status}`)
  return (await res.json()) as Release[]
}

/** SHA-256 of the served jar (`sha256sum` output), or null when it is not published. */
export async function fetchChecksum(): Promise<string | null> {
  const res = await fetch(CHECKSUM_URL)
  if (!res.ok) return null
  const hash = (await res.text()).trim().split(/\s+/)[0]?.toLowerCase() ?? ''
  return /^[0-9a-f]{64}$/.test(hash) ? hash : null
}

/** Total bytes of `.jar` assets in a release — the download button's size hint. */
export function releaseSize(release: Release): number {
  return release.assets.reduce((sum, a) => (a.name.endsWith('.jar') ? sum + a.size : sum), 0)
}

/** Human size in megabytes, locale-aware (e.g. «4,2 МБ» / "4.2 MB"). */
export function formatSize(bytes: number, locale: string): string {
  const mb = bytes / (1024 * 1024)
  return new Intl.NumberFormat(locale, {
    style: 'unit',
    unit: 'megabyte',
    unitDisplay: 'short',
    maximumFractionDigits: 1,
  }).format(mb)
}
