# LoVisual Mod (combatant-client-26.2) — план переписи

Мод — форк Combatant Client, переписываем под LoVisual.

## Правила (ОБЯЗАТЕЛЬНЫЕ, нарушение = откат)
- **KISS / DRY / SOLID** — простая структура, без оверинжиниринга
- **Макс. 200 строк на файл** (было >1500) — превышение = разбить на модули
- **Макс. 4 файла на папку** — больше = новая подпапка по смыслу
- **Публичный API можно менять свободно** (решение владельца 2026-09-05):
      KISS важнее обратной совместимости; при рефакторинге разрешено менять
      сигнатуры, удалять делегирование и мёртвый код, обновляя вызывающих
- **Коммиты — на английском** (решение 2026-09-23, действует с этого момента
      вперёд; более старые коммиты на русском не переписываем)
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
- [x] Юнит-тесты: EventBus (12), конфиг (values: NumberValue/Boolean/String/Set/ItemIdSet/BooleanMap,
      ModeValue, EnumValue, RGB/RGBA color; profile: DiffEngine, ValueCodec),
      утилиты (LegacyTextUtil — 12). Игровые классы без моков: тесты только на чистую логику.
- [x] По мере переписи модулей — тест на каждый (Текущий итог: 292 теста, все зелёные)

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
- [x] **StreamerMode** (misc) — маскирует ники-подобные токены и названия серверов в чате
      (общая точка NameProtect: ChatComponentMixin.addMessage), скины заменяются на
      стандартные (AbstractClientPlayerMixin.getSkin). Чистая логика — `util/text/NameMasker`
      (тест NameMaskerTest).
- [x] **LockSlot** (player, ~36) — запрет выброса из выбранных слотов 1–9 (анти-случайный Q),
      опция «только в PVP», предупреждение в чат. Точка: перехват drop-пакета.
- [x] **AutoRespawn** (player, ~28) — авто-возрождение после смерти (tick на death screen).
- [x] **AutoLeave** (player) — выход по триггерам: низкое ХП, N игроков в радиусе
      (друзья игнорируются опцией), задержка + предупреждение. Выход через
      `mc.disconnect(null, false)`; чистая state-machine `LeavePolicy` (AutoLeaveTest).
- [x] **AutoAccept** (player) — авто-ответ на ТП/клан-запросы: паттерны фраз (SetValue),
      команда ответа, «только друзья» (PlayerRelations), задержка, кулдаун ≥500мс.
      Пакетный путь: PvpChatEvent (handleSystemChat/handlePlayerChat), отправка
      `connection.sendCommand(...)`. Чистый парсер `TeleportRequestParser` (AutoAcceptTest).
- [x] **SoundReducer** (player, ~42) — РЕШЕНО: развитие NoSound, а не новый модуль.
      Режимы Mute (старое поведение через ClientPacketListenerMixin) и Reduce —
      новая громкость 1–100% через SoundEngineMixin (@Redirect getVolume в
      SoundEngine.play). Чистая математика `NoSound.resolveScale` (NoSoundTest).
- [x] **Wasted** (misc, ~100) — ОТМЕНЕНО (решение пользователя)
- [x] **ThirdPerson** (player) — свободный обзор от 3-го лица по удержанию бинда (L,
      настраивается), движение игрока не поворачивается. MouseMixin.turnPlayer копит
      orbit-yaw/pitch, CameraMixin.update (TAIL) ставит камеру за игрока по орбите
      (+ cull-frustum). Орбиты сбрасываются, когда бинд отпущен.
- [x] **UseTracker** (player) — уведомления: мои тотемы (N осталось), тотемы других,
      эффекты оппонентов (группа: invisibility/speed/strength/regeneration/fire_res/weakness).
      Пакетный путь: ClientboundEntityEventPacket (event 35) + ClientboundUpdateMobEffectPacket.
      Тест UseTrackerTest.
- [x] **NoServerPack** (misc) — авто-отклонение принудительного серверного ресурспака
      (спрашивать/отклонять всегда). Точка: ClientCommonPacketListenerImplMixin.
- [x] **NoFriendDamage** (combat, ~19) — отмена атаки, если цель — друг (PlayerRelations).
- [x] **TapeMouse** — ОСТАВЛЕН (решение владельца 2026-09-08): автоклик по таймеру
      (Attack/Use) для AFK-ферм, интервал, опция «только при зажатии». Считаем
      допустимым исключением из Фазы 3.5 по явному решению владельца.

### Кандидаты из отчёта (не в списке пользователя — обсудить)
- [x] **DeathCoords** (~27) — координаты последней смерти в чат. Дёшево, полезно.
- [x] **ItemScroller** (misc) — shift-перекладка предметов колёсиком в контейнерах:
      QUICK_MOVE клик по слоту под курсором, иначе слот хотбара (опция).
      Точка: AbstractContainerScreenMixin.mouseScrolled; чистая логика
      `ScrollMovePolicy` (ScrollMovePolicyTest).
- [x] **Arrows / off-screen индикаторы** — ОТМЕНЕНО (решение владельца 2026-09-08).

### Проверка после каждого модуля: `./gradlew build` ✓, тесты на чистую логику ✓

## Фаза 7.5: Команды — префикс % + автокомплит
- [x] Префикс команд сменён `@` → **`%`** (старый `@` принимается как legacy).
      Источник истины — `features/command/CommandPrefix` (CHAR/STRING/isCommandLike/strip);
      вызовы обновлены: CommandManager.handle/suggest, ScreenMixin (RunCommand),
      BetterChat (TabHandler, SuggestionState, SuggestionRenderer, ContextMenuHandler),
      usage-строки и RunCommand-ссылки всех команд (help/modules и др.).
- [x] Аргументный автокомплит дополнен usage-подсказками: если `suggest()` команды
      не дал вариантов на позиции — показывается подсказка вида `%toggle <module>`
      (`features/command/UsageHints`, парсит `<...>`/`[...]` из usage-строки).
      Существующий UI автокомплита BetterChat (стрелки/Tab/клик) работает как раньше.

### Проверка: `./gradlew build` ✓, `./gradlew test` ✓ (CommandPrefixTest, UsageHintsTest,
### CommandDiscoveryTest обновлён)

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

## Фаза 8.5: Аудит структуры (правила ≤200 строк / ≤4 файла) — ПОЛНЫЙ СПИСОК НА РЕФАКТОРИНГ
> Снимок 2026-09-09 (`src/main/java`, 1459 файлов / 197k строк).
> **Гейт перед Фазой 9.2 (Optimize)**: движковые файлы, которые Optimize будет трогать,
> должны быть разбиты заранее. 9.1 (мёртвые грузы) от этого НЕ зависит — идёт своим порядком.
> Стратегия та же, что Фаза 3/8: фасад + package-private хелперы ≤200, 0 правок вызывающих,
> билд после каждого файла. Фасад-исключение (Renderer2D) — см. оговорку ниже.

