# LoVisual Mod (combatant-client-26.2) — план переписи

Мод — форк Combatant Client, переписываем под LoVisual.

## Правила (ОБЯЗАТЕЛЬНЫЕ, нарушение = откат)
- **KISS / DRY / SOLID** — простая структура, без оверинжиниринга
- **Макс. 200 строк на файл** (было >1500) — превышение = разбить на модули
- **Макс. 4 файла на папку** — больше = новая подпапка по смыслу
- **Никакого статика** — `Minecraft.getInstance()` только лениво, никогда в поле
- **Никаких this-escape** — конструктор не публикует `this`
- **Никаких мёртвых миксинов** — миксин только если реально нужен; `cancellable=true` только при реальном cancel
- **Без пустышек** — файл без реализации не создаётся
- **Каждый этап компилируется** — `./gradlew build` перед переходом к следующему
- **Тесты обязательны** для новых модулей (JUnit)

---

## СТАТУС ПРОБЛЕМ (что вижу сейчас)

### 1. Ребрендинг не закончен (пункт 2)
- 288 файлов / **1464 упоминания** `combatant` в main-коде
- Классы: `CombatantRenderSystem`, `CombatantRenderEngineBootstrap`, `CombatantRenderPipelines`, `CombatantVulkanBackend`, `CombatantFrameGraph`, `CombatantTerrainVertex` и др.
- Заголовки файлов: «This file is part of the Combatant Client distribution» / копирайт изначального автора

### 2. Файлы-гиганты (пункт 3) — 12 файлов > 1500 строк
| Файл | Строк |
|------|-------|
| render/engine/renderer/Renderer2D.java | 3379 |
| features/gui/chat/BetterChatRenderer.java | 2870 |
| features/gui/clickgui/settings/UnifiedSettingRenderer.java | 2798 |
| features/module/modules/player/Scaffold.java | 2416 |
| features/module/modules/combat/AttributeSwap.java | 1922 |
| features/gui/clickgui/ClickGuiRenderer.java | 1900 |
| features/module/modules/visuals/NameTags.java | 1791 |
| features/module/modules/combat/ElytraTarget.java | 1711 |
| features/gui/hud/draggable/impl/TargetHud.java | 1687 |
| features/module/modules/visuals/ESP.java | 1520 |
| features/module/modules/combat/AutoCrystal.java | 1516 |
| features/gui/hud/nondraggable/impl/CustomBar.java | 1496 |

### 3. Runtime / JarReplacement (пункт 4)
- `runtime/` (ClientRuntime, RuntimeGate, ClientRuntimeState, SOFT_PANIC/JAR_REPLACEMENT_PANIC, shutdown/resume-участники)
- `runtime/loader/` (JarReplacementCoordinator, LoaderBridge, PanicOnlyJarReplacementCoordinator)
- **Моё предложение: выкинуть.** Живая замена jar в памяти — источник classloader-утечек, хрупкая, для клиента не нужна (обновление делает лаунчер). Заменить на простой Lifecycle.

### 4. Security (пункт 7)
- `features/security/BackdoorProtection` — на деле защита от SSRF/translate-инъекций (порт ExploitPreventer, MIT). Полезное, но `enum`-синглтон со статикой.
- `features/relations/StaffTracker` + `StaffHeuristicsMatcher` — детект «стафа» по эвристикам TAB. Сомнительно, решить оставить/выкинуть.

### 5. Тесты (пункт 6)
- 5 тестов на ~213k строк. Добавлять по мере переписи модулей.

---

## Фаза 1: Ребрендинг (пункт 2)
- [x] **Восстановлен `config/`-пакет** (55+3 файла) из `ref/zip/combatant-client-26.2 (1).zip` — он отсутствовал в форке, без него проект не собирался. Переименован `combatant.client.config` → `dev.loki.lovisual.config`. `compileJava` ✓
- [x] Заменить `combatant` → `lovisual` в main-коде (1464 вхождения, 288 файлов)
- [x] Переименовать классы `Combatant*` → `LoVisual*` (23 шт., включая `IrisCombatantFrameHooks` → `IrisLoVisualFrameHooks`)
- [x] Заголовки файлов → LoVisual, пересобрать копирайты (GPLv3 форка)
- [x] Проверить `fabric.mod.json` / `lovisual.mixins.json` на остатки
- [x] Проверить строки/сообщения/логи на «combatant»
- [x] JNI-имена в native: `Java_combatant_client_` → `Java_dev_loki_lovisual_` (34 шт.) + FindClass-пути

### Проверка: `rg -i combatant src` → 0 ✓, `./gradlew build` ✓ (1406 файлов изменено)

