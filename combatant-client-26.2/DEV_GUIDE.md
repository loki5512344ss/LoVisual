# LoVisual Mod — Dev Guide (Единый справочник)

Цель: быстро добавить модуль / HUD-элемент / команду / миксин, не перекапывая весь код. Сводка по реальному коду, объединено из `DEV_GUIDE.md` + `DEVELOPMENT.md`.

- Пакеты: `dev.loki.lovisual.*`
- Стек: **Java 25 · Fabric Loader 0.19.3 · Minecraft 26.2** (Fabric Loom), Lombok
- Правила — нарушение = откат (см. §2)

---

## 1. Сборка и запуск

```bash
cd combatant-client-26.2
./gradlew build        # jar: build/libs/lovisual-<ver>.jar
./gradlew test         # юнит-тесты (~320)
./gradlew runClient    # dev-запуск Minecraft с модом
./gradlew runClient --rerun-tasks   # если кэш врёт
```

Готовый jar для теста в реальном инстансе:

```bash
cp build/libs/lovisual-*.jar /storage/prismlauncher/instances/26.2/minecraft/mods/
```

Ресурсы: **только настоящие PNG** (сигнатура `89 50 4E 47`). JPEG, переименованный в `.png`, уронит `Bad PNG Signature` при загрузке.

---

## 2. Правила проекта (ОБЯЗАТЕЛЬНЫЕ)

- **KISS / DRY / SOLID** — просто, без оверинжиниринга.
- **≤200 строк на файл.** Фасад-делегат (`Renderer2D`, `Module` ≈1181) — исключение; больше логики → выноси `package-private` хелперы в тот же пакет.
- **≤4 файла на папку** — больше → новая подпапка по смыслу.
- **Никакого статика от Minecraft**: `Minecraft.getInstance()` только локальной переменной в месте использования, никогда в поле. `INSTANCE` — только для сервисов без lifecycle.
- **Lifecycle гейты**: `Lifecycle.isActive()/canRunRender()/canRunHud()` — до `activate()` события/тики не работают; `EventBus.post()` сам гейтит.
- **Никаких this-escape** — конструктор не публикует `this`.
- **Никаких мёртвых миксинов** — миксин только если реально нужен; `cancellable=true` только при реальном `cancel`.
- **`dev.loki.lovisual.mixins.*` — ТОЛЬКО классы с `@Mixin`.** Обычный хелпер там крашит `IllegalClassLoadError` (Knot). Хелперы — в `util/` или пакет фичи (см. `HandDynamics`).
- **Без пустышек** — файл без реализации не создаётся.
- **Каждый этап компилируется**: `./gradlew build` перед коммитом.
- **Тесты обязательны** для новой чистой логики (JUnit 5, без моков Minecraft — выноси математику в `static` и тесть).
- **Референсы (`ref/`)** — чужой код (SoupVisuals SOUP-1.0, Delta и др.): **только идеи**, пере-реализация под наши правила, никакого копирования.
- **`System.currentTimeMillis()` в модулях → `GameClock.millis()`**, `static final Minecraft` в полях — баг.

---

## 3. Карта пакетов