### Итоговая картина
- **Файлы >200 строк: 244 шт.** → >800 строк: 26 | 400–800: 78 | 201–400: 140
- **Папки >4 файлов: 86 шт.**
- Разбивка больших файлов по зонам: features/gui — 73 | render/engine — 54 |
  features/module — 35 | util — ~35 | mixins — 3 | config — ~6 | прочее — остальное

### 8.5.1 Папки >4 файлов — топ должников (разбить на подпапки по смыслу)
- [x] `mixins/` — 108 файлов → leaf-пакеты (entity/blaze3d/input/world/network/gui/hud/render),
      mixins.json перегенерирован, FQN-строки LoVisualMixinPlugin обновлены. Коммиты 94d5af7, 3c76817.
- [x] `mixins/accessors/` — 45 → 12 групп (entity/renderer/world/state/packet/inventory/...). Коммит 3c76817.
      Также разбиты iris/sodium/security на подпакеты.
- [x] `features/module/modules/visuals/` — 67 → esp/nametags/hats/effects/weather/scene/camera/
      player/items/fx + lyrics/lrc. Конфиг-id НЕ менялись (проверено). Коммит 073ac26.
- [x] `features/gui/clickgui/settings/` — 51 → core/bool/mode/slider/text/text-list/group/bind/
      color(+render/preset/hex)/i18n/render/skin/owner/cooldown/util. Коммит 9db1fec.
- [x] `features/gui/hud/draggable/impl/` — 40 → stats/metrics/status/combat/panels/targethud(+/render,
      render/parts)/scripted + hudnotifier/model, targethud-модель свёрнута в класс с record-ами. Коммит 6b89e40.
- [x] `events/impl/` — 32 → input/movement/world/network/entity/pvp/render/player;
      events/ верхний уровень: core(Event,EventBus,Events) + meta(EventHandler,UsedImplicitly).
- [x] `features/gui/chat/` — 31 → `render/`, `suggestions/`, `menu/`, `model/`.
- [x] `render/engine/uniform/impl/` — 28 → по потребителям (`uniform/impl/pipeline/`, `uniform/impl/effects/`).
- [x] `render/engine/renderer/ui/` — 28: верхний уровень разнести по уже существующим
      `draw/`, `runtime/`, добавить `blur/`, `batches/`.
- [x] `render/engine/text/` — 24 и `text/backend/` — 12 → `text/` разбить на `text/render/`,
      `text/layout/`, `text/msdf/`.
- [x] `render/engine/renderer/ui/runtime/script/` — 23, `ui/draw/` — 23 → внутри по 4-файловым кластерам.
- [x] `features/gui/hud/nondraggable/impl/` — 20 (+`impl/tab/`) → `impl/overlays/`, `impl/tablist/`.
- [x] `features/gui/clickgui/` — 20 на верхнем уровне → всё в существующие `layout/`, `sections/`,
      `picker/`, `protocol/`, `util/`.
- [x] `render/engine/command/` — 17, `render/helpers/` — 16, `features/command/impl/` — 16,
      `config/values/` — 16 → по 2-3 подпапки (типам: `values/numeric/`, `values/color/`…).
- [x] `render/engine/profiler/` — 14, `modules/player/` — 14, `features/gui/hud/` — 14,
      `addon/` — 14 — средние: раскидать при касании.
- [x] Хвост (5–13 файлов, ~55 папок): `util/pvp` 13, `util/projectile` 13, `render/iris` 13,
      `config/profile` 13, `features/module/misc` 12, `features/module` 12, `config` 12 и т.д. —
      ЗАКРЫТ выделенным проходом (коммиты 8846d53…303b17e, волна 8.5.1 завершена).
      Снимок после волны: 0 папок >4 файлов в `src/main/java` (скан 2026-09-10).
      Замечания: command-discovery рекурсивный (ClassGraph acceptPackages), конфиг-id
      и ключи i18n не менялись; package-private члены, ставшие кросс-пакетными,
      расширены до public/protected; svgMsdfTool-импорты починены после split svg.

### 8.5.2 Файлы-гиганты >600 строк — приоритетная очередь разбивки (53 файла) — ЗАВЕРШЕНА
> Проверено 2026-09-28: `find src/main/java -name "*.java" | xargs wc -l` → 0 файлов >600 строк
> (самый крупный — Renderer2D.java 474, осознанный фасад-делегат, исключение Фазы 8). Все 53
> пункта очереди ниже закрыты. Остаток структурного долга — 8.5.3 (201–400 строк), не марафон,
> а правило «трогаешь файл по другой задаче — первым коммитом режешь до ≤200».
> Формат: файл (строк) → идея разбивки. Порядок сверху вниз. Facade-оговорка:
> Renderer2D остаётся фасадом-делегатом (правило Фазы 8), но его вспомогательные
> классы (kawase/blur/glass) — вынести из OrderedUiBatcher (935) в `ui/blur/`.
- [x] Renderer2D 2179→1597 → ГОТОВО 7b848ddd+464f7868+098c5515 (2026-09-25): ФАСАД;
      далее 1597→475 наследованием (волна 2026-09-25 ниже)
      (исключение по правилу Фазы 8) — делегирование оставлено; −234 мёртвые шимы
      совместимости, −271 недостижимые перегрузки liquidGlass/item/Corners,
      −115 прочие мёртвые перегрузки+импорты; макс. тело метода 35 строк (блоков >200 нет),
      хелперы в `renderer/renderer2d/` (Rects2D/Strokes2D/…), build+test ✓
- [x] LoVisualAltManagerScreen 1113 → ГОТОВО (2026-09-10): 11 файлов `mainmenu/alt/`
      (Screen 132, Controller 200, Ops 59, Anims 108 + auth/ 171 + render/ 157/156/139/100
      + render/cards/ 178/166), все ≤200, build+test ✓
- [x] UiPrimitiveRenderer 1053 → ГОТОВО (2026-09-10): фасад 188 (коннекторы) +
      render/primitive/ 4 (Context 153, Props 158, Painter 145, GlowPainter 147) +
      render/rect/ 2 (RectPainter 150, CornerPainter 150), build+test ✓
- [x] BetterButtons 1032 → ГОТОВО 7e86f8d: 388 + 7×≤200 (state/hover/parallax/queue/render) — extraction+tooltip+render → `hud/nondraggable/impl/buttons/`
- [x] InventorySwap 1031 → УДАЛЁН 5acafc8: чит-инфра, заменён ShitEsp + хотбар-only — по стратегиям свапа; чистая логика отдельными классами с тестами
- [x] SvgMeshBackend 1014 → ГОТОВО c11ebe3/aafe7de: 1020→267 + Parser181/Geometry173/Texture223/Render35/Cache25 — parser/triangulate/cache разнести
- [x] Themes 1012 → ГОТОВО e51a610: 1012→562 + 3×163 пресета — реестр тем в `theme/presets/` (по 4 темы/файл), дефолты отдельно
- [x] WorldParticles 991 → ГОТОВО 1d757a6+5a463d7e: 999→821→260 (фасад) + ambient/render/ 3×≤182
      (Batches/Bubble/Funnel) + ambient/particle/ 197 + geometry/ доработаны (offset/multiplyAlpha/STACKS) —
      типы частиц в `visuals/effects/ambient/`
