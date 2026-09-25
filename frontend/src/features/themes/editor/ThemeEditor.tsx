import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '../../../shared/ui/Button'
import { PRESETS } from '../presets.generated'
import type { GradientSpec, ThemeColors, ThemeEntry } from '../types'
import { EditorPreview } from './EditorPreview'
import { ColorField } from './ColorField'
import { COLOR_SLOTS, GRADIENT_SLOTS, fromProfileJson, toProfileJson, toShareHash } from './codec'
import { GradientField } from './GradientField'
import { useThemeHistory } from './useThemeHistory'

/** Pack the draft name into a slug id, mirroring the codec's `slug`. */
const nameToId = (name: string) => name.trim().toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') || 'custom'

export function ThemeEditor({ initial }: { initial: ThemeEntry }) {
  const { t } = useTranslation('themes')
  const { draft, set, reset, undo, redo, canUndo, canRedo } = useThemeHistory(initial)
  const [notice, setNotice] = useState<string | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined)

  const flash = useCallback((text: string) => {
    setNotice(text)
    clearTimeout(timer.current)
    timer.current = setTimeout(() => setNotice(null), 2000)
  }, [])

  useEffect(() => () => clearTimeout(timer.current), [])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (!(e.ctrlKey || e.metaKey) || e.key.toLowerCase() !== 'z') return
      e.preventDefault()
      if (e.shiftKey) redo()
      else undo()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [undo, redo])

  const setColor = (slot: keyof ThemeColors, hex: string) => set({ ...draft, theme: { ...draft.theme, [slot]: hex } })
  const setGradient = (key: keyof ThemeEntry, patch: Partial<GradientSpec>) =>
    set({ ...draft, [key]: { ...(draft[key] as GradientSpec), ...patch } })

  const copy = async () => {
    await navigator.clipboard?.writeText(toProfileJson(draft))
    flash(t('editor.copied'))
  }

  const download = () => {
    const url = URL.createObjectURL(new Blob([toProfileJson(draft)], { type: 'application/json' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `${draft.id}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  const share = async () => {
    await navigator.clipboard?.writeText(`${location.origin}${location.pathname}#t=${toShareHash(draft)}`)
    flash(t('editor.shared'))
  }

  const onFile = (file?: File) => {
    if (!file) return
    file.text().then((text) => {
      const result = fromProfileJson(text)
      if ('error' in result) flash(t('editor.importError'))
      else reset(result)
    })
  }

  return (
    <div className="grid gap-8 lg:grid-cols-2">
      <EditorPreview draft={draft} />

      <div className="flex flex-col gap-6">
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="quiet" onClick={undo} disabled={!canUndo}>
            {t('editor.undo')}
          </Button>
          <Button variant="quiet" onClick={redo} disabled={!canRedo}>
            {t('editor.redo')}
          </Button>
          <Button variant="quiet" onClick={() => reset(initial)}>
            {t('editor.reset')}
          </Button>
          {notice && <span className="ml-auto font-code text-xs text-ice">{notice}</span>}
        </div>

        <label className="flex flex-col gap-1">
          <span className="text-xs text-frost">{t('editor.nameLabel')}</span>
          <input
            value={draft.name}
            onChange={(e) => set({ ...draft, name: e.target.value, id: nameToId(e.target.value) })}
            className="rounded-md border border-white/15 bg-black/40 px-3 py-2 text-snow"
          />
        </label>

        <section className="flex flex-col gap-2">
          <h3 className="text-sm font-semibold uppercase text-frost">{t('editor.presets')}</h3>
          <div className="flex flex-wrap gap-2">
            {PRESETS.map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => reset(p)}
                className={`lv-preset-chip ${p.id === draft.id ? 'is-active' : ''}`}
              >
                {p.name}
              </button>
            ))}
          </div>
        </section>

        <section className="flex flex-col gap-3">
          <h3 className="text-sm font-semibold uppercase text-frost">{t('editor.colors')}</h3>
          <div className="grid gap-3 sm:grid-cols-2">
            {COLOR_SLOTS.map((slot) => (
              <ColorField key={slot} label={t(`colors.${slot}`)} value={draft.theme[slot]} onChange={(hex) => setColor(slot, hex)} />
            ))}
          </div>
        </section>

        <section className="flex flex-col gap-3">
          <h3 className="text-sm font-semibold uppercase text-frost">{t('editor.gradients')}</h3>
          <div className="grid gap-3">
            {GRADIENT_SLOTS.map((g) => (
              <GradientField
                key={g.prefix}
                title={t(`editor.gradient.${g.prefix}`)}
                value={draft[g.key] as GradientSpec}
                onChange={(patch) => setGradient(g.key, patch)}
              />
            ))}
          </div>
        </section>

        <div className="flex flex-wrap gap-2">
          <Button onClick={copy}>{t('editor.copy')}</Button>
          <Button variant="quiet" onClick={download}>
            {t('editor.download')}
          </Button>
          <Button variant="quiet" onClick={() => fileRef.current?.click()}>
            {t('editor.import')}
          </Button>
          <Button variant="quiet" onClick={share}>
            {t('editor.share')}
          </Button>
          <input ref={fileRef} type="file" accept="application/json,.json" className="hidden" onChange={(e) => onFile(e.target.files?.[0])} />
        </div>
      </div>
    </div>
  )
}
