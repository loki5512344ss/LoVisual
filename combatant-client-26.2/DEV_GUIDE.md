# LoVisual Mod — Dev Guide / Справочник по созданию модулей

Цель: быстро посмотреть структуру и написать новый модуль / HUD-элемент /
команду, не перекапывая весь код. Всё ниже — сводка по реальному коду.

- Пакеты: `dev.loki.lovisual.*`
- Minecraft 26.2, Fabric, Java 25, Lombok
- Правила проекта (важно!): файл ≤200 строк, папка ≤3 файлов,
  `./gradlew build` после каждого этапа, тесты для новой логики,
  никаких `static final Minecraft` в полях — только ленивый `Minecraft.getInstance()`.

---

## 1. Архитектура (главные пакеты)

```
dev.loki.lovisual
├── LoVisual.java                 — ClientModInitializer: весь старт (тики, рендер-фазы)
├── Lifecycle.java                — INIT → ACTIVE → SHUTDOWN; гейты isActive/canRunRender/canRunHud
├── features/
│   ├── module/                   — СИСТЕМА МОДУЛЕЙ (главное для вас)
│   │   ├── Module.java           — базовый класс (1181 стр, фасад)
│   │   ├── ModuleInfo            — @annotation для модуля
│   │   ├── ModuleManager         — реестр + диспетчер фаз
│   │   ├── ModuleAutoLoader      — авто-обнаружение по аннотации (ClassGraph)
│   │   ├── Modules               — удобный доступ: Modules.get(Класс.class)
│   │   ├── ModuleCategory        — COMBAT/MOVEMENT/PLAYER/VISUALS/MISC
│   │   ├── HudPhase / WorldPhase — фазы рендера
│   │   └── modules/{combat,player,visuals,misc}/
│   ├── command/                  — команды: @CommandInfo + ClientCommand
│   ├── gui/
│   │   ├── clickgui/             — экран настроек (Setting → виджет)
│   │   └── hud/                  — HUD-элементы
│   │       ├── draggable/        — перетаскиваемые (Fps, Coords, ModuleList, TargetHud…)
│   │       └── nondraggable/     — статичные (CustomBar, CustomHotbar, BetterChat…)
│   ├── relations/                — друзья/враги/категории (CategoryService)
│   ├── security/                 — BackdoorProtection (SSRF/translate-защита)
│   └── theme/                    — темы: Theme.theme() → Themes.Theme (цвета)
├── config/
│   ├── values/                   — типы настроек: ConfigValue (bool/num/mode/color…)
│   ├── SettingDef.java           — GUI-нейтральный дескриптор настройки
│   └── common/CommonSettingSchemas — переиспользуемые схемы (i18n-ключи)
├── events/
│   ├── EventBus (Events.BUS)     — шина событий
│   ├── @EventHandler             — аннотация хендлера
│   └── impl/                     — все события (см. §5)
├── mixins/                       — миксины + accessors (mixins/accessors/)
├── addon/                        — система аддонов (API v0)
├── api/v0/                       — публичный API аддонов (module/client/render/clickgui)
├── render/
│   ├── engine/
│   │   ├── renderer/Renderer2D   — весь 2D-рендер (прямоугольники, круги, текст, items)
│   │   ├── renderer/Renderer3D   — 3D (линии, меши, quads)
│   │   ├── text/                 — Fonts.renderer("Onest", Regular), TextRenderer
│   │   ├── animation/AnimationUtility
│   │   ├── pipeline/LoVisualRenderPipelines — готовые пайплайны (UI_COLORED и т.д.)
│   │   └── uniform/MeshBuilder   — построение мешей
│   └── engine/color/RenderColor  — ARGB
└── util/                         — куча хелперов (см. §8)
```

---

## 2. Жизненный цикл и старт

В `LoVisual.onInitializeClient()` по порядку:

1. `LoVisualRenderEngineBootstrap.init()`
2. `MainConfig.get()`, `AccountConfig.get()`, `CommandManager.init()`
3. `ModuleAutoLoader.load("dev.loki.lovisual.features.module.modules")` — сканирует пакет,
   ищет классы с `@ModuleInfo`, создаёт через no-args конструктор и зовёт `module.postInit()`.