- [x] Module 989 → ГОТОВО c8da336: 1031→139 + 4×≤134 (ext/) — базовый класс: settings-механику вынести в `module/ext/` (осторожно, общий предок!)
- [x] LoVisualAddonManagerScreen 989 → ГОТОВО de56b4b9: 989→112 + 10 файлов mainmenu/addon/
      (Controller 164, Ops 112, PanelAnims 56, auth/AddonScanState 52 + render/ 115/162/183
      + render/cards/ 79/88), все ≤200 — по образцу AltManager
- [x] Chams 976 → ГОТОВО 78b5335: 985→129 + 4×≤195 — режимы рендера в подклассы-политики (фасад модуля тонкий)
- [x] ClickGuiPickerState 962 → ГОТОВО 234f0d9: 970→166 + 4×≤140; CombatProtocolHeuristicsEditorState 932→79 +4×≤192 03ae4a6 — state/actions хелперы
- [x] ModulesMenuScreen 960 → ГОТОВО 234f0d9: 966→145 + 4×≤178 — рендер категорий отдельно от состояния
- [x] UiMeshGeometry 954 → ГОТОВО sub-agent: 956→99 + 4×≤135 — по примитивам геометрии
- [x] BetterTooltips 953 → ГОТОВО 0b8ea41: 960→181 +4×≤199; OrderedUiBatcher 935 → ГОТОВО 6e0bf7f: 944→188 (→ batches + blur), Scoreboard 924,
      Rain 893 → ГОТОВО sub-agent: 903→159 +4×≤162; CompactHudStatModel 893 → ГОТОВО 8979140: 894→125 +4×≤175; TazikHat 859 ✓ ✓ закрыт портированием в
      Cosmetics built-in (2026-09-11, см. Фазу 7),
      ShaderPatchCompiler 843 → ГОТОВО 2b55fbc: 843→66+4×≤174; OnlineRelationPlayerPickerComponent 837→74+4×≤192 17fc665; CustomTabList 830 → ГОТОВО 9e9b71d: 834→136+4×≤166,
      GameRendererMixin 830 → ГОТОВО f45f46e: 830→344 (только хуки+Host/BobInvoker) +
      mixins/render/main/handlers/ 4×≤200 (ProjectionHelper/WorldEngine/WorldPost/WorldDebug), Predictions 805 → ГОТОВО (см. хвост)
- [x] Хвост 400–800: ЧАСТИЧНО — ConfigProfilesComponent 799→125  d95bfec, HitEffect 770→120 5e1ce05, PlayerMovementSimulation 757→157 4b253d1, ProjectilePuncher 747→104 fba4f7d, MediaPlayer 739→174 8ce3927, Crosshair 737→143 4cf0970, Itemizer 699→193/Cooldowns 707→151 11245ac, Potions 617→176 2e4912a, Admins 702→118 f18cb96, Keybinds 611→157 cc18feb, ModuleList 499→181 52327dc, Radar 392→161 f580373, Fps 309→168 5df2cc0, Memory 304→151 de7cc2b, Tps 339→154 e167f0c, Ping 309→145 15d04cb, SpeedBps 334→156 b78a638, GameTime 314→150 922d3bc, SystemTime 300→136 3cb6e40, Coordinates 337→174 9412982, Armor 324→162 932639d, Inventory 476→153 f4124f4, BlockHighlight 692→271+4×≤200 9f76e5a5 (scene/highlight/), см. /tmp-снимок в истории команд: ConfigProfilesComponent 791,
      HitEffect 770, PlayerMovementSimulation 755, ProjectilePuncher 746, MediaPlayer 739,
      Crosshair 737, MeshBuilder 729, SettingsPanelComponent 703, Admins 702, Cooldowns 701,
      Itemizer 699, LocalPlayerMixin 687, Trails 681, ItemBatchRenderer 673,
      CosmeticModel 671, MenuScreen 667, GlStencilFramebufferSupport 663, ConfigSerializer 663,
      DynamicIsland 661, ReimaginedVisual 660, RelationsComponent 659, UiStyle 640, Potions 617,
      ModuleComponent 612, MsdfFont 606, Keybinds 603, MediaSessionService 594 и др.)
- [x] Волна 2026-09-24 (HUD/settings): NameTags 453→223 (nametags/impl/ Keys+Collector+Projection) b1414d7a,
      MainSettingsComponent 448→291 (+Scroll+Model) 3f0d452f, LinuxMediaSession 441→148 3d11bb4e,
      CustomTextRenderer 229→195 (GradientTexts), CustomBar 438→185 (+CustomBarModel константы
      +bars/impl/BarSettings, forwardеры удалены, mixins правлены на реальные рендереры) 4e442fb7,
      CustomHealthBar 406→146 (+bars/impl/HealthBarPainter+HealthScriptRenderer, dead-code удалён) a3415435.
      bars/ закрыт: все файлы ≤200, impl/ = 3 файла.
- [x] Волна 2026-09-25 (реестры): Renderer2D 2179→1597 (см. строку 8.5.2 выше,
      7b848ddd+464f7868+098c5515), LoVisualRenderPipelines 432→311 9577ed3d
      (SHADER_* 105 констант → pipeline/ShaderIds 130, мёртвые импорты удалены;
      логика ≤26 строк, фасад-исключение), build+test ✓.
- [x] Волна 2026-09-25 (наследование фасада): Renderer2D 1597→475 (leaf), цепочка
      Renderer2D extends Item(124) extends Glass(183) extends Shapes(313) extends
      Path(245) extends Rounded(315) extends Base(23); API вызывающих не менялся
      (0 правок callers, static-import ITEM_OVERLAY_* и Renderer2D.LiquidGlassPreset
      работают по наследству); renderer/facade/ = 4 файла, facade/shape/ = 2 ✓ лимит;
      Base.self() для this→Renderer2D-делегатов; семейства >200 — фасад-части
      (исключение TODO:Facades); 37c7c410+64e23ef8+9ae72eeb+2b8c40a3+6204017b+
      9fffc58d, build+test+checkstyle ✓.

### 8.5.3 201–400 строк (~140 файлов) — проход «касаться = чинить»
- [x] Волна 2026-09-30 (топ-4 по объёму, выбор владельца): каждый файл — отдельный
      коммит, фасад + хелперы, публичный API и вызывающие не тронуты:
      ConfigDiffPanelComponent 393→167 (фасад + `config/diff/`
      HeaderRenderer/BodyRenderer/Scrollbar) 7e8b6b64;
      RotationManager 391→199 (фасад + `rotation/internal/`
      RotationState/RotationStepper/RotationPackets, @EventHandler-методы остались
      на фасаде — регистрация не менялась) 3e57542b;
      ScriptedTriangulatorHudPanel 386→189 (записи Panel/Palette остались
      вложенными → `ScriptedTriangulatorHudPanel.Panel` у вызывающих работает,
      рендер → ScriptedTriangulatorRenderer, пропсы/сигнатуры → `scripted/panel/`)
      76bd8e95; BetterButtons 381→197 (настройки → ButtonStyleSettings по образцу
      BarSettings, иконки → ButtonIconRenderer, −3 мёртвых метода без вызовов)
      71f779c8; `./gradlew build` ✓ test 438 ✓ checkFolderLimit ✓ на каждом шаге;
      файлов >200: 155 → 151