```
dev.loki.lovisual
├── LoVisual.java                 — ClientModInitializer: старт (тики, рендер-фазы)
├── Lifecycle.java                — INIT → ACTIVE → SHUTDOWN; гейты
├── events/                       — EventBus (@EventHandler, приоритеты, гейт по Module)
│   └── impl/                     — GameTickEvent, PacketEvent, AttackEntityEvent, LightmapModifyEvent...
├── config/                       — MainConfig, SettingDef, .lvcfg (legacy .cbcfg читается)
│   ├── values/                   — ConfigValue: bool/num/mode/color/set/... (есть тесты)
│   └── profile/                  — профили (collect/apply/diff/binary codec)
├── features/
│   ├── module/                   — СИСТЕМА МОДУЛЕЙ (главное)
│   │   ├── Module.java           — базовый класс
│   │   ├── ModuleInfo            — @annotation
│   │   ├── ModuleManager         — реестр + диспетчер фаз
│   │   ├── ModuleAutoLoader      — ClassGraph по @ModuleInfo (no-args ctor → postInit)
│   │   ├── Modules               — Modules.get(Класс.class) / enabled(id)
│   │   ├── ModuleCategory        — COMBAT/MOVEMENT/PLAYER/VISUALS/MISC (легит-направление: читы удаляем)
│   │   ├── HudPhase / WorldPhase — фазы рендера
│   │   └── modules/{combat,player,visuals,misc}/
│   ├── command/                  — @CommandInfo + ClientCommand (ClassGraph, префикс %)
│   ├── gui/
│   │   ├── clickgui/             — экран настроек (Setting → виджет)
│   │   └── hud/                  — HUD-элементы
│   │       ├── draggable/        — перетаскиваемые (Fps, Coords, ModuleList, TargetHud…)
│   │       └── nondraggable/     — статичные (CustomBar, BetterChat, BetterButtons, DynamicIsland…)
│   ├── relations/                — друзья/враги (PlayerRelations, CategoryService)
│   ├── security/                 — BackdoorProtection (SSRF/translate)
│   └── theme/                    — Themes.Theme (windowBg/header/surface/accent...), Theme.theme()
├── render/engine/                — Renderer2D/3D, пайплайны, шрифты, MeshBuilder
│   ├── renderer/Renderer2D       — 2D (quad/roundedRect/circle/line/item/svg/effect)
│   ├── renderer/Renderer3D       — 3D (line/quad/triangle)
│   ├── text/                     — Fonts.renderer("Onest", Regular), TextRenderer
│   ├── animation/AnimationUtility
│   ├── pipeline/LoVisualRenderPipelines — UI_COLORED, WORLD_COLORED, HAND_*, SHADER_ESP_*, POST_FX...
│   └── uniform/MeshBuilder
├── mixins/                       — @Mixin-классы (mixins.json, LoVisualMixinPlugin)
│   └── mixins/accessors/         — MinecraftAccessor, PlayerInventoryAccessor...
│   └── mixininterface/           — IGuiGraphics, IEntity...
├── addon/ + api/v0/              — система аддонов (LoVisualAddon, LoVisualModuleExtension, RenderCallback)
└── util/                         — хелперы: target, pvp, aiming, time/GameClock, input/KeyManager, screen, text, raycast...
```

---

## 4. Жизненный цикл и старт

В `LoVisual.onInitializeClient()` по порядку:
1. `LoVisualRenderEngineBootstrap.init()`, `PostProcessManager.register(...)`
2. `MainConfig.get()`, `AccountConfig.get()`, `CommandManager.init()`, `MediaSessionService`
3. `ModuleAutoLoader.load("dev.loki.lovisual.features.module.modules")` — ищет `@ModuleInfo`, `new` + `postInit()`
4. `ModuleManager.loadAllModuleConfigs()` — применяет `.lvcfg`, включает `enabled`
5. `HudElements.init()`, `StaticHudElementBootstrap.init()`
6. `Events.BUS.register(...)` — глобальные сервисы (`RotationManager`, `PvpTracker`...)
7. `Lifecycle.activate()` — после этого тики/события/рендер работают

**Вывод:** новый модуль — просто класс с `@ModuleInfo` в `...modules.<категория>`; регистрировать руками не нужно.

---

## 5. Модуль — быстрый старт

```java
package dev.loki.lovisual.features.module.modules.misc;

import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.events.impl.render.GameTickEvent;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;

@ModuleInfo(id = "my_feature", displayName = "My Feature", category = ModuleCategory.MISC)
public class MyFeature extends Module {
    private final NumberValue<Integer> amount = num("myFeatureAmount", 3, 0, 20);

    @Override public void onEnable() {}
    @Override public void onDisable() {}
    @Override public void onTick() {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
    }
    @EventHandler private void onTick(GameTickEvent e) {}
}
```

`@ModuleInfo`: `id` (snake, уникальный, ключ конфига/команды), `displayName`, `aliases`, `category`, `enabledByDefault`, `description` (i18n).

Автогенерируемые поля `Module`: `enabled`/`bind`/`activation_source`/`show_in_module_list` (`postInit` → `SettingFactory.fromDefs` → `ModuleManager`).