## Фаза 2: Выкинуть Runtime/JarReplacement (пункт 4)
> **Вместе с рантаймом удалить и модуль Panic** (`features/module/modules/misc/Panic.java`),
> интерфейс `RuntimeControlModule`, команду `RuntimeCommand` и все ветки на них
> в `ModuleManager`/`Module`. Panic — единственная реализация `RuntimeControlModule`,
> без него они становятся мёртвым кодом.
- [x] Удалить `runtime/` и `runtime/loader/` (14 файлов)
- [x] Удалить `Panic.java` + `RuntimeControlModule.java` + `RuntimeCommand.java`
- [x] Убрать ветки `instanceof RuntimeControlModule` и `tickRuntimeControllers()` из ModuleManager/Module
- [x] Убрать `RuntimeGate` из точек входа (LoVisual.java, модули, рендер) — заменён на `Lifecycle`
- [x] Заменить на простой Lifecycle-класс (`dev.loki.lovisual.Lifecycle`: INIT → ACTIVE → SHUTDOWN, гейты `canRunRender/Hud/ShaderBridge` + `RenderResourceReadiness`)
- [x] Panic-фичи удалены; `ModuleManager.disableAll()` оставлен как простая утилита
- [x] Мёртвые cleanup-методы удалены (`shutdownForRuntime`, `clearRuntimeReferences`, `VanillaChatRuntimeCleanup`, i18n-ключи panic/runtime)

### Проверка: `./gradlew build` ✓, тесты ✓ (13), `rg runtime-подсистема` → 0

## Фаза 3: Разбивка файлов-гигантов (пункт 3) — ЗАВЕРШЕНА
> Стратегия: публичный фасад исключён из лимита ≤200 (делегирует ~все публичные методы),
> вся логика вынесена в package-private final-хелперы в том же пакете (плоско),
> ≤200 строк каждый. 0 вызывающих файлов изменено. AttributeSwap/Scaffold/ElytraTarget/
> AutoCrystal удалены в Фазе 3.5 — их разбивать не нужно.
- [x] Renderer2D (3379) → фасад 2150 + 14 хелперов (Blur, Chamfered, Circles, Connectors,
      Items, Lines, LiquidGlass, Orbiz, Polygons, Rects, RectsExtra, RectsGlow, SmokeFill, Textures)
- [x] BetterChatRenderer (2870) → фасад 210 + 21 хелпер (LayoutEngine, Viewport, FrameRenderer,
      MessageRenderer, GlyphRenderer, InputRenderer, Suggestion*, InputHandler, MouseClick,
      ContextMenu, Tooltip, Draw/Font/EditBox/Segment/CommandMerge + 3 model-файла)
- [x] UnifiedSettingRenderer (2798) → фасад 208 + 24 хелпера (по типам настроек + цвет-редактор
      из 9 классов: Renderer, SquareRenderer, PresetRenderer/Handler, HexInput/State, Drag, Layout, Math)
- [x] ClickGuiRenderer (1887) → фасад 542 + 15 хелперов (Input, Keyboard, RenderEngine, Lifecycle,
      Screen, TextBatch, DrawHelpers, KeyBind, Editor, PickerIcon, TopLayer, TabBar, TabState + Model + Fonts)
- [x] NameTags (1791) → фасад 438 + 11 хелперов (Renderer, Plate, LayoutEngine, Face, Equipment,
      Opponent, Effects, LabelBuilder, EnchantFormatter, Cache + Model)
- [x] TargetHud (1655) → фасад 340 + 11 хелперов (Renderer, TargetResolver, Panel, Face, Content,
      Text, Arc, Equipment, Effects, Formatter + Model)
- [x] ESP (1538) → фасад 269 + 11 хелперов (EntityCollector, ShaderRenderer/Mask/Color, 2D/3D,
      SearchOverlay, Outline, MeshBuilder, ColorUtil + Model)
- [x] CustomBar (1495) → фасад 426 + 11 хелперов (XpBar, JumpBar, Locator*×7, Resolver + Model)

### Проверка: после каждого файла `./gradlew build` ✓; хелперы ≤200, фасады исключены;
### 0 вызывающих файлов изменено во всей Фазе 3

### Долг (файлы >200 вне списка гигантов, в т.ч. новые фасады):
### Renderer3D 433, Module 1181, LoVisualRenderPipelines 1329, DraggableHudElementRegistry 1449,
### Predictions 1363, DynamicIsland 1255, CustomHealthBar 412, CustomHotbar 547, HudNotifier 1116,
### Triangulator 1102, BetterButtons 1033, Themes 1012, SvgMeshBackend 1014, UiPrimitiveRenderer 1042,
### BetterTooltips 948, CustomTabList 829, InventorySwap 1009, ModuleManager 521, LoVisual 441,
### а также многие HUD/module/rendering-файлы 200–800 строк. Разбирать по желанию.