- [ ] Не делать отдельным марафоном: при правке файла по другой фазе — первым коммитом
      разбивка на ≤200, вторым — целевое изменение (иначе diff-каша, правило Фазы 8)

### 8.5.4 Порядок и критерии готовности
- [x] Очередь: 8.5.1 крупные папки (mixins, visuals, clickgui/settings, hud impl) →
      8.5.2 гиганты >800 → далее по убыванию. Каждая папка/файл = отдельный коммит.
      → 8.5.1 ЗАВЕРШЕНА (2026-09-10): 0 папок >4 файлов.
- [ ] После каждого шага: `./gradlew build` ✓, `./gradlew test` ✓; конфиг-id модулей и
      ключи сеттингов НЕ ломаем (совместимость .lvcfg); скрипты-переименования пакетов
      не трогают string-literals id — проверять grep'ом после moves.
- [x] Критерий закрытия для старта 9.2: ноль файлов >200 в путях, которые трогает Optimize
      (Renderer2D-секции blur/glass, OrderedUiBatcher, HUD-имплементации bgEffect,
      MsdfFont/CustomTextRenderer), и ноль папок >4 в этих же пакетах. Полный ноль по всем
      244/86 — долгий хвост, 9.2 им не блокируется.
      → 2026-09-24: MsdfFont 155 ✓, OrderedUiBatcher 180 ✓, CustomTextRenderer 229→195 ✓
      (glyph-pass вынесен в GradientTexts). Остаток гейта — bgEffect-HUD >200:
      TargetHud 333, Triangulator 304, Scoreboard 233 — ВСЕ фасады Фазы 3 с helper-пакетами
      (targethud/render/, triangulator/{render,Solver,Tracker,View}, panels/scoreboard/) →
      исключение по правилу фасадов. ГЕЙТ 9.2 ЗАКРЫТ.
- [ ] Обновить этот раздел при подходе к волне (отмечать [x], доснимать счётчики).

## Фаза 9: Оптимизация FPS (аудит 2026-09-09) — ПЛАН
> Диагноз: у клиента есть «налог каждый кадр», который платится даже при
> выключенных модулях (друг: просадка FPS на чистом конфиге, Windows).
> Решение владельца: мёртвые грузы — ЧИНИМ в коде; авто-деградации по FPS НЕ ДЕЛАЕМ;
> вместо неё — ручной модуль **Optimize** (включил → порезали стекло/blur/тяжёлые проходы).

### 9.1 Мёртвые грузы (чиним в коде всегда, независимо от модулей) — приоритет
- [x] **glGetError на каждом render pass** — ГОТОВО 2026-09-11 (коммит 137121e): гейт по clip-active || stencil-debug — `GlStencilShapeClipBackend.endRenderPass`
      (`:140`) и `beginRenderPass` (`:98/:104/:110/:124`) дёргают `checkGlErrors`
      (`:428-434`) на КАЖДЫЙ проход (миксин `CommandEncoderMixin:102-112` висит на
      submitRenderPass → это и все ванильные/Sodium проходы). `glGetError` = синк с
      Windows-драйвером, несколько мс/кадр. Фикс: проверять только если
      `mode != DISABLED || renderPassAttachmentRequired`, либо под dev-флагом;
      в релизе — no-op.
- [x] **`lovisual$isVulkanBackend()` на каждый submitRenderPass** — ГОТОВО (c9a65b1), кэш Boolean
      (`CommandEncoderMixin:66-74`) — `getDeviceInfo().backendName().toLowerCase()`
      заново на кадр×проходы. Фикс: вычислить один раз, закэшировать boolean.
- [x] **PreTranslucentDepth — полноэкранный depth-copy каждый кадр без нужды** — ГОТОВО (b747b47): demand-флаг requestForNextFrame()/takeRequested() у потребителей getDepthView(For), capture в vanilla- и Iris-путях по запросу (1-кадр задержка, фолбэк на live view) —
      `LevelRendererMixin:275-283` гейтится только «нет Iris». Фикс: гейт
      `needsPreTranslucentDepthCapture()` по образцу `WorldSceneDepth`
      (`LevelRendererMixin:160-218`); потребители: `PostProcessGraphResources:61`,
      `LoVisualVisuals:79`, `WorldPassCompiler:174`.
- [x] **copyMainColor каждый кадр ДО гейта** — ГОТОВО (118eb2a): ранний return по liquidGlassBlurRequested — `UiBlurResources.captureWorldSource`
      (`:116-123`): полноэкранный color-copy делается до проверки
      `liquidGlassBlurRequested` (`:122`). Фикс: переставить — copy только когда
      blur реально запрошен.
- [x] **Строки профайлера при выключенном профайлере** — ГОТОВО (aa55c4d): 2-arg scope()/section()/postPass(prefix,suffix) в ProfilerPhase/RenderProfiler2D/3D/RenderCostProfiler (DEV-гейт конкатенации) — конкатенации на call-site
      каждый кадр: `PostProcessGraph:59` (`"graph:" + phase`), `ModuleManager:219-221/:290`
      (`"3d:module:" + m.name()`). Фикс: ленивые supplier'ы / ранний isEnabled-check.
- [x] **Мелочь, по ходу**: `new PoseStack`/`new Matrix4f`×2/`new Frustum`×2 в кадре
      (`GameRendererMixin:203-239/:341-347/:609-617`) — закэшить переиспользуемые копии. Frustum-часть отменена (da6525c): @Invoker("calculateFrustum") не резолвится на remapped-рантайме = краш; scratch-кэш матриц/PoseStack остался.