4. `ModuleManager.loadAllModuleConfigs()` — применяет сохранённые конфиги, включает enabled-модули.
5. `HudElements.init()`, `StaticHudElementBootstrap.init()` — HUD-элементы.
6. `Events.BUS.register(...)` — реестрируются глобальные сервисы.
7. `Lifecycle.activate()` — после этого события/тики работают.

**Вывод:** чтобы добавить модуль — просто положи класс с `@ModuleInfo` в пакет
`...modules.<категория>` и всё. Реестрировать руками не нужно.

`Lifecycle` — главный гейт: `Lifecycle.isActive()`, `canRunRender()`, `canRunHud()`.
До `activate()` модули и события не работают.

---

## 3. Модуль — быстрый старт

Минимальный модуль (по образцу `FullBright`):

```java
package dev.loki.lovisual.features.module.modules.misc;

import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.events.EventHandler;
import dev.loki.lovisual.events.impl.GameTickEvent;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;

@ModuleInfo(
        id = "my_feature",
        displayName = "My Feature",
        aliases = {"mf"},
        category = ModuleCategory.MISC,
        enabledByDefault = false
)
public class MyFeature extends Module {

    private final NumberValue<Integer> amount =
            num("myFeatureAmount", 3, 0, 20);   // имя_конфига, дефолт, мин, макс

    @Override
    public void onEnable() {
        // вызывается при включении
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onTick() {
        // каждый игровой тик, если модуль включён
        if (!isEnabled()) return;
        int a = amount.get();
    }
}
```

### `@ModuleInfo`
```java
id = "имя"                 // нижний регистр, уникальный, это ключ конфига и команды
displayName = "Имя"        // показывается в UI; можно i18n-ключ
aliases = {}               // необязательно
category = ModuleCategory.XXX
enabledByDefault = false
description = ""           // или i18n-ключ "module.<id>.description"
```

### Автогенерируемые настройки модуля
У каждого модуля уже есть (создаются в конструкторе `Module`):
- `enabled` (BooleanValue) — состояние,
- `bind` (KeyBindSetting, значение `KeyBindValue`) — клавиша,
- `activation_source`,
- `show_in_module_list`.

`postInit()` вызывается автоматически лоадером: читает поля-настройки, делает
`SettingFactory.fromDefs(...)`, регистрирует модуль в `ModuleManager`.

### Хуки жизненного цикла (переопределяемые)
| Метод | Когда |
|-------|-------|
| `onEnable()` / `onDisable()` | переключение модуля |
| `onTick()` | каждый игровой тик (в мире) |
| `onFrame(float tickDelta)` | каждый кадр |
| `onKey(int key, int action)` | нажатия клавиш |
| `onRender2D(GuiGraphicsExtractor)` | vanilla HUD pass |
| `onRenderHudEngine(Renderer2D, TextRenderer, ...)` | кастомный 2D-рендер (см. §6) |
| `onRenderHudEngineForeground(...)` | 2D поверх всего |
| `onRenderWorld(PoseStack, SubmitNodeCollector)` | 3D vanilla-style |
| `onRenderWorldEngine(Renderer3D, Renderer3D, float)` | 3D через движок (см. §7) |
| `getHudPhase()` / `getWorldPhase()` | фаза, в которой рендерить |

> `getHudPhase()`/`getWorldPhase()` по умолчанию `NONE` — модуль вообще не рендерится
> диспетчером. Верните фазу, чтобы рендер-хуки вызывались (см. §6/§7).

### Управление состоянием
- `isEnabled()`, `setEnabled(boolean)`, `setEnabled(boolean, ModuleActivationSource)`, `toggle()`
- `isAvailable()` / `getAvailabilityReason()` — переопределите `getUnavailableReason()`
- `name()`, `getDisplayName()`, `getAliases()`, `getCategory()`, `getDescription()`
- `saveConfig()`, `getConfigValue(String)`, `getSettings()`

### Доступ к другим модулям
```java
import dev.loki.lovisual.features.module.Modules;

Freecam fc = Modules.get(Freecam.class);      // null если не активен/не зарегистрирован
boolean on = Modules.enabled("freecam");      // по id
```
`ModuleManager.get(String id)` / `ModuleManager.get(Class)` / `ModuleManager.require(Class)` тоже доступны.

---

## 4. Настройки (ConfigValue → Setting)

