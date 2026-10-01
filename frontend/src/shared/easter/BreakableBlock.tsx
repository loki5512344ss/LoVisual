import { useState } from 'react'
import { useTranslation } from '@shared/i18n/react'
import '@shared/motion/motion.css'
import './block.css'

/** Clicks needed before the block shatters: two crack stages then the burst. */
const BREAK_AT = 3

type Particle = { id: number; dx: string; dy: string }

/**
 * `/` 404 easter egg: a grass block drops into a dusk world, cracks over a few clicks,
 * then bursts into square debris and respawns. Under reduced motion it skips the fall and
 * the burst — clicking still walks the crack stages and resets.
 */
export function BreakableBlock() {
  const { t } = useTranslation('common')
  const [cracks, setCracks] = useState(0)
  const [particles, setParticles] = useState<Particle[]>([])
  const [cycle, setCycle] = useState(0) // bump to replay the drop after a burst

  function burst() {
    setParticles(
      Array.from({ length: 10 }, (_, i) => ({
        id: i,
        dx: `${(i - 4.5) * 16}px`,
        dy: `${-40 - (i % 3) * 24}px`,
      })),
    )
    setCracks(0)
    setCycle((c) => c + 1)
    window.setTimeout(() => setParticles([]), 500)
  }

  function crack() {
    const next = cracks + 1
    if (next >= BREAK_AT) {
      burst()
      return
    }
    setCracks(next)
  }

  return (
    <div className="lv-block-world">
      <span key={cycle} className="lv-block-wrap lv-drop">
        <button type="button" className="lv-block" aria-label={t('notFound.blockAria')} onClick={crack}>
          {cracks > 0 && <span className="lv-block-crack" data-stage={String(Math.min(cracks, 2))} aria-hidden="true" />}
        </button>
      </span>

      {particles.map((p) => (
        <span key={`${cycle}-${p.id}`} className="lv-block-particle" style={{ ['--dx' as string]: p.dx, ['--dy' as string]: p.dy }} />
      ))}
    </div>
  )
}