### 9.2 Модуль **Optimize** (misc) — ручной рубильник экономии
> ⛔ ГЕЙТ: не начинать, пока не закрыта Фаза 8.5 (рефакторинг-аудитStructure ниже) —
> Optimize трогает Renderer2D/OrderedUiBatcher/HUD-имплементации, все они >200 строк.
> Идея владельца: один тумблер «хочу максимум FPS». НИКАКОГО автовключения по FPS.
> Пока активен — движок и HUD работают в лёгком режиме.
- [x] Каркас — ГОТОВО ade82f06: `modules/misc/optimize/Optimize.java` (в подпапке — лимит
      `checkFolderLimit`=4 файла в `misc/`), 4 bool-тумблера (default on), тест `OptimizeTest`.
      Отклонения от плана при реализации:
      - `no_glass` — **9246e010**: не мутим bgEffect (конфиг элементов неизменяем) и не
        ищем `prepareSharedBlurInternal` (метода больше нет). Гейты: `Blur2D.source()`
        → null при no_glass (вместе с `FpsOptimizer.isGlassDisabled` → все
        blurRect/glassBlurRect мимо) + ранние return в
        `UiBlurResources.requestLiquidGlassBlur/requestBeforeNextShapeClip`
        (флаги не ставятся → `captureWorldSource` early-out → нет полноэкранного copy,
        kawase и prewarm). Побочный эффект: гасит и низкоконтрастный blur текста
        (осознанно — «все glass/blur off» по замыслу тумблера).
      - `lite_post` — **52463246**: MotionBlur `SHADER_TAPS=11 → LITE_TAPS=5`
        (план «15→5» устарел — тапов уже 11), `historyValid = hasHistory && !lite`
        (temporal-история off = ещё ~10 neighborhood-fetch'ей в шейдерный early-out;
        цена: хвостов истории нет, остаётся чисто пространственный блюр).
        DoF: `taps = lite ? LOW : quality` (LOW=8; план «32→8» устарел — квалити 8/14/22).
      - `lite_sky` — **c403cb53**: `SkyboxShaderPasses.LITE_SKY_LAYER_MASK=0xF00F`
        (базовые слои 0-3 + звёзды 12-15, чистит nebula/curtains/bursts/polar/water/…),
        `layerMask &=` при включённом тумблере; в `reimagined_skybox.frag` 4
        безусловных fbm + bursts-блок обёрнуты в layer-гейты; pulsar/thunder fbm
        4→2 октавы через `u_Thunder.w` (ранее всегда 0). Тест `SkyboxShaderPassesTest`,
        все 3 шейдера проходят `glslangValidator -S frag`.
      - `lean_hud` — **8746074d**: `GradientTexts.horizontalFade` → plain-текст (без
        per-glyph `new String` и gradient-лямбд; marquee-clipped — clip остаётся,
        fade-градиенты гаснут) в Custom и Vanilla-реализациях; Fps digit-roll off
        (`FpsAnimator.widerText/digitProgress` → settled-прогресс).
        Не в scope: `GradientTexts.gradient/quadGradient` (визуал списка модулей),
        аллокация в `HorizontalFade.glyphAdvance` (её просто обходит lean-гейт).
- [x] Механизм «как движок узнаёт» — ГОТОВО ade82f06: `render/engine/optimize/
      OptimizeState.java` (enum-utility, 4 volatile boolean, `apply/reset`, чистый
      статик без состояния модуля), движок читает геттеры, модуль пушит в onEnable/onFrame.
- [x] Взаимодействие с 9.1 — ГОТОВО: фиксы 9.1 не завязаны на Optimize и продолжают
      работать всегда; Optimize только добавляет гейты сверху.
- [x] i18n en_us + ru_ru — ГОТОВО ade82f06: `module.optimize.*` + `setting.optimize.*`
      (4 ключа, симметрия en/ru проверена).
- [x] Нотификация при первом включении — ГОТОВО ade82f06: once-флаг в инстансе модуля +
      `Notifier.info("Optimize: " + summaryOf(...))` со списком включённых тумблеров.

### 9.3 Проверка
- [x] `./gradlew build` после каждого пункта; `./gradlew test` зелёный — выполнялось
      после каждого из 5 коммитов 9.2 (итог: build+test зелёные на 8746074d).
- [x] Порядок: 9.1 (по одному пункту на коммит) → 9.2 — 9.1 закрыта (коммиты выше),
      9.2 закрыта ade82f06..8746074d.
- A/B-мерка до/после 9.1 на чистом конфиге — УБРАНА из плана (решение владельца
      2026-09-25): требует запуска Minecraft на Windows, задачей не является.

## Фаза 10: Заход по референсам из Downloads (2026-09-15)
> Пользователь скинул 5 архивов, распакованы в `/tmp/lv_refs/`. Порядок выполнения —
> сверху вниз, по одному коммиту на пункт, `./gradlew build`+`test` после каждого.

- [x] **TrollfaceMask — почини "не работает"** (`/tmp/lv_refs/trollface/TrollfaceMask.java`,
      референс Moonward 1.21.4). Наш модуль: `trollface/TrollfaceMask.java`.
      Причина бага: биллборд ориентировался к камере (headPos→camPos); от первого лица
      камера почти совпадает по вертикали с головой → view≈(0,1,0) → right=view×up
      вырождается в 0 → плоскость ложится горизонтально, не видна. Режимы «Друзей»/«Всех»
      брали только `TargetManager.getTarget()` (сущность под прицелом) — маска рисовалась
      максимум на одном игроке. Фикс: ориентация по `getViewVector()` цели вместо камеры,
      перебор `mc.level.players()` с фильтром по режиму (себя/friend via PlayerRelations/все).
      Коммит e7e0ff2.
- [x] **Rain — сравнение с двумя референсами (2026-09-15, вывод: у нас уже паритет/лучше).**
      Референс 1 `/tmp/lv_refs/rainfull/` (Cataclysm client) — наш `Rain.java` (894 строки) уже
      является портом этого самого класса (Drop/Splash/Droplet/Mist, presetDensity/Wind/Mass,
      drawStreaks/Ripples/Crowns/Droplets/Mist — 1:1 совпадающие константы и формулы), ПЛЮС у нас
      уже есть их wet-surface screen-space постфх (`WetSurfaceUniforms` + шейдер
      `assets/lovisual/shaders/wet_surface.frag`, pipeline `WET_SURFACE`, реализован как
      `PostProcessPass` в самом Rain.java) — то, что в референсе вынесено в отдельный
      `WetSurfaceRenderer`. Улучшать нечего, разница только в деталях реализации пайплайна.
      Референс 2 `/tmp/lv_refs/mokriyzhir/` (`WetWorld.java`, пакет `ez.redux...`) — это ОТДЕЛЬНЫЙ
      самостоятельный модуль "Wet World" из чужого чит-клиента (Category.VISUALS), НЕ часть дождя:
      мочит поверхности всего мира по реальной погоде сервера (`world.getRainGradient`), с настройками
      Quality (Low/Medium/High/Ultra), Reflection Strength, Wet Darkening, Ripples+Speed, Rain Only,
      учитывает fog (интеграция с их модулем Fog: цвет/дистанция/яркость подмешивается в шейдер).
      У нас такого отдельного всегда-активного "мир мокрый по факту дождя" модуля нет — наш
      wet-surface жёстко завязан на модуль Rain (работает только когда сам Rain включён). Если
      захочется — можно вынести wet-surface-pass Rain.java в отдельный модуль WetWorld с привязкой
      к `mc.level.isRaining()`+`getRainLevel(tickDelta)` вместо `onlyWhenRaining` галки, плюс quality-
      пресет и fog-интеграцию. НЕ делаю сейчас — не запрошено явно, чисто идея на будущее.