Базовый класс: `config/values/ConfigValue<T>` — `get()`, `set(v)`, `getName()`,
`toJson()`, `fromJson(Object)`, `toDisplay()`.

Внутри модуля настройки создаются **хелперами** в `Module` (все `protected final`).
Имя в конфиге и `settingId` (i18n) могут различаться.

### Хелперы в Module / BaseHudElement
| Хелпер | Тип | Пример |
|--------|-----|--------|
| `bool(name, def)` | `BooleanValue` | `bool("my_on", true)` |
| `num(name, def, min, max)` | `NumberValue<Integer/Long/Float/Double>` | `num("my_num", 3, 0, 20)` |
| `mode(name, def, options...)` | `ModeValue` (String) | `mode("my_mode", "A", "A", "B", "C")` |
| `enumMode(name, defEnum)` / `enumSetting(name, defEnum, ...)` | `EnumValue<E>` | см. `Reach` |
| `color(name, "#AARRGGBB")` | `RGBAColorValue` | `color("my_bg", "#F7343434")` |
| `colorNoAlpha(name, "#RRGGBB")` | `RGBColorValue` | `colorNoAlpha("my_fg", "#FFFFFF")` |
| `text(name, def)` | `StringValue` | `text("my_text", "")` |
| `textList(name)` / `textList(name, pickerMode)` | `SetValue` (множество строк) | `textList("my_list")` |
| `itemList(name)` | `ItemIdSetValue` (список предметов) | `itemList("my_items")` |
| `group(name, defaultsMap)` | `BooleanMapValue` (группа чекбоксов) | `group("gui_sounds", Map.of(...))` |
| `bind(name, def, mode)` | `KeyBindValue` | `bind("my_bind", "R", BindMode.PRESS)` |
| `action(name, defaultKey, mode)` | `FunctionBindSetting` | см. ниже |
| `setDefaultBind("KEY")` | — | клавиша модуля (см. `ClickGui`) |

**Варианты с общим i18n** (`*Common`): `boolCommon`, `numCommon`, `modeCommon`,
`enumCommon`, `color` (без common) — берут схему из `CommonSettingSchemas`:

```java
distance = numCommon("reachDistance", "range", CommonSettingSchemas.COMBAT_RANGE, 4.5, 3.0, 6.0);
mode     = enumCommon("reachMode", "mode", CommonSettingSchemas.ANTICHEAT_MODE, Mode.NORMAL, Mode.values());
```

### Условная видимость / доступность
```java
private final NumberValue<Double> wallDistance =
        visibleWhen(numCommon("reachWallDistance", "wall_range", schema, 3.0, 0.0, 6.0),
                () -> raycast.get() == RaycastMode.THROUGH_WALLS);

// недоступен (показывается серым + причина):
appliesWhen(mySetting, () -> mc.player != null, "Только в мире");
```

### Экшены (вторая кнопка на модуль)
```java
action("climb", "G", BindMode.PRESS);   // создаёт FunctionBindSetting

// в тике:
if (isActionPressedOnce("climb")) { ... }   // фронт нажатия
if (isActionHeld("climb")) { ... }          // удержание
```

### Настройки в HUD-элементах
У `BaseHudElement` те же хелперы (`bool`, `num`, `mode`, `enumSetting`, `color`,
`textList`, `bind`, `group`, `visibleWhen`), но объявление идёт в
`defineSettings(List<SettingDef>)` (см. §9).

### Виджеты в UI (SettingDef)
`SettingDef` — GUI-нейтральный дескриптор. `SettingFactory.fromDef(def)` превращает
его в виджет. Доступные `Kind`: `BOOLEAN, NUMBER, MODE, COLOR, COLOR_NO_ALPHA,
TEXT, TEXT_LIST, COOLDOWN_RULES, PROTOCOL_HEURISTICS, GROUP, BIND`.
Для модулей SettingDef строятся автоматически из хелперов — руками не нужно.
`@DisableSettingI18n(name = false, options = true)` — отключить i18n у настройки.

---

## 5. События (EventBus)

`Events.BUS.post(event)` — доставка хендлерам. Хендлер — любой метод с одним
аргументом-наследником `Event`, помеченный `@EventHandler(priority = N)`.
Подписчики регистрируются автоматически при `ModuleManager.register()` (модули),
или явно `Events.BUS.register(obj)` (в `LoVisual.onInitializeClient`).