## Фаза 3.5: Удаление читерских модулей (легит-направление)
> Пользователь решил: LoVisual — легитное приложение. Все читерские модули удаляются.
- [x] combat: удалены AttributeSwap, AutoAnchor, AutoAttack, AutoBed, AutoBow, AutoCrystal, Backtrack,
      Criticals, ElytraTarget, Hitbox, KillAura, KTLeave, MaceKill. **Оставлены:** ProjectilePuncher,
      PvpCooldowns, Reach, RubberHand, SpearAssist, TPSSync
- [x] movement: удалены ВСЕ модули (AirJump, AutoDodge, ElytraFly, ElytraHelper, Flight, Freeze,
      InventoryMove, NoFall, NoPush, NoStun, Parkour, Phase, ReverseStep, SafeWalk, Speed, Sprint,
      Step, Strafe, SuperFirework, TargetStrafe, Timer, Velocity, WindJump) — 22 файла
- [x] player: удалены AirPlace, AutoArmor, AutoEat, AutoTotem, Scaffold, XCarry.
      **Оставлены:** AutoTool, ClickPearl, FakeLag, NoDelay, NoInteract, ShitDropper
- [x] Осиротевшие хелпер-пакеты: удалены `combat/autocrystal/` (17), `combat/autobed/` (9),
      `combat/autoanchor/` (9), `util/block/scaffold/` (10), `BlockPlacer` + тест ScaffoldPlacementGateTest
- [x] Enum-заглушки: `KillAura.RaycastMode/RotationTiming/AngleSmoothMode` → локальные enum в
      ProjectilePuncher/Reach; `Criticals.SelectionMode` → удалено; `Hitbox`-зависимость убрана из ESP
- [x] ESP переписан под легит: новые настройки `only_visible` (LOS-луч от камеры), `skip_invisible`,
      `only_armor` (без брони не подсвечивать) — по умолчанию `true`, только для игроков

### Проверка: `./gradlew build` ✓, тесты ✓ (13), остатки только в комментариях/строках

## Фаза 4: Security (пункт 7)
- [x] BackdoorProtection: убрать `enum`-синглтон, сделать инстансный сервис, ленивый `Minecraft.getInstance()`
- [x] Решить судьбу StaffTracker/StaffHeuristics — согласовать и выкинуть или переписать
      (решение: StaffHeuristics удалён, StaffTracker+StaffData оставлены, переведены на INSTANCE-паттерн)
- [x] Покрыть логику фильтров тестами (BackdoorProtectionTest, 11 тестов)

## Фаза 5: Модули — нормализация под правила
- [x] Прочесать все 131 модуль: убрать `static final Minecraft` в полях (6 шт. уже найдено)
      (LegacyHudNotifier, ScissorFunction, ShitDropper, ProtocolUtil, CooldownRender, ClickGuiRenderer.MC
      + 7 файлов-потребителей ClickGui-пакета — всё переведено на ленивый `Minecraft.getInstance()`)
- [x] Проверить модули на `System.currentTimeMillis()` → заменить на игровое время
      (16 файлов модулей → новый `util/time/GameClock.millis()`, откат на стенное время вне мира)
- [ ] Разнести addon/events/mixininterface по смыслу — ОТЛОЖЕНО: blast-radius 54/41/88 файлов-импортёров,
      чистый import-churn без функциональной ценности; делать только отдельным скриптовым проходом

## Фаза 6: Тесты (пункт 6)
- [ ] Юнит-тесты: EventBus, конфиг, утилиты (игровые классы — mock)
- [ ] По мере переписи модулей — тест на каждый

## Фаза 7: QoL-модули из Delta (легит) — ПЛАН, обсуждаем
> Референс: `ref/delta-26.2-main` (чит-клиент, код обфусцирован — берём только идеи,
> переписываем под наши правила; у Delta 114 модулей, у нас 63).
> Уже перенесено: SeeInvisibles→InvisESP, HandsShader→Chams/Noise, AtmoDawnFog,
> ShulkerPreview, Animations, ItemPhysic (из WorldTweaks), HoldMyItems (фикс ordinal-бага,
> математика в EaseCurves). Реф-исходники: ref/hmi-src, ref/soupvisuals-src.

### Решения по обсуждению (2026-09-05)
- [x] Wasted — НЕ делаем
- [x] TapeMouse — делаем (пользователь: AFK-фермы, не чит-софт)
- [x] DeathCoords — делаем
- [x] ItemScroller / Arrows — НЕ делаем
- [x] ColorPlus ≈ наш PostFX — PostFX восстановлен и починен (дефолтный пресет был near-identity → добавлен Cinematic + тесты)
- [x] Правило папок: максимум 4 файла (было 3)