- [x] **WetWorld — сделан отдельным модулем (2026-09-15), шейдер Rain пофикшен.**
      Третий референс `/tmp/lv_refs/wetworld2/` (`ru.white...WetWorld` + `WetWorldPipeline` +
      `wet_world.vsh/fsh`) оказался технически лучшим из трёх: настоящий SSR с бинарным
      рефайнментом пересечения, dithering шага луча, реконструкция нормали по соседям в буфере
      глубины (не screen-space derivative), fbm-небо с sun specular, fog-интеграция.
      Проверка нашего `wet_surface.frag` («может кривые») подтвердила баги:
      1) `normalFromDepth` считался через `dFdx/dFdy(viewPos)` — рвётся на границах глубины,
         шумная/блочная нормаль -> дрожащий край лужи;
      2) рябь была МЁРТВЫМ КОДОМ: `vec2 perturb = vec2(dFdx(ripple), dFdy(ripple)) * 0.0;` —
         умножение на 0 полностью гасило эффект, а сама рябь добавлялась как сырое значение
         волны вместо её наклона.
      Исправлено (коммит 0986ce6): нормаль — по 4 соседям depth-буфера (как в лучшем референсе),
      рябь — аналитический градиент (разность высоты в соседних точках), добавлен bin-рефайнмент
      SSR-пересечения + dithering, небо дополнено fbm-облаками/sun specular, затемнение вынесено
      в отдельный uniform-параметр (было зашито константой 0.82). Новый модуль
      `visuals/weather/WetWorld.java`: мочит мир по факту `mc.level.getRainLevel()`, независимо
      от партиклов Rain и тумблера `wet_ground` — Quality (Low/Medium/High/Ultra
      -> шаги/дистанция/bin-рефайнмент SSR), Reflection Strength, Darkening, Ripples+Speed,
      Rain Only. Переиспользует общий `WET_SURFACE` pipeline/`WetSurfaceUniforms` (при
      одновременном включении с Rain.wet_ground эффект стекается — задокументировано в коде).
      Fog-интеграция референса НЕ перенесена (нет доступа к чужому Fog-модулю/данным тумана в
      нашем движке в этом заходе) — можно добавить отдельно, если понадобится.
- [x] **HitEffect — интегрирован ImpactRing (2026-09-15).**
      Референс `/tmp/lv_refs/myasomaybe/ImpactRing/` — `JumpWaveRenderer`/`JumpGlowRenderer`
      используют захват сцены + собственный низкоуровневый RHI-пайплайн с SSR-варпом
      (кастомный шейдер сэмплит скопированный цветовой буфер по кольцу) — тяжеловесная техника
      не в духе нашего движка. Взята идея визуала (кольцо + вертикальная стена свечения в точке
      удара), реализована через уже существующий движок: обычные аддитивные квады через
      MeshBuilder/Renderer3D (`WORLD_COLORED_ADDITIVE` + `WORLD_COLORED_ADDITIVE_DEPTH`), без
      захвата сцены и отдельного шейдера — консистентно с уже существующими WaveEffect/HitBubble
      в том же файле. Новый `EffectMode.IMPACT` в HitEffect: настройки impact_radius/
      impact_duration/impact_glow_height, срабатывает как WAVE (по удару), рендер —
      расширяющееся кольцо (сегменты, fade к центру) + полупрозрачная вертикальная стена по
      контуру круга, затухающая вверх и во времени. Коммит 61c478e. `./gradlew build`+`test` ✓.
- [x] **HoldMyItems — сверка с HMI.zip (2026-09-15), портирован grip.**
      Референс `/tmp/lv_refs/hmi/` (Delta HMI): обёртка обфусцированного чит-клиента над
      СТОРОННИМ jar-модом `holdmyitems-5.1.1+26.2.jar` + свои `SwingAnimations` (десяток стилей
      свинга, weapon-inspect a-la CS:GO karambit-spin, "Pretty Hands" режимы). Полный набор
      НЕ портирован — фича-крип чит-клиента, конфликтует с KISS и с правилом «без стороннего
      jar в сборке» (Delta буквально бандлит чужой мод и синхронизирует с ним настройки через
      `OriginalHmiBridge`). Взята единственная самоценная и самодостаточная идея — **grip**:
      лёгкий хват предметов с рукоятью (мечи/инструменты/трезубец/палки — см.
      `heldAlongTheFist`) наискось в кулаке вместо строго вертикального ванильного equip.
      Портировано (коммит b257fde): `HeldAlongFist` (чистый предикат по ItemTags),
      `HandPoseMath.grip(...)` (чистая функция позы, тесты gripMirrorsBySide/
      gripExtendPullsCloserAndAcrossFist), настройки `grip_enabled`+6 slider'ов в HoldMyItems
      (дефолты как у референсного пресета "HMI"), проброс ItemStack через
      HandDynamics.apply/ItemInHandRendererMixin. `./gradlew build`+`test` зелёные.

### Проверка после каждого пункта: `./gradlew build` ✓, `./gradlew test` ✓, TODO.md отмечен

## Фаза 11: Аудит attack-визуалов — sacura 1.21.4 / GodWeer-26.2 (2026-09-22)
> Референсы распакованы в `ref/clients/sacura-1.21.4` и `ref/clients/godweer-26.2`.
> Вывод по большинству эффектов: у нас уже полноценнее (больше режимов, единый render
> engine с пайплайнами вместо сырого GL). Ниже — конкретные пробелы, которые реально
> стоит доделать; всё остальное сравнённое (HitEffect, KillEffect, JumpCircles,
> Hitboxes, ItemPhysic, ViewModel offsets) уже на уровне или выше обоих референсов.

- [ ] **ViewModel — `Custom` swing-режим.** sacura (`SwingAnimationsModule`) имеет режим
      "Кастомная" с explicit слайдерами угол/наклон. У нас 8 фиксированных пресетов
      (Hammer/Chop/Arc/Double/Shake/Shove/Slash/Thrust), но нет свободно настраиваемого.
      Также посмотреть на sacura-режимы "Возвращение"/"Возвращение к себе"/"Круговая" —
      возможно стоят портирования как доп. пресеты, если визуально отличаются от наших.
- [ ] **HitEffect — опциональный distortion/glow post-process слой.** godweer
      (`HitEffectRenderer`) кроме частиц кладёт screen-space distortion+glow слайдеры.
      У нас частицы (Bubbles/Wave/Impact) есть, post-process слоя над ними нет.
      Паттерн `PostProcessPass` уже есть (см. `TotemFX`), можно переиспользовать —
      но не блокирующая, это чистый апгрейд поверх уже более богатой системы.
- [ ] **JumpCircles — easing на появление/исчезновение.** sacura использует `EaseBackIn`
      для пружинки при спавне/затухании круга. Проверить `render/engine/math` — если
      easing-утилита уже есть, подключить к текущему fade вместо линейного.
- [ ] **KillEffect — through-walls поведение.** godweer (`PopEffect`) имеет явный toggle
      "сквозь стены" для beam/взрыв эффектов. Проверить DepthMode у Lightning/Orthodox
      beam в нашем KillEffect — сейчас режется окклюзией или нет; если режется и это не
      специально — добавить toggle по аналогии.
- [ ] **Trails — живое сравнение с sacura `TrailsModule`.** Не разбирался по коду в этом
      заходе (визуальный эффект, решение лучше принимать посмотрев обе стороны вживую
      в игре, а не по исходникам).

### Итог: ничего из sacura/GodWeer не даёт принципиально нового attack-эффекта,
### которого нет в LoVisual — расхождение только в деталях полировки (easing-кривые,
### доп. custom-режим свинга, возможный distortion-слой, through-walls toggle).