Хуки: `onEnable/onDisable/onTick/onFrame/onKey/onRender2D/onRenderHudEngine/onRenderWorldEngine`, `getHudPhase()/getWorldPhase()` (по умолч. `NONE` — не рендерится). Управление: `isEnabled()/toggle()/setEnabled()`, `isAvailable()`, `Modules.get(Freecam.class)`.

---

## 6. Настройки (ConfigValue → SettingDef)

База `ConfigValue<T>`: `get()/set()/getName()/toJson()/fromJson()`.

Хелперы в `Module`/`BaseHudElement` (`protected final`):

| Хелпер | Тип | Пример |
|--------|-----|--------|
| `bool(name, def)` | `BooleanValue` | `bool("my_on", true)` |
| `num(name, def, min, max)` | `NumberValue` | `num("my_num", 3, 0, 20)` |
| `mode(name, def, opts...)` | `ModeValue` | `mode("my_mode", "A", "A","B")` |
| `enumMode(name, defEnum)` | `EnumValue` | см. `Reach` |
| `color(name, "#AARRGGBB")` | `RGBAColorValue` | `color("my_bg", "#F7343434")` |
| `text/textList/itemList/group/bind/action` | `StringValue/SetValue/ItemIdSetValue/...` | `itemList("my_items")` |

`*Common` варианты (`boolCommon/numCommon/enumCommon`) берут схему из `CommonSettingSchemas`.

Условность: `visibleWhen(numCommon(...), () -> cond)`, `appliesWhen(setting, () -> mc.player!=null, "Только в мире")`. Экшены: `action("climb","G", PRESS)` → `isActionPressedOnce("climb")`.

HUD-элементы объявляют настройки в `defineSettings(List<SettingDef>)`; модули — полями (автоматом). Виджеты `SettingDef.Kind`: `BOOLEAN, NUMBER, MODE, COLOR, TEXT, TEXT_LIST, GROUP, BIND...` (`@DisableSettingI18n`).

---

## 7. HUD-элементы

**Draggable** (`hud/draggable/impl/`, `@HudElementInfo(id, displayName, enabledByDefault, order)`, `DraggableHudElement`): `applyDefaultPosition(w,h)`, `renderEngine(Renderer2D, TextRenderer, GuiGraphicsExtractor, tickDelta, w,h)`, `usesEngineRenderer()=true`, `x/y/width/height`.

**Static** (`hud/nondraggable/impl/`, `@HudElementRegister(order)`, `AbstractHudElement` `super(id,"Title",defaultEnabled)` + `INSTANCE`): для вкладки UI добавить `new StaticHudCard(...)` в `MenuSettingsResolver.STATIC_UI_CARDS`. Тема: `Theme.theme().accent()/windowBg()`, `HudRenderUtil.setAlpha/mixColor`, текст `Fonts.renderer(...)`.

---

## 8. Команды

`ClientCommand` + `@CommandInfo(id, aliases, usage, descriptionKey)`, регистрация в `CommandManager` (ClassGraph). Префикс **`%`** (legacy `@`). `CommandPrefix` — источник истины; автокомплит `suggest()` + `UsageHints`.

```java
@CommandInfo(id="toggle", aliases="t", usage="%toggle <module> [on|off]", descriptionKey="command.toggle.description")
public final class ToggleCommand implements ClientCommand {
    public boolean execute(CommandContext ctx){ CommandOutput.success("Done"); return true; }
    public List<String> suggest(CommandContext ctx,int i,String token){ return List.of(); }
}
```

`CommandContext.arg(i)`, `CommandOutput.success/warning/error`.

---

## 9. События (EventBus)

`Events.BUS.post(event)` → `@EventHandler(priority=N)` (один аргумент-`Event`). Модули гейтятся `isEnabled()`. `Event` имеет `cancel()`.

`events/impl/` : `GameTickEvent`, `PacketEvent`, `AttackEntityEvent`, `EventBreakBlock`, `LightmapModifyEvent`, `PlayerMoveEvent`, `RenderPrewarmCollectEvent`...

```java
@EventHandler private void onTick(GameTickEvent e){ }
```

---

## 10. 2D-рендер (HUD)

