import { act, renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { useKonami } from '../useKonami'

const press = (key: string) => act(() => void window.dispatchEvent(new KeyboardEvent('keydown', { key })))

describe('useKonami', () => {
  it('activates after the full code', () => {
    const { result } = renderHook(() => useKonami())
    ;['ArrowUp', 'ArrowUp', 'ArrowDown', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'ArrowLeft', 'ArrowRight', 'b'].forEach(press)
    expect(result.current).toBe(false)
    press('a')
    expect(result.current).toBe(true)
  })
  it('ignores a wrong sequence', () => {
    const { result } = renderHook(() => useKonami())
    ;['ArrowUp', 'ArrowDown', 'b', 'a'].forEach(press)
    expect(result.current).toBe(false)
  })
})