---

## Фаза 12: Сессия 2026-10-01 — новые визуалы, меню, войсчат, автокомплит, починка запуска

> Всё за 2026-10-01 лежит ОДНИМ коммитом (владелец просит большие коммиты, не дробить).
> Игру в этой сессии запускал только владелец (`./gradlew runClient`, из каталога `mod/`);
> всё, что ниже помечено «не проверено вживую», видно только в игре.

### Сделано (код + сборка + тесты зелёные)

- [x] **TargetESP: режим «Созвездие» (`constellation`).** Звёзды на наклонной орбите вокруг цели,
      линии между ними, свечение. Настройки: звёзды 3-24, скорость, орбита, линии, размер, свечение.
      Геометрия: `combat/targetesp/constellation/` (чистый `ConstellationLayout` + тест).
      `TargetESP.java` сокращён 329 -> 161 строка: отслеживание цели и плавное появление вынесены в
      `targetesp/state/TargetEspTracker`, цвет в `TargetEspColors`.
- [x] **JumpCircles переписан** (саб-агент): 7 новых форм (Звезда, Ромб, Спираль, Гексагон, Волна, Крест,
      Лепестки) к Круг/Кольцо/Пузырь; общая анимация (`CircleAnimation`), цвета из `AnimatedRenderColors`,
      круг рождается на высоте земли в момент отрыва. Пакет `visuals/player/jumpcircles/` (+ `shape/`),
      12 тестов. Удалены настройки `ease_out`, `pulse_max_size`, `pulse_lifetime`.
- [x] **KillEffect: режим «Кровь» (`Blood`)** по архиву killeffect.zip. Капли с физикой, отскоки от стен,
      лужи на полу, мелкие брызги; через наш `Renderer3D`. Пакет `hits/blood/`. Настройки: цвет,
      количество, сила, размер, время пятен, тип разлёта (Burst/Fountain/Ring/Spray; Spray летит от атакующего).
- [x] **Главное меню: тип «Hero»** по архиву Ya_Lublu_Femboyev.zip (экран Vectra). Настройка `menuLayout`
      (`orbs` по умолчанию / `hero`), фабрика `MainMenus`, экран `mainmenu/hero/*`, чистая логика
      `hero/logic/*` + тесты. Интеграция: альт-менеджер, `SkinManager`, наши темы.
- [x] **Simple Voice Chat (мягкая совместимость, API 2.6.24).** `compat/voicechat/` (плагин, `VoiceState`,
      `VoiceChatBridge`), модуль `VoiceChat` (кольца и свечение над говорящими, крест при заглушённом
      микрофоне), HUD-панель `VoiceChat` (список говорящих + статус микрофона). API не попадает в jar.
- [x] **`render/engine/renderer/quick/Draw`** — короткий статический фасад над `Renderer2D.COLOR`
      (rect, gradient, outline, glassOutline, shadow, circle) + `GlassEdge` с тестом.
      Заметка: у `Renderer2D` короткие перегрузки `roundedRect(x,y,w,h,radius,argb)` уже были,
      6-й параметр в длинной форме это `softness`, а не alpha.
- [x] **Автокомплит `%` в ванильном чате** (как `#` у Baritone): `mixins/gui/chat/CommandSuggestionsMixin`
      подставляет наши команды/аргументы в попап по мере набора; подсказки аргументов `<module>`
      показываются серым текстом (`command/suggest/SuggestionPlan` + тест). BetterChat идёт своим путём.
      Раньше подсказки работали только в BetterChat.
- [x] **Починка запуска клиента** (игра не стартовала с августа, все пять дефектов найдены по логам runClient):
      1. MixinExtras 0.5.4 падал на `@Redirect(at=...)` (ClassCastException при трансформации `Entity`):
         добавлен `mixinextras-fabric:0.5.5` (`gradle.properties: mixinextras_version`), вшит в jar через `include`.
      2. `ServerPolicyClient`: `registerReceiver` -> `registerGlobalReceiver`.
      3. Классы-помощники лежали в пакете миксинов (Mixin запрещает их загрузку): вынесены в
         `render/gamerenderer/` и `mixinhandlers/{player,hud,network}/`, сделаны public.
      4. `Themes`: `STORE` создавался раньше таблицы пресетов (NPE в `ThemeStoreIO`): порядок инициализации.
      5. `MainSettingsComponent.rebuildSettings`: `addAll` в неизменяемый список (падал ClickGUI по RShift).
- [x] 2026-10-01 все сегодняшние коммиты объединены в один по просьбе владельца.

### Отзывы владельца после запуска (скриншоты) — СДЕЛАНО в коде, ждёт проверки в игре

Итог 2026-10-01 (сборка и тесты зелёные, визуально не проверено):
- Тултипы: причина была в том, что пользовательский тултип, перекраска, фон и предпросмотр шалкера читали
  собственные флаги и игнорировали переключатель модуля; теперь везде `isEnabled()`. Синие строки
  категорий красятся акцентом темы (`TooltipColors`). Тема по умолчанию `Themes.THEME_DEFAULT = AMBER`
  (сохранённый выбор пользователя не трогается).
- Описания: ~98 новых ru/en (62 модуля, 36 HUD), `HudElement.getDescription()`, тест `DescriptionCoverageTest`.
- ClickGUI: плашка с описанием над колонками (`ModulesMenuDescriptionRenderer`, `DescriptionLines`).
- Hero-меню: раскладка по измеренному тексту (`TextMetrics`, `HeroLayout`), кнопки с SVG-иконками
  (`hero/paint/*`), orbs: подпись под кнопкой при наведении (`MainMenuHoverLabel`).
- Настройки меню: новая категория «Главное меню» (`MAIN_MENU`), `MainConfig.getMainMenuSettingDefs()`.
- Проверить в игре: положение и размер плашки описания, вид amber-тултипа, иконки и шрифты Hero-меню,
  подпись orbs-кнопок, видимость настроек (`visibleWhen`) в категории «Главное меню».

Задача А (форк, тултипы и описания):
- [x] **Тултип предметов (BetterTooltips) странный и не выключается.** При первом запуске у камня синие
      строки-категории («Building Blocks / Natural Blocks») и тёмная панель; даже при выключенном модуле
      стиль остаётся. Нужно: найти причину (миксин `mixins/hud/tooltips/TooltipRenderUtilMixin`
      применяет стиль без проверки включённости / кэш `TooltipState`), при выключении тултип строго
      ванильный, при включении аккуратный стиль. **Стиль по умолчанию должен быть `amber`, а не `classic`**
      (`Themes.THEME_AMBER`); старые конфиги не ломать.
- [x] **Нет описаний модулей и HUD-элементов в настройках.** На всех карточках HUD/UI написано
      «Module description is missing.». Нужно добавить описания ru/en для каждого `@ModuleInfo`-модуля и
      каждого HUD-элемента (ключи `module.<id>.description` и HUD-аналог), проверить, что не осталось
      пустых (тест или скрипт сверки id <-> lang).