Фазы `getHudPhase()`: `FIRST, BEFORE_MISC_OVERLAYS, AFTER_MISC_OVERLAYS, AFTER_BOSS_BAR, BEFORE_DEMO_TIMER, BEFORE_CHAT, AFTER_SUBTITLES, LAST` → `onRenderHudEngine`.

`Renderer2D` (фасад, `begin()/render()` делает диспетчер): `quad/roundedRect/roundedRectGradient/circle/line/item/svg/effect`. Текст: `Fonts.renderer("Onest", Regular, textRenderer).begin(scale,true,false) → getWidth/render → end()` или `textRenderer.begin...`. Анимации `AnimationUtility`, хелперы `HudRenderUtil`/`HudGlobalConfig`/`Theme.theme()`.

---

## 11. 3D-рендер (мир)

Фазы `getWorldPhase()`: `BEFORE_ENTITIES, AFTER_ENTITIES, BEFORE_TRANSLUCENT, END_MAIN, AFTER_POST_PROCESS` → `onRenderWorldEngine(Renderer3D, Renderer3D, tickDelta)`.

Простой путь: `renderer.begin(); renderer.line(...); renderer.quad(...); renderer.render(new PoseStack());` (см. `Tracers`). Продвинутый: `MeshBuilder` + `LoVisualRenderPipelines` (`UI_COLORED, WORLD_COLORED...`). `Renderer3D.Cull.isInFrustum`.

---

## 12. Миксины и accessors

Все в `mixins/` (`@Mixin`), реестр `lovisual.mixins.json` (`LoVisualMixinPlugin`). Accessors в `mixins/accessors/` (`PlayerInventoryAccessor`...), `mixininterface/` (`IGuiGraphics`). Правило: только `@Mixin` в `mixins.*`.

---

## 13. Конфиг, профили и i18n

Модули/HUD → `<game>/lovisual/*.lvcfg` (legacy `.cbcfg` читается). Профили `config/profile/` (ClickGUI → Configs).

i18n `assets/lovisual/lang/{en_us,ru_ru}.json`:
- модуль `module.<id>.description`, `setting.<id>.<setting_id>`, `option.<Option>`
- draggable `hud.draggable.<id>`, static `setting.hud_element.<id>.<key>`, команды `command.<id>.description`, общие `setting.common.<key>`

Пропущенный ключ ловит `MissingI18nReporter` в dev.

---

## 14. Полезные утилиты (`util/`)

`TargetManager`, `PvpTracker`, `RotationManager`, `GameClock.millis()`, `TimerController`, `KeyManager`, `ClientScreen`, `PlayerSimulationCache`, `OmniItemUtils`, `DebugLog`, `RenderResourceReadiness`, `CategoryService`, `FastFps`...

Ленивый `Minecraft.getInstance()` — только локально.

---

## 15. API для аддонов (`api/v0/`)

`LoVisualAddon` (`onConfigureModules/onInitialize/onClientReady/onShutdown`), `LoVisualModuleExtension` (`beforeEnable/afterEnable/beforeTick...`), `LoVisualClientApi.get().modules()/isModuleEnabled()`, `LoVisualRenderCallback` (`HUD_RAW, WORLD_...`), `LoVisualClickGuiSection`.

---

## 16. Тесты

`src/test/java/` зеркалит `main`, **только чистая логика** (без MC рантайма): `util/text/NameProtectorTest`, `config/values/*Test`, `config/profile/ConfigProfileValueCodecTest`, `events/EventBusTest` (нужен `Lifecycle.activate()`).

Выноси `static int tint(...)` / `record Layout(...)` и тесть.

---

## 17. Чек-лист нового модуля / перед коммитом

1. `@ModuleInfo` в `modules/<категория>/`, `extends Module`, `onEnable/onTick` + `@EventHandler` если нужно
2. Настройки хелперами (`num/bool/color/visibleWhen`), `i18n` в оба `lang` файла
3. Рендер: верни `getHudPhase()/getWorldPhase()` + `onRenderHudEngine/onRenderWorldEngine`
4. `./gradlew build` зелёный, `./gradlew test` зелёный, файл ≤200, папка ≤4, нет `static Minecraft` в полях, нет `System.currentTimeMillis()` → `GameClock`, нет пустышек/мёртвых миксинов, `mixin` только `@Mixin`
5. `TODO.md` отмечен, `ref/` не копировался

