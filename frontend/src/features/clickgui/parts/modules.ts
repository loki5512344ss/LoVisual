// In-game catalogue shown by the ClickGui replica: the mod's `@ModuleInfo` display names grouped
// the way `ModulesMenuCategory` groups them (MISC is shown as "Other"), sorted like the in-game
// list. Names are the mod's own labels and stay untranslated.

export type CategoryId = 'combat' | 'player' | 'visuals' | 'other'

export type GuiModule = { name: string; on?: boolean }

export type Category = { id: CategoryId; name: string; modules: GuiModule[] }

const list = (names: string[], on: string[] = []): GuiModule[] =>
  [...names]
    .sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase(), 'en'))
    .map((name) => (on.includes(name) ? { name, on: true } : { name }))

export const CATEGORIES: Category[] = [
  {
    id: 'combat',
    name: 'Combat',
    modules: list(
      ['CombatLog', 'DamageTint', 'No Friend Damage', 'Predictions', 'ProjectilePuncher', 'RubberHand', 'SpearAssist', 'TargetESP', 'TPSSync'],
      ['No Friend Damage', 'Predictions', 'TargetESP'],
    ),
  },
  {
    id: 'player',
    name: 'Player',
    modules: list(
      ['Auto Accept', 'Auto Leave', 'Auto Respawn', 'AutoTool', 'ClickPearl', 'Death Coords', 'Lock Slot', 'NoDelay',
        'NoInteract', 'Tape Mouse', 'Third Person', 'Use Tracker'],
      ['Auto Accept'],
    ),
  },
  {
    id: 'visuals',
    name: 'Visuals',
    modules: list(
      ['AlwaysVisibleHand', 'AmbientParticles', 'Animations', 'AspectRatio', 'AtmoDawnFog', 'BackSword', 'BlockHighlight',
        'CameraClip', 'Chams', 'ChinaHat', 'Crosshair', 'CrystalHoles', 'DropESP', 'EndCrystal', 'ESP', 'FovControl', 'FPS Optimizer',
        'Freecam', 'FullBright', 'Gps', 'Hitboxes', 'HitBubbles', 'HitEffect', 'Item Holder', 'Item Physic', 'JumpCircles',
        'KillEffect', 'KineticLyrics', 'Light', 'Lightning Target ESP', 'MotionBlur', 'NameTags', 'NoRender', 'PostFX', 'Rain',
        'ReimaginedVisual', 'ShaderSky', 'Shulker Preview', 'SoundESP', 'TazikHat', 'TotemFX', 'Trails', 'Trinket', 'Trollface Mask',
        'ViewModel', 'Wet World', 'Wings', 'WorldParticles', 'WorldTweaks', 'Zoom'],
      ['Chams'],
    ),
  },
  {
    id: 'other',
    name: 'Other',
    modules: list(
      ['Balancer', 'BetterMinecraft', 'ClickGUI', 'DefineTarget', 'FakePlayer', 'HitSounds', 'Item Scroller', 'MessageFilter',
        'Name Protect', 'No Server Pack', 'NoSound', 'Optimize', 'PvpCooldowns', 'ShitESP', 'Streamer Mode'],
      ['ClickGUI'],
    ),
  },
]

/** The pill bar's two segments — in-game labels. */
export const TABS = ['modules', 'settings'] as const
export type TabId = (typeof TABS)[number]

/** Presets the scripted demo cycles through (all sit in the first rows of the theme grid). */
export const QUICK_THEMES = ['amber', 'purple', 'blue', 'classic'] as const

/** The settings sidebar, top to bottom; `null` draws a divider. Only "themes" has a page. */
export const SIDEBAR = ['hud', 'ui', 'themes', null, 'layout', 'account', null, 'configs'] as const
export type SidebarId = Exclude<(typeof SIDEBAR)[number], null>