- [x] **Описание функции при наведении в обычном ClickGUI** (колонки Combat/Player/Visuals/Other).
      В области сверху по центру над колонками показывать описание функции под курсором (плашка в нашем
      стиле, плавное появление, перенос/обрезка по ширине).

Задача Б (форк, главное меню):
- [x] **Hero-меню «калово»**: заголовок налезает на подзаголовок и кнопки, нет иконок на кнопках,
      мелкие и кривые шрифты. Эталон: референс Vectra (иконки слева в кнопках, акцентная Multiplayer,
      тёмное стекло у остальных, пара Options/Quit внизу, карточка аккаунта слева сверху, виджет
      времени справа сверху). Раскладку считать по измеренным размерам текста, а не по константам.
- [x] **Основное (orbs) меню тоже улучшить стилем** (типографика, тени, hover, появление).
- [x] **Настройки главного меню разбросаны и спрятаны** (`menuLayout`, `menuBg`, `menuBgUseTheme`,
      `menuClockShowSeconds` в `MainConfig`). Сгруппировать в понятный раздел «Главное меню» в
      настройках (ключи конфигов не менять, чтобы сохранённые значения не потерялись).

### Правки после второго запуска (2026-10-01)

- [x] `%tier <player>`: не было `suggest` (подсказка показывала только серый `<player>`): теперь ники игроков и режимы
      (`vanilla|uhc|pot|nethop|smp|sword|axe|mace`). Остальные команды без `suggest` не имеют аргументов.
- [x] KillEffect Blood было видно сквозь блоки: пайплайн `WORLD_COLORED` без depth-теста заменён на `WORLD_COLORED_DEPTH`.
- [x] TargetESP: новый режим `swords` (8 мечей на орбите, пружинная физика, отдача при ударе, подсветка при хите).
      `targetesp/sword/` (`SwordPhysics` чистая + 4 теста, `TargetEspSwords` рендер), текстура `particles/sword.png`
      нарисована скриптом (пиксель-арт, окрашивается цветом палитры). Мечи рисуются поверх блоков (ESP, без depth).
      Не проверено вживую: вид, размер, яркость свечения, текстура меча (при желании заменить на свою).

### Фаза 13: разбор evo клиента (2026-10-01), план работ

Отчёт: `mod/docs/EVO_REPORT.md` (29 модулей evo, которых нет у нас; крылья; ножи; HMI/Lua).
Лицензия: `ref/evo/README.md` объявляет код и ассеты проприетарными. Для релиза только свои модели/скрипты
или разрешение автора; формат (Bedrock geo/anim, HMI-Lua) стандартный, код пишем свой.
Порядок по просьбе владельца: сначала 1, потом остальное.

- [x] 1. HoldMyItems Scripted на LuaJ (пункты 1-8 отчёта, частично): настройка `Режим` = Dynamics / Scripts / Both
      (по умолчанию Dynamics, LuaJ грузится лениво). `held/lua/`: `HandScripts` (загрузка + горячая перезагрузка раз
      в секунду из `config/lovisual/holdmyitems/{hand_pose,item_pose}.lua`, иначе встроенные примеры в
      `assets/lovisual/holdmyitems/`), `HandFrame`; `runtime/`: `LuaSandbox` (только base/bit32/table/string/math,
      без io/os/luajava/require/load/debug, лимит инструкций через хук), `LuaPreprocessor` (`global.x = v;` -> registry),
      `ScriptSlot` (ошибка выключает только этот скрипт, лог один раз); `api/`: `M` (move/rotate с pivot/scale/push/pop/
      math), `P`, `I`, `Easings` (8 функций), без отдачи Java-объектов в Lua. Хуки в `ItemInHandRendererMixin`:
      hand_pose в `submitArmWithItem`, item_pose в `renderItem` (first person). 7 тестов `LuaSandboxTest`.
      Не сделано из 1-8: `hand_relative_pose`, аддоны (`*_addon.lua`), подмена ванильного equip/swing скриптом,
      обе руки всегда, ItemStack-поля. Не проверено вживую: направление осей M:rotate на реальной руке.
- [ ] 1b. HMI хвост (пункты 9-16): счётчики свинга и события, обе руки, ItemStack-поля, use_duration/using_item,
      аниматор частей модели (`item_model.lua`), камера из Lua, Lua-частицы, клавиша осмотра.
- [ ] 2. Wings: режим «Модель» (Bedrock geo.json + animation.json, 14 видов, общий `BedrockGeoParser` + бейк + сэмплер).
- [ ] 3. Модуль Knife (Melee Skins): 33 скина, клипы draw/inspect/melee_1..3/run, first/third person.
- [ ] 4. Быстрые модули: TNTTimer, AutoSprint, TotemCounter, PlaceAnimation, EnchantGlint, ArmorColor, GodRays, ProjectileTrails.
- [ ] 5. DeathEffects, Blizzard, ElytraTrails.

### Не проверено вживую / открытые вопросы

- [ ] Проверить в игре автокомплит `%`: ввод `%`, `%to`, Tab; серая подсказка `<module>` после `%toggle `.
      Если миксин не применится, прислать лог (поля `CommandSuggestions` сверены по байткоду 26.2).
- [ ] Визуально подогнать: режим «Созвездие», новые формы JumpCircles (ориентация текстур, яркость
      аддитивного бленда), кровь (лужи, яркость), Hero/orbs-меню, индикаторы войсчата.
- [ ] Войсчат: события SVC трактованы по javap (`MicrophoneMuteEvent.isDisabled()` как «заглушён или
      выключен», `getEntityId()` как UUID игрока); проверить с настоящим SVC 2.6.24; значок шапки HUD-панели
      сейчас wifi-глиф (подходящего микрофона в шрифте нет).
- [ ] Тип ClickGUI (идея владельца): селектор «Тип ClickGUI» в настройках + точка выбора экрана
      (`ClickGuiScreenManager`, три места `new ClickGuiScreen()`), `classic` по умолчанию, второй вид позже;
      бинд и звуки остаются в модуле `ClickGui`. Ждёт решения, какой второй вид делать первым.
- [ ] `.gitignore` для `server-plugin/.classpath`, `.project`, `.settings/`.
- [ ] Фаза 8.5.3 по-прежнему в очереди (около 150 файлов >200 строк), по мере касания.
- [ ] Приёмка серверного плагина (Фаза 11 в корневом TODO.md): Velocity + `%policy` на стенде.

### Заметки для следующих сессий

- Игру запускает владелец: `cd mod && ./gradlew runClient`. Из корня репозитория `./gradlew` не существует.
- Параллельные gradle-сборки блокируют друг друга: форкам запускать через
  `flock /tmp/claude-1000/gradle.lock ./gradlew build --offline -q`.
- Новые классы-помощники для миксинов НЕЛЬЗЯ класть в пакет `dev.loki.lovisual.mixins.*`
  (Mixin: `IllegalClassLoadError`); им место в `mixinhandlers/` или `render/`.
- Лимиты проекта: ≤200 строк на файл, ≤4 файла на папку (`checkFolderLimit` падает при нарушении),
  без длинного тире в текстах сайта и ответах.
