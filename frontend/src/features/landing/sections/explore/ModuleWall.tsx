import { useTranslation } from '@shared/i18n/react'
import { CATEGORIES } from '@features/clickgui/parts/modules'
import '@features/landing/styles/landing.css'

/** Each column scrolls at its own pace; the list is duplicated so the loop is seamless. */
const SPEED_MS = [42, 30, 50, 36]

export function ModuleWall() {
  const { t } = useTranslation('landing')

  return (
    <section aria-labelledby="wall-title" className="flex flex-col gap-10 py-20 sm:py-28">
      <div className="grid items-end gap-5 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,0.8fr)] lg:gap-14">
        <h2 id="wall-title" className="text-[2.4rem] leading-[1] tracking-[-0.03em] sm:text-[4rem]">{t('wall.title')}</h2>
        <p className="max-w-[40ch] text-lg text-frost">{t('wall.lead')}</p>
      </div>

      <div role="list" aria-label={t('wall.columnsAria')} className="lv-wall grid min-w-0 grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-4">
        {CATEGORIES.map((cat, i) => {
          // Visuals is what the mod is about, so its column carries the theme accent.
          const featured = cat.id === 'visuals'
          return (
            <div key={cat.id} role="listitem" className="lv-wall-col flex min-w-0 flex-col gap-3">
              <div className={`flex items-baseline justify-between gap-2 border-b pb-2 ${featured ? 'border-ice' : 'border-white/10'}`}>
                <h3 className={`text-lg ${featured ? 'text-ice' : 'text-snow'}`}>{cat.name}</h3>
                <span className="font-code text-sm text-frost">{cat.modules.length}</span>
              </div>
              <div className="lv-wall-viewport relative h-[340px] overflow-hidden">
                <ul className="lv-wall-track flex flex-col gap-1.5" style={{ ['--dur' as string]: `${SPEED_MS[i % SPEED_MS.length]}s` }}>
                  {[...cat.modules, ...cat.modules].map((m, idx) => (
                    <li
                      key={`${m.name}-${idx}`}
                      className={`truncate rounded-md px-2.5 py-1.5 text-sm ${featured ? 'bg-ice/10 text-snow' : 'bg-black/30 text-snow/80'}`}
                    >
                      {m.name}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          )
        })}
      </div>
    </section>
  )
}