### Перенос (список пользователя)
- [ ] **StreamerMode** (misc, ~80 у них) — при записи/стриме скрывает: ники игроков,
      имена друзей, названия серверов («Анархия-N») в чате/таб-листе/скорборде; скрытие скинов.
      Интеграция: наши BetterChat + CustomTabList + Scoreboard-виджет + рендер скинов.
- [x] **LockSlot** (player, ~36) — запрет выброса из выбранных слотов 1–9 (анти-случайный Q),
      опция «только в PVP», предупреждение в чат. Точка: перехват drop-пакета.
- [x] **AutoRespawn** (player, ~28) — авто-возрождение после смерти (tick на death screen).
- [ ] **AutoLeave** (player, ~61) — выход в хаб по триггерам: малое ХП, игроки рядом (N, радиус), задержка.
- [ ] **AutoAccept** (player, ~67) — авто-принятие запросов: клан/ТП; опция «только от друзей»
      через наш PlayerRelations. Точка: перехват chat-пакетов.
- [ ] **SoundReducer** (player, ~42) — приглушение выбранных звуков/категорий (громкость 0..N),
      в отличие от нашего NoSound (он только вкл/выкл всего). Вопрос: новый модуль или развитие NoSound.
- [ ] **Wasted** (misc, ~100) — ОТМЕНЕНО (решение пользователя)
- [ ] **ThirdPerson** (player, ~77) — свободный обзор от 3-го лица без поворота движения
      (по нажатию/зажатию бинда). Не дублирует Freecam (он про отдельную камеру-сущность).
      Точка: наш CameraMixin.
- [ ] **UseTracker** (player, ~86) — уведомления об использованиях: тотем, зелья, предметы.
      Вывод через наш HudNotifier. Точка: пакеты + инвентарь.
- [x] **NoServerPack** (misc) — у Delta ПУСТЫШКА (пустой класс). Сделаем по-настоящему:
      авто-отклонение принудительного серверного ресурспака (спрашивать/отклонять всегда).
      Точка: наш ClientCommonPacketListenerImplMixin.
- [x] **NoFriendDamage** (combat, ~19) — отмена атаки, если цель — друг (PlayerRelations).
      Точка: перехват атаки (AttackEvent у них; у нас миксин на attack/interact-пакет).
- [x] **TapeMouse** (combat, ~66) — автоклик по таймеру. ⚠️ ПРОТИВОРЕЧИТ легит-политике Фазы 3.5
      (тот же класс, что удалённые AutoAttack/TriggerBot). По умолчанию НЕ делаем / делаем
      легит-вариант «напоминание о клике» без автоматического клика — РЕШИТЬ.

### Кандидаты из отчёта (не в списке пользователя — обсудить)
- [x] **DeathCoords** (~27) — координаты последней смерти в чат. Дёшево, полезно.
- [ ] **ItemScroller** — перекладывание предметов прокруткой в инвентарях. Крупный QoL (~200+).
- [ ] **ColorPlus** — ⚠️ это по сути наш удалённый PostFX (экспозиция/гамма/контраст/насыщенность/
      виньетка/резкость/LUT). Восстанавливать под новым именем или забыть — РЕШИТЬ.
- [ ] **Arrows / off-screen индикаторы** — дополнение к Pointers (у нас их нет вообще).

### Проверка после каждого модуля: `./gradlew build` ✓, тесты на чистую логику ✓

## Фаза 8: Структура «как у Delta» (маленькие файлы) — ПЛАН
> Правило ≤200 уже есть (Фаза 1–3), но долг: ~240 файлов >200 строк.
> У Delta файлы компактные и слои разделены: api / client(features, ui, utils, processors) /
> inject(accessors, invokers, mixin). Наша пакетная структура сопоставима — проблема в размере файлов.

- [ ] Топ-гигантов к разбивке (после Фазы 3): LoVisualRenderPipelines 1527,
      DraggableHudElementRegistry 1449, Predictions 1363, DynamicIsland 1254, Module 1181,
      HudNotifier 1116, AltManager 1115, Triangulator 1102, TargetESP 1061, UiPrimitiveRenderer 1042
- [ ] Стратегия: как в Фазе 3 — фасад + package-private хелперы ≤200, 0 изменений вызывающих,
      `./gradlew build` после каждого; по мере касания файлов в Фазе 7 — разбивать на ходу
- [ ] Facades >200 (Renderer2D 2150 и т.п.) — оставить как есть (исключение по правилам)
- [ ] Не смешивать с Фазой 7: разбивка структуры — отдельными проходами, иначе diff-каша

### Проверка: `./gradlew test` зелёный