Модули-подписчики автоматически гейтятся: если модуль выключен — его хендлеры не вызываются.
Поэтому в хендлерах модуля проверка `if (!isEnabled()) return;` не обязательна, но
встречается в коде для ясности.

`Event` — база с `cancelled`/`cancel()`. Наследование событий поддерживается.

Полный список `events/impl/` (по имени файла):

```
AttackEntityEvent  BlinkPacketEvent  CombatProtocolBossbarEvent
CrosshairTargetUpdateEvent  EventBreakBlock  EventCollision  EventPostSync
EventPushOutOfBlocks  EventSync  EventTargetChanged  FireworkEvent  GameTickEvent
I18nPreflightCollectEvent  KeybindIsPressedEvent  KeyInputEvent  LightmapEvent
LightmapModifyEvent  MovementInputEvent  PacketEvent  PlayerJumpEvent  PlayerMoveEvent
PlayerSafeWalkEvent  PlayerStepEvent  PlayerStepSuccessEvent  PlayerVelocityStrafe
PostPlayerUpdateEvent  PvpChatEvent  PvpOverlayEvent  PvpTabEvent
RenderPrewarmCollectEvent  RotationUpdateEvent  SprintControlEvent
```

Типичные события для нового модуля:
- `GameTickEvent` — каждый тик,
- `PacketEvent` — сетевые пакеты,
- `AttackEntityEvent`, `EventTargetChanged` — бой/цели,
- `LightmapModifyEvent` — свет (пример `FullBright`),
- `PlayerMoveEvent`, `MovementInputEvent` — движение,
- `RenderPrewarmCollectEvent` — «прогреть» шрифты/текстуры для вашего рендера.

Пример:
```java
@EventHandler
private void onTick(GameTickEvent event) { ... }
```

---

## 6. 2D-рендер (HUD)

### Фазы
Верните не-`NONE` фазу из `getHudPhase()`:
```java
HudPhase: FIRST, BEFORE_MISC_OVERLAYS, AFTER_MISC_OVERLAYS, AFTER_BOSS_BAR,
          BEFORE_DEMO_TIMER, BEFORE_CHAT, AFTER_SUBTITLES, LAST
```
`ModuleManager.renderHudEngine(phase, renderer, textRenderer, ctx, tickDelta)` вызывает
у модуля `onRenderHudEngine(...)`; `renderHudEngineForeground` → `onRenderHudEngineForeground`.

### Renderer2D — главный 2D API
Включается/выключается через `begin()` / `render()` (уже делает диспетчер).
Готовые методы (фасад `Renderer2D.java`, все в `px`):

- **Фигуры:** `quad(x,y,w,h,argb)`, `quad(x,y,w,h,cTL,cTR,cBR,cBL)`,
  `roundedRect(x,y,w,h,radius,argb)`, `roundedRectStroke(...)`,
  `roundedRectGradient(...)`, `roundedRectGlow/Shadow/SoftShadow(...)`,
  `circle(cx,cy,r,argb)`, `circleStroke(...)`, `arcStroke(...)`,
  `line(x1,y1,x2,y2,argb)`, `boxLines(...)`, `roundedSmokeFill(...)`.
- **Текст:** см. §6.2.
- **Предметы:** `item(ItemStack, x, y, scale)`, `itemUnscaled(...)`, `itemPivot(...)`.
- **SVG/текстуры:** `svg(Identifier/Path, x, y, w, h)`, `textureQuad(...)`, `msdfTextureQuad(...)`.
- **Effect (стеклянные панели):** `effect(UiEffectSpec)`.

### Текст
```java
TextRenderer tr = Fonts.renderer("Onest", FontInfo.Type.Regular, textRenderer);
// или Fonts.renderer("OnestMedium", Regular), "OnestBold", "Inter", "Iosevka", "Icons"...
tr.begin(scale, true, false);
double w = tr.getWidth("text", false);
double h = tr.getHeight(false);
tr.render("text", x, y, new RenderColor(0xFFFFFFFF), false);
tr.end();
```
Универсальная запись «в одном блоке»:
```java
textRenderer.begin(scale, true, false);
textRenderer.render("FPS", x, y, new RenderColor(argb), false);
textRenderer.end();
```
`TextRenderer.get()` — текущий рендерер (передаётся в хук). `HudScale.scale(screenW, screenH)`
даёт масштаб для адаптивного HUD (см. `Fps`).

