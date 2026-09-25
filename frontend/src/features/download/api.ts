const DEFAULT_REPO = 'loki5512344/LoVisual-'

/** `owner/name` of the GitHub repository that publishes mod releases. */
export const GITHUB_REPO: string = import.meta.env.VITE_GITHUB_REPO || DEFAULT_REPO

/**
 * One-click download of the newest build: GitHub redirects `releases/latest/download/<asset>`
 * straight to the file, so every release must carry an asset named exactly `lovisual.jar`.
 */
export const DOWNLOAD_URL = `https://github.com/${GITHUB_REPO}/releases/latest/download/lovisual.jar`

/** A published GitHub release, narrowed to the fields the download page renders. */
export type Release = {
  tag_name: string
  name: string
  published_at: string
  body: string
  html_url: string
  assets: { name: string; size: number; download_count: number }[]
}

/** Ten most recent releases from the public GitHub API (unauthenticated, ~60 req/h/IP). */
export async function fetchReleases(): Promise<Release[]> {
  const res = await fetch(`https://api.github.com/repos/${GITHUB_REPO}/releases?per_page=10`, {
    headers: { accept: 'application/vnd.github+json' },
  })
  if (!res.ok) throw new Error(`GitHub API responded ${res.status}`)
  return (await res.json()) as Release[]
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
