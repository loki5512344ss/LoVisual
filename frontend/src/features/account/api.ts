import { api } from '@shared/api/client'
import type { TFunction } from '@shared/i18n/react'

export type DeviceLink = { id: string; linked_at: string; last_seen: string | null }

const ALLOWED_TYPES = new Set(['image/png', 'image/jpeg', 'image/webp'])
const MAX_BYTES = 5 * 1024 * 1024

/** Client-side avatar checks so a bad file never reaches the server. */
export function checkAvatarFile(file: File, t: TFunction<'account'>): string | undefined {
  if (!ALLOWED_TYPES.has(file.type)) return t('validation.avatarType')
  if (file.size > MAX_BYTES) return t('validation.avatarTooBig')
  return undefined
}

export async function uploadAvatar(file: File): Promise<{ avatar_url: string }> {
  const body = new FormData()
  body.set('file', file)
  return api.request('/avatars', { method: 'POST', body })
}

/**
 * DELETE /avatars — clears the caller's picture server-side. The backend is
 * idempotent (always 204), so a repeated click is safe. The `me` cache is
 * invalidated by the caller, not here, so the button can chain invalidations
 * with any other side effects (e.g. history tick bump).
 */
export async function removeAvatar(): Promise<void> {
  await api.request('/avatars', { method: 'DELETE' })
}

/** Byte-level progress comes only from XHR (see `api.upload`), not from fetch. */
export function uploadAvatarWithProgress(file: File, onProgress: (pct: number) => void): Promise<{ avatar_url: string }> {
  const body = new FormData()
  body.set('file', file)
  return api.upload('/avatars', body, onProgress) as Promise<{ avatar_url: string }>
}

/** Downloads the current avatar so it can be re-cropped without re-picking the source file. */
export async function fetchCurrentAvatarAsFile(url: string): Promise<File> {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return new File([await res.blob()], 'avatar.png', { type: 'image/png' })
}

const DB_NAME = 'lovisual-avatars'
const STORE = 'history'
const HISTORY_LIMIT = 5

export type AvatarHistoryEntry = { id: number; blob: Blob; storedAt: string }

// IndexedDB structured-clone does not reliably persist a Blob (Node/fake-indexeddb drop it), so the
// PNG is stored as an ArrayBuffer — universally cloneable — and rewrapped as a Blob on read.
type AvatarRecord = { id: number; data: ArrayBuffer; storedAt: string }

function openAvatarDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const open = indexedDB.open(DB_NAME, 1)
    open.onupgradeneeded = () => {
      if (!open.result.objectStoreNames.contains(STORE)) open.result.createObjectStore(STORE, { keyPath: 'id', autoIncrement: true })
    }
    open.onsuccess = () => resolve(open.result)
    open.onerror = () => reject(open.error)
  })
}

function readAll(db: IDBDatabase): Promise<AvatarRecord[]> {
  return new Promise((resolve, reject) => {
    const req = db.transaction(STORE).objectStore(STORE).getAll()
    req.onsuccess = () => resolve(req.result as AvatarRecord[])
    req.onerror = () => reject(req.error)
  })
}

/** Stores a cropped avatar locally (this device only) and trims the history to the newest few. */
export async function addAvatarHistory(blob: Blob): Promise<void> {
  const db = await openAvatarDb()
  const data = await blob.arrayBuffer()
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).put({ data, storedAt: new Date().toISOString() })
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
  // Same-ms inserts are indistinguishable by timestamp, so keep the last 5 by insertion order.
  const records = await readAll(db)
  const stale = records.slice(0, Math.max(0, records.length - HISTORY_LIMIT))
  if (stale.length === 0) return
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    for (const r of stale) tx.objectStore(STORE).delete(r.id)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

/** Newest-first history. */
export async function listAvatarHistory(): Promise<AvatarHistoryEntry[]> {
  const db = await openAvatarDb()
  const records = await readAll(db)
  return records
    .sort((a, b) => b.storedAt.localeCompare(a.storedAt) || b.id - a.id)
    .slice(0, HISTORY_LIMIT)
    .map(({ id, data, storedAt }) => ({ id, storedAt, blob: new Blob([data], { type: 'image/png' }) }))
}

export async function listDevices(): Promise<DeviceLink[]> {
  return api.request('/device/links')
}

export async function revokeDevice(id: string): Promise<void> {
  await api.request(`/device/links/${id}`, { method: 'DELETE' })
}