**Не делай:** `static final Minecraft` в полях, `this`-escape, мёртвый код, копипаст чужого `ref/`.

---

## 18. Современные рекомендации (2025-2026, web-поиск)

### Clean Code — без догматизма
- **YAGNI** «You Aren't Gonna Need It» — не пиши на будущее; **KISS** > OCP в быстро меняющемся коде. Принцип — эвристика, не закон.
- **DRY ловушка**: дублирование дешевле неверной абстракции (Sandi Metz). Выноси только *семантически* одинаковое, синтаксически похожее `for`-циклы — не повод для хелпера → лишняя связность.
- **SOLID критика**: ортодоксальный SOLID плодит `Service → RepoInterface → RepoImpl → QueryBuilder`, низкая связность, но нечитаемо и 15× медленнее (Casey Muratori «Clean Code, Horrible Performance» 2023: замена полиморфного OCP на `switch` дала 15×). Применяй умеренно, только где абстракция реально нужна.
- **Читаемость**: без глубокой вложенности — guard-clauses `if (user==null) return;`, без магических чисел → `static final MIN_DRINKING_AGE=21`, говорящие имена `customerOrderHistoryList` > `list`, комментарии *почему*, а не *что*, консистентное форматирование, DI вместо `new`.

### Java 21/25 (LTS)
- `if (obj instanceof String s)` / `record Point(int x,int y)` + `if (o instanceof Point(int x,int y))` / `switch` с pattern-matching и `case null`, `sealed interface Command permits Get,Put,Delete` — меньше кастов, безопаснее API.
- **Virtual threads** — не «pixie dust»; дают выигрыш на I/O-bound, но не на CPU-bound. **ScopedValue** (финал JDK25) + **Structured Concurrency** (preview JDK25) лучше `ThreadLocal` по памяти/синхронизации, особенно с виртуальными потоками.
- **JDK25 perf**: `String.hash` `@Stable` → константная свёртка `Map.of("constant",...)`, `Stable Value` preview, `ForkJoinPool` теперь `ScheduledExecutorService` (быстрее отмена timeout), ML-KEM/ML-DSA квантостойкая криптография ×2 на AVX-512/ARM, `HKDF` и др. — просто обнови JDK, код ускорится без изменений.

### Secure Coding (OWASP Java Cheat Sheet)
- **Injection**: только параметризация (`PreparedStatement`, JPQL `setParameter`), не конкатенация строк; для `isReachable` вместо `Runtime.exec("ping "+host)`.
- **XSS**: `OWASP Java HTML Sanitizer` + `OWASP Java Encoder` (`Encode.forHtml`) при выводе к пользователю.
- **Крипто**: никогда не пиши своё — используй JCA/JCE, `AES-GCM` + `ECDH`, каждый раз новый `nonce`, секреты — через cloud secret manager, ключи ротируй. Слабые `MD5/DES` запрещены `JEP 565` в Java 21.

### Fabric / Mixin (2025-2026)
- Порядок предпочтения: `@Inject` (callback, стекается) > `MixinExtras @WrapOperation/@ModifyExpressionValue` > `@Redirect/@ModifyConstant` (только один на таргет → конфликт) > `@Overwrite` (почти никогда).
- `@Unique modid$field`, абстрактный mixin-класс (не надо имплементить интерфейсы родителя), `@Shadow` для доступа, `(Target)(Object)this` для `this`, внутренние классы `Outer$Inner`.
- Сеть: пакеты приходят на net-thread — парсь там, а доступ к `Minecraft`/`Level` только через `client.execute` / task-queue на main thread.
- Избегай `java.awt/javax.swing` — вешает игру; не патчи Fabric API напрямую, ищи `FabricBlockSettings` и т.п.

*Источники: Medium 2025 DRY/KISS/YAGNI, Baeldung Clean Code, JavaCodeGeeks Dark Side SOLID, Oracle Inside Java 2025-2026, Fabric Wiki mixin_tips, OWASP Java Security Cheat Sheet.*