### Иконки / текстуры
Идентификаторы: `Identifier.fromNamespaceAndPath("lovisual", "textures/hud/elements/fps.png")`.
SVG лежат в `assets/lovisual/svg/`.

### Анимации
`AnimationUtility`: `approach(value, target, dt, speed)`, `fast(...)`, `lerp(...)`,
`easeOutCubic`, `easeInOutCubic`, `easeOutBack`, `blink(ms)`, `deltaTime()`, `snap(...)`.

### Хелперы
- `HudRenderUtil`: `setAlpha(argb, a)`, `mixColor(a, b, t)`, `glassBackground()`,
  `drawLiquidGlass(...)`, `animateVisibility(...)`, `visibilityScale(...)`.
- `HudGlobalConfig.get()`: `getFontSize()`, `getBlurRadius()` и т.д.
- Тема: `Theme.theme()` (статический импорт `dev.loki.lovisual.features.theme.Theme.theme`)
  → `Themes.Theme` c полями `windowBg, windowHeader, windowStroke, surface, surfaceHover,
  cardEnabled, cardDisabled, textPrimary, textMuted, accent, accentSoft, strokeSoft`
  (всё `int` ARGB). См. `Fps.updatePalette()`.

---

## 7. 3D-рендер (мир)

### Фазы
Верните не-`NONE` фазу из `getWorldPhase()`:
```java
WorldPhase: BEFORE_ENTITIES, AFTER_ENTITIES, BEFORE_TRANSLUCENT, END_MAIN, AFTER_POST_PROCESS
```

### Renderer3D — простой путь
В хуке `onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta)`:

```java
renderer.begin();
renderer.line(x1, y1, z1, x2, y2, z2, r, g, b, a);   // линия
renderer.quad(x1,y1,z1, x2,y2,z2, x3,y3,z3, x4,y4,z4); // квад
renderer.triangle(...);
renderer.render(new PoseStack());   // в конце
```
Пример: `Tracers.onRenderWorldEngine` (линии от камеры к игрокам).

### MeshBuilder + RenderPipeline — продвинутый путь
```java
MeshBuilder mesh = new MeshBuilder(LoVisualRenderPipelines.UI_TEXTURED_ADDITIVE);
mesh.begin();
mesh.ensureQuadCapacity();
int i1 = mesh.vec2(x, y).vec2(u, v).color(r, g, b, a).next();
int i2 = mesh.vec2(...).vec2(...).color(...).next();
mesh.quad(i1, i2, i3, i4);
mesh.end();
```
Готовые пайплайны в `LoVisualRenderPipelines` (фрагмент):
```
UI_COLORED, UI_COLORED_LINES, UI_TEXTURED, UI_TEXTURED_ADDITIVE, UI_TEXT, UI_TEXT_MSDF,
UI_SVG_MSDF, UI_BLUR, WORLD_COLORED, WORLD_COLORED_LINES, WORLD_COLORED_DEPTH,
WORLD_TEXTURED, WORLD_TEXTURED_DEPTH, WORLD_TEXT, WORLD_TEXT_MSDF, WORLD_DECAL_SDF,
WORLD_COLORED_LINES_DEPTH, WORLD_WIDE_COLORED_LINES, ...HAND_*, SHADER_ESP_*, POST_FX...
```
Отправить на отрисовку (2D-стрелки из `Tracers`):
```java
MeshRenderer.begin()
        .attachments(mc.gameRenderer.mainRenderTarget())
        .pipeline(LoVisualRenderPipelines.UI_TEXTURED_ADDITIVE)
        .mesh(mesh).transform(matrix4f)
        .sampler("u_Texture", view, sampler)
        .end();
```
`Renderer3D.Cull` — утилиты отсечения: `isInFrustum(...)`, `isSectionVisible(...)`, `isInFront(...)`.

---

## 8. Полезные утилиты (`util/`)

