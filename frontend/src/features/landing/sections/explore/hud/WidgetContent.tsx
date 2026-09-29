import type { WidgetId } from './layout'

/** Enabled modules as they would appear in the mod's ModuleList panel. */
const MODULES_ON = ['TargetESP', 'PvpCooldowns', 'HitBubbles', 'ShaderSky']

/** Widgets read the accent through --gui-accent so it cross-fades with the active theme. */
const ACCENT = 'var(--gui-accent)'

function KeyCap({ k, pressed = false }: { k: string; pressed?: boolean }) {
  return (
    <span
      className="grid size-7 place-items-center rounded-[6px] border bg-white/5 font-code text-xs leading-none"
      style={{
        borderColor: pressed ? ACCENT : 'rgb(255 255 255 / 0.12)',
        color: pressed ? ACCENT : undefined,
        background: pressed ? `color-mix(in srgb, ${ACCENT} 18%, transparent)` : undefined,
      }}
    >
      {k}
    </span>
  )
}

export function WidgetContent({ id, unitMs }: { id: WidgetId; unitMs: string }) {
  switch (id) {
    case 'fps':
      return (
        <div className="flex items-baseline gap-1.5">
          <span className="font-code text-2xl leading-none font-bold" style={{ color: ACCENT }}>240</span>
          <span className="text-[0.65rem] text-frost">FPS</span>
        </div>
      )
    case 'coords':
      return (
        <div className="font-code text-sm leading-none text-snow">
          <span style={{ color: ACCENT }}>X</span> 128&nbsp; <span style={{ color: ACCENT }}>Y</span> 74&nbsp;{' '}
          <span style={{ color: ACCENT }}>Z</span> -204
        </div>
      )
    case 'modules':
      return (
        <div className="flex flex-col items-end gap-1">
          {MODULES_ON.map((m) => (
            <span
              key={m}
              className="rounded-[5px] border-r-2 bg-black/25 px-2 py-[3px] font-code text-[0.7rem] leading-none text-snow"
              style={{ borderRightColor: ACCENT }}
            >
              {m}
            </span>
          ))}
        </div>
      )
    case 'keys':
      return (
        <div className="flex flex-col items-center gap-1">
          <KeyCap k="W" pressed />
          <div className="flex gap-1">
            <KeyCap k="A" />
            <KeyCap k="S" pressed />
            <KeyCap k="D" />
          </div>
        </div>
      )
    case 'ping':
      return (
        <div className="flex items-center gap-2">
          <span className="font-code text-sm leading-none text-snow">
            32&nbsp;<span className="text-frost">{unitMs}</span>
          </span>
          <span aria-hidden="true" className="flex items-end gap-[3px]">
            {[6, 10, 14, 18].map((h, i) => (
              <span key={h} className="w-[4px] rounded-sm" style={{ height: h, background: i < 2 ? ACCENT : 'rgb(255 255 255 / 0.18)' }} />
            ))}
          </span>
        </div>
      )
  }
}
