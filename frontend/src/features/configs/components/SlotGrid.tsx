import { useQuery } from '@tanstack/react-query'
import { listSlots } from '../api'
import type { SlotSummary } from '../types'
import { SlotPanel } from './SlotPanel'

const SLOTS = [1, 2, 3, 4] as const

export function SlotGrid() {
  const { data } = useQuery({ queryKey: ['configs'], queryFn: listSlots })
  const bySlot = new Map<number, SlotSummary>((data ?? []).map((s) => [s.slot, s]))

  return (
    <ul className="grid grid-cols-1 gap-4 min-[640px]:grid-cols-2">
      {SLOTS.map((slot) => (
        <li key={slot}>
          <SlotPanel slot={slot} data={bySlot.get(slot) ?? null} />
        </li>
      ))}
    </ul>
  )
}
