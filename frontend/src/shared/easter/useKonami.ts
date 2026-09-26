import { useEffect, useState } from 'react'

const CODE = ['ArrowUp', 'ArrowUp', 'ArrowDown', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'ArrowLeft', 'ArrowRight', 'b', 'a']

/** Returns true for a few seconds after the Konami code is typed anywhere on the page. */
export function useKonami(durationMs = 7000): boolean {
  const [active, setActive] = useState(false)

  useEffect(() => {
    let index = 0
    let timer: ReturnType<typeof setTimeout> | undefined
    const onKey = (e: KeyboardEvent) => {
      const key = e.key.length === 1 ? e.key.toLowerCase() : e.key
      index = key === CODE[index] ? index + 1 : key === CODE[0] ? 1 : 0
      if (index < CODE.length) return
      index = 0
      setActive(true)
      clearTimeout(timer)
      timer = setTimeout(() => setActive(false), durationMs)
    }
    window.addEventListener('keydown', onKey)
    return () => {
      window.removeEventListener('keydown', onKey)
      clearTimeout(timer)
    }
  }, [durationMs])

  return active
}