| Пакет / класс | Что даёт |
|---------------|----------|
| `util.target.TargetManager` | текущая цель: `getTarget()`, `getTarget(true/false)`, `onAttack()`, `setPredictionTarget()`, `Source` |
| `util.pvp.PvpTracker`, `PvpTargetTracker` | статистика PvP / отслеживание цели |
| `util.aiming.RotationManager` | плавные/снап-ротации |
| `util.combat.VulcanReachController` | клампинг дистанции (режим античита) |
| `util.time.GameClock` | `millis()` — игровое время (тики×50мс), вне мира — стенное |
| `util.time.TimerController` | таймеры |
| `util.input.KeyManager` | `wasPressed("func")`, `isHeld("func")`, `isComboHeldAllowScreen(...)` |
| `util.screen.ClientScreen` | текущий экран: `ClientScreen.current(mc)` / `.show(mc, screen)` |
| `util.text` | текст-хелперы |
| `util.entity.simulation.PlayerSimulationCache`, `BoatSimulationCache` | предсказание позиций |
| `util.raycast` | рейкасты |
| `util.item.OmniItemUtils` | утилиты предметов |
| `util.combat.protocol.CombatProtocolHeuristics` | эвристики античита |
| `util.logging.DebugLog` | `DebugLog.info/warn/error/config(...)` (printf-style) |
| `util.resources.RenderResourceReadiness` | готовность рендер-ресурсов |
| `features.relations.CategoryService` | `isFriend(Player)`, `getColor(Player)` — категории игроков |
| `util.FastFps` | `getFps()` — быстрый FPS без накладок |

Ленивый доступ к MC:
```java
private final Minecraft mc = Minecraft.getInstance();
```
(поле, но это ок — поле инициализируется при создании модуля, а модули создаются
в рантайме; правило «никакого `static final Minecraft` в полях»).

---

## 9. HUD-элементы

### Draggable (перетаскиваемый) — пример `Fps`
```java
@HudElementInfo(id = "fps", displayName = "FPS", enabledByDefault = true, order = 120)
public final class Fps extends DraggableHudElement {
    // настройки — прямо поля:
    private final NumberValue<Double> scale = num("fps_scale", 2.37, 0.5, 5.0);
    private final RGBColorValue iconColor = visibleWhen(colorNoAlpha("fps_icon_color", "#FFFFFF"), this::isCustomMode);

    @Override
    public void applyDefaultPosition(int screenW, int screenH) { this.x = 16f; this.y = 20f; }

    @Override
    public boolean usesEngineRenderer() { return true; }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer,
                             GuiGraphicsExtractor ctx, float tickDelta,
                             int screenW, int screenH) {
        if (!preview && !isEnabled()) { width = 0f; height = 0f; return; }
        // считаем width/height, рисуем через renderer + Fonts.renderer(...)
    }
}
```
- Настройки объявляются как поля с хелперами (автоматом попадают в SettingDef).
- `defaultLayout(x, y, anchorX, anchorY)` / `defaultLinkedLayout(...)` — стартовое положение.
- `x, y, width, height` — позиция и размер (обновляйте width/height в renderEngine!).
- Регистрация автоматическая (сканирование по `@HudElementInfo`).
- Интерактив: `isMouseOverInteractive(mx, my)`, `onMouseClicked(mx, my, button)`.

### Nondraggable (статичный)
Классы в `features/gui/hud/nondraggable/impl/` (CustomBar, CustomHealthBar, CustomHotbar,
BetterTooltips, BetterButtons, DynamicIsland). Наследуют `BaseHudElement`, рендер через
`renderEngine(...)` + `getRenderSpace()`/`getHudPhase()`.

---

## 10. Команды

Интерфейс `ClientCommand` + аннотация `@CommandInfo`, регистрация — вручную в `CommandManager`.

```java
@CommandInfo(
        id = "toggle",
        aliases = "t",
        usage = "@toggle <module> [on|off|toggle]",
        descriptionKey = "command.toggle.description"
)
public final class ToggleCommand implements ClientCommand {
    @Override
    public boolean execute(CommandContext ctx) {
        // ctx.arg(0), ctx.arg(1), ...
        CommandOutput.success("Done");
        return true;
    }

    @Override
    public List<String> suggest(CommandContext ctx, int argIndex, String token) {
        return List.of();
    }
}
```
- `CommandContext(mc, raw, name, args)` — `arg(i)` возвращает null если нет.
- `CommandOutput` — `success/warning/error/send(String)` с префиксом `[LoVisual]`.
- `CommandManager.init()` регистрирует команды из `impl/` (через ClassGraph, как модули).

