import { useTranslation } from 'react-i18next'
import { CATEGORIES } from '@features/clickgui/parts/modules'
import '@features/landing/styles/landing.css'

/** Each column scrolls at its own pace; the list is duplicated so the loop is seamless. */
const SPEED_MS = [42, 30, 50, 36]

export function ModuleWall() {
  const { t } = useTranslation('landing')

  return (
    <section aria-labelledby="wall-title" className="grid gap-10 py-20 sm:py-28 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)] lg:gap-14">
      <div className="flex min-w-0 flex-col items-start gap-4">
        <span className="font-code text-xs uppercase tracking-[0.2em] text-ice">{t('wall.eyebrow')}</span>
        <h2 id="wall-title" className="text-[2rem] leading-[1.1] sm:text-[2.6rem]">{t('wall.title')}</h2>
        <p className="max-w-[42ch] text-lg text-frost">{t('wall.lead')}</p>
      </div>

      <div role="list" aria-label={t('wall.columnsAria')} className="lv-wall grid min-w-0 grid-cols-2 gap-4 sm:grid-cols-4">
        {CATEGORIES.map((cat, i) => (
          <div key={cat.id} role="listitem" className="lv-wall-col flex min-w-0 flex-col gap-2">
            <h3 className="font-code text-xs uppercase tracking-[0.18em] text-ice">{cat.name}</h3>
            <div className="lv-wall-viewport relative h-[300px] overflow-hidden">
              <ul className="lv-wall-track flex flex-col gap-1.5" style={{ ['--dur' as string]: `${SPEED_MS[i % SPEED_MS.length]}s` }}>
                {[...cat.modules, ...cat.modules].map((m, idx) => (
                  <li
                    key={`${m.name}-${idx}`}
                    className="truncate rounded-md border border-white/5 bg-black/30 px-2.5 py-1.5 text-sm text-snow/90 hover:border-ice/40"
                  >
                    {m.name}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
