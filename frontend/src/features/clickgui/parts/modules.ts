// In-game catalogue shown by the ClickGui replica. Module, category and setting names are
// the mod's own identifiers and stay untranslated; descriptions live in the clickgui i18n.

export type Setting =
  | { kind: 'slider'; name: string; min: number; max: number; value: number; unit?: string }
  | { kind: 'check'; name: string; value: boolean }
  | { kind: 'color'; name: string; value: string }

export type GuiModule = { name: ModuleName; on?: boolean; settings?: Setting[] }

export type CategoryId = 'combat' | 'visuals' | 'player' | 'misc'

export type Category = { id: CategoryId; name: string; modules: GuiModule[] }

export type ModuleName =
  | 'PvpCooldowns' | 'Predictions' | 'TargetESP' | 'DamageTint' | 'NoFriendDamage' | 'CombatLog' | 'TPSSync'
  | 'ProjectilePuncher' | 'Trails' | 'JumpCircles' | 'HitBubbles' | 'AmbientParticles' | 'Wings' | 'ChinaHat'
  | 'ShaderSky' | 'MotionBlur' | 'PostFX' | 'KillEffect' | 'ThirdPerson' | 'AutoTool' | 'DeathCoords'
  | 'AutoRespawn' | 'ClickPearl' | 'LockSlot' | 'UseTracker' | 'NoDelay' | 'StreamerMode' | 'NameProtect'
  | 'HitSounds' | 'ItemScroller' | 'MessageFilter' | 'Optimize' | 'NoServerPack' | 'BetterMinecraft'

const slider = (name: string, value: number, max = 100, unit?: string): Setting => ({
  kind: 'slider', name, min: 0, max, value, unit,
})
const check = (name: string, value: boolean): Setting => ({ kind: 'check', name, value })
const color = (name: string, value: string): Setting => ({ kind: 'color', name, value })

export const CATEGORIES: Category[] = [
  {
    id: 'combat',
    name: 'Combat',
    modules: [
      { name: 'TargetESP', on: true, settings: [slider('Size', 60), check('Pulse', true), color('Color', 'var(--gui-accent)')] },
      { name: 'PvpCooldowns', on: true },
      { name: 'Predictions', settings: [slider('Ticks', 6, 20), check('Show path', true)] },
      { name: 'DamageTint', on: true, settings: [slider('Strength', 40), color('Tint', '#ff5a6e')] },
      { name: 'NoFriendDamage' },
      { name: 'TPSSync' },
      { name: 'ProjectilePuncher' },
      { name: 'CombatLog' },
    ],
  },
  {
    id: 'visuals',
    name: 'Visuals',
    modules: [
      { name: 'Trails', settings: [slider('Length', 45), check('Rainbow', false), color('Color', 'var(--gui-accent)')] },
      { name: 'JumpCircles', on: true, settings: [slider('Radius', 70), color('Color', 'var(--gui-accent)')] },
      { name: 'HitBubbles', on: true },
      { name: 'AmbientParticles', on: true, settings: [slider('Count', 120, 150), check('Glow', true)] },
      { name: 'Wings' },
      { name: 'ChinaHat', settings: [check('Gradient', true), color('Color', '#f2c14e')] },
      { name: 'ShaderSky', on: true },
      { name: 'MotionBlur', settings: [slider('Strength', 30)] },
      { name: 'PostFX', on: true, settings: [check('Bloom', true), slider('Vignette', 25)] },
      { name: 'KillEffect' },
    ],
  },
  {
    id: 'player',
    name: 'Player',
    modules: [
      { name: 'ThirdPerson', on: true, settings: [slider('Distance', 4, 12, 'm')] },
      { name: 'AutoTool', on: true },
      { name: 'DeathCoords', on: true },
      { name: 'AutoRespawn' },
      { name: 'ClickPearl' },
      { name: 'LockSlot' },
      { name: 'UseTracker' },
      { name: 'NoDelay' },
    ],
  },
  {
    id: 'misc',
    name: 'Misc',
    modules: [
      { name: 'StreamerMode', settings: [check('Hide coords', true), check('Hide IP', true)] },
      { name: 'NameProtect', on: true },
      { name: 'HitSounds', on: true, settings: [slider('Volume', 65)] },
      { name: 'ItemScroller' },
      { name: 'MessageFilter' },
      { name: 'Optimize', on: true },
      { name: 'NoServerPack' },
      { name: 'BetterMinecraft' },
    ],
  },
]

/** The ClickGui's top tab row — in-game labels. */
export const TABS = ['Modules', 'Themes', 'Configs', 'HUD'] as const

/** Presets offered as quick dots in the category column (and cycled by the demo). */
export const QUICK_THEMES = ['classic', 'nitro', 'sunset', 'aurora'] as const

export const settingKey = (module: string, setting: string) => `${module}.${setting}`

export const findModule = (name: string) => CATEGORIES.flatMap((c) => c.modules).find((m) => m.name === name)