---

## 11. API для аддонов (`api/v0/`)

- `LoVisualAddon` — entrypoint `"lovisual:addon"`: `onConfigureModules`,
  `onInitialize(LoVisualAddonContext)`, `onClientReady`, `onShutdown`.
- `LoVisualModuleExtension` — расширение модуля (`ModuleExtensionContext`),
  перехватывает `beforeEnable/afterEnable/beforeTick/...` (см. `ModuleExtensionManager`).
- `LoVisualClientApi.get()` — `modules()`, `module(id)`, `isModuleEnabled(id)`, `setModuleEnabled`.
- `render/` — `LoVisualRenderCallback` + `LoVisualRenderStage`
  (HUD_RAW, HUD_SCALED, HUD_LOGICAL, WORLD_*, SCREEN_*), `LoVisualPostProcessCallback`,
  `LoVisualUniforms`, `LoVisualRenderPipelineBuilder`.
- `clickgui/` — `LoVisualClickGuiSection`, `LoVisualClickGuiRenderContext` — свои секции в кликгую.

---

## 12. i18n (языковые ключи)

Файлы: `src/main/resources/assets/lovisual/lang/en_us.json`, `ru_ru.json`.

Соглашения по ключам:
- Модуль: `module.<id>`, `module.<id>.name`, `module.<id>.description`,
  настройка: `module.<id>.setting.<setting_id>`, опции: `module.<id>.option.<option>`.
- HUD draggable: `hud.draggable.<id>` / `setting.draggable.<id>.<setting>`.
- HUD static: `hud.static.<id>`.
- Команды: `command.<id>.description`.
- Общие схемы: `CommonSettingSchemas` указывают `commonI18nKeys` (например `combat.range`)
  — они ищутся в lang-файлах с префиксом `setting.common.<key>` (проверьте по `CommonKey`).

`I18n.get("key")` — получение перевода. Если ключа нет — возвращается сам ключ.
В `Module.getDisplayName()`/`getDescription()` fallback на `displayName`/`description`.

---

## 13. Миксины и accessors

- Все миксины в `mixins/` (`@Mixin(...)`). Пакеты: `accessors`, `iris`, `sodium`, `moreculling`,
  `xaero`, `security`.
- Accessors в `mixins/accessors/` — примеры: `MinecraftAccessor`, `LocalPlayerAccessor`,
  `PlayerInventoryAccessor`, `LivingEntityAccessor`, `EntityAccessor`, `ItemCooldownManagerAccessor`.
- Реестр: `resources/lovisual.mixins.json`. `LoVisualMixinPlugin` — условная загрузка.
- Правило: «никаких мёртвых миксинов» — добавляйте миксин только если реально нужен.
- `mixininterface/` — интерфейсы-протуберанцы для доступа к данным из миксинов (`IGuiGraphics`, `IEntity`).

---

## 14. Чек-лист нового модуля

1. `@ModuleInfo` на классе в `features/module/modules/<категория>/`.
2. Унаследовать `Module`, переопределить `onEnable/onDisable/onTick` и т.д.
3. Настройки — поля с хелперами (`num`, `bool`, `mode`, `color`, `visibleWhen`…).
4. Если нужен рендер — вернуть `getHudPhase()`/`getWorldPhase()` и реализовать
   `onRenderHudEngine`/`onRenderWorldEngine`.
5. Если нужны события — методы с `@EventHandler`.
6. `i18n`: добавить `module.<id>.name/description` и ключи настроек в `en_us.json`/`ru_ru.json`.
7. Проверки: `./gradlew build` (сборка), `./gradlew test` (тесты), для чистой логики — JUnit.
8. Файл ≤200 строк — иначе вынести логику в хелпер-классы в том же пакете.

**Не делайте:**
- `static final Minecraft` в полях → только ленивый `Minecraft.getInstance()`.
- `System.currentTimeMillis()` в модулях → `GameClock.millis()`.
- this-escape в конструкторе, `#[allow(dead_code)]`-аналогов (мёртвый код удаляется),
  мёртвые миксины.
