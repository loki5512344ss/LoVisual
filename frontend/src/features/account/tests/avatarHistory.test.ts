import 'fake-indexeddb/auto'
import { beforeEach, expect, test } from 'vitest'
import { addAvatarHistory, listAvatarHistory } from '../api'

beforeEach(async () => {
  const { indexedDB } = globalThis
  indexedDB.deleteDatabase('lovisual-avatars')
})

test('keeps only the 5 most recent entries, newest first', async () => {
  for (let i = 0; i < 7; i++) await addAvatarHistory(new Blob([String(i)], { type: 'image/png' }))
  const entries = await listAvatarHistory()
  expect(entries).toHaveLength(5)
  expect(await entries[0].blob.text()).toBe('6')
  expect(await entries[4].blob.text()).toBe('2')
})
