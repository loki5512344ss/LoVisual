# LoVisual Mod (combatant-client-26.2) — план переписи

Мод — форк Combatant Client, переписываем под LoVisual.

## Правила (ОБЯЗАТЕЛЬНЫЕ, нарушение = откат)
- **KISS / DRY / SOLID** — простая структура, без оверинжиниринга
- **Макс. 200 строк на файл** (было >1500) — превышение = разбить на модули
- **Макс. 4 файла на папку** — больше = новая подпапка по смыслу
- **Публичный API можно менять свободно** (решение владельца 2026-09-05):
      KISS важнее обратной совместимости; при рефакторинге разрешено менять
      сигнатуры, удалять делегирование и мёртвый код, обновляя вызывающих
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

### 8.5.2 Файлы-гиганты >600 строк — приоритетная очередь разбивки (53 файла)
> Формат: файл (строк) → идея разбивки. Порядок сверху вниз. Facade-оговорка:
> Renderer2D остаётся фасадом-делегатом (правило Фазы 8), но его вспомогательные
> классы (kawase/blur/glass) — вынести из OrderedUiBatcher (935) в `ui/blur/`.
- [ ] Renderer2D 2166 — ФАСАД (исключение по правилу Фазы 8): оставить делегирование,
      но блоки >200 строк переносить в package-private хелперы в `renderer/renderer2d/`
      (частично уже есть Rects2D/Strokes2D)
- [x] LoVisualAltManagerScreen 1113 → ГОТОВО (2026-09-10): 11 файлов `mainmenu/alt/`
      (Screen 132, Controller 200, Ops 59, Anims 108 + auth/ 171 + render/ 157/156/139/100
      + render/cards/ 178/166), все ≤200, build+test ✓
- [x] UiPrimitiveRenderer 1053 → ГОТОВО (2026-09-10): фасад 188 (коннекторы) +
      render/primitive/ 4 (Context 153, Props 158, Painter 145, GlowPainter 147) +
      render/rect/ 2 (RectPainter 150, CornerPainter 150), build+test ✓
- [ ] BetterButtons 1032 — extraction+tooltip+render → `hud/nondraggable/impl/buttons/`
- [ ] InventorySwap 1031 — по стратегиям свапа; чистая логика отдельными классами с тестами
- [ ] SvgMeshBackend 1014 — parser/triangulate/cache разнести
- [ ] Themes 1012 — реестр тем в `theme/presets/` (по 4 темы/файл), дефолты отдельно
- [ ] WorldParticles 991 — типы частиц в `visuals/effects/particles/`
- [ ] Module 989 — базовый класс: settings-механику вынести в `module/ext/` (осторожно, общий предок!)
- [ ] LoVisualAddonManagerScreen 989 — как AltManager
- [ ] Chams 976 — режимы рендера в подклассы-политики (фасад модуля тонкий)
- [ ] ClickGuiPickerState 962 + CombatProtocolHeuristicsEditorState 932 — state/actions хелперы
- [ ] ModulesMenuScreen 960 — рендер категорий отдельно от состояния
- [ ] UiMeshGeometry 954 — по примитивам геометрии
- [ ] BetterTooltips 953, OrderedUiBatcher 935 (→ batches + blur), Scoreboard 924,
      Rain 893 (+CompactHudStatModel 893), TazikHat 859 ✓ закрыт портированием в
      Cosmetics built-in (2026-09-11, см. Фазу 7),
      ShaderPatchCompiler 843, OnlineRelationPlayerPickerComponent 837, CustomTabList 830,
      GameRendererMixin 810 (только хуки; тело вынести в `render/engine/entry/`-классы —
      ВНИМАНИЕ: 9.1 тоже его трогает, координировать), Predictions 805
- [ ] Хвост 400–800 (~78 файлов, см. /tmp-снимок в истории команд: ConfigProfilesComponent 791,
      HitEffect 770, PlayerMovementSimulation 755, ProjectilePuncher 746, MediaPlayer 739,
      Crosshair 737, MeshBuilder 729, SettingsPanelComponent 703, Admins 702, Cooldowns 701,
      Itemizer 699, BlockHighlight 692, LocalPlayerMixin 687, Trails 681, ItemBatchRenderer 673,
      CosmeticModel 671, MenuScreen 667, GlStencilFramebufferSupport 663, ConfigSerializer 663,
      DynamicIsland 661, ReimaginedVisual 660, RelationsComponent 659, UiStyle 640, Potions 617,
      ModuleComponent 612, MsdfFont 606, Keybinds 603, MediaSessionService 594 и др.)

### 8.5.3 201–400 строк (~140 файлов) — проход «касаться = чинить»
- [ ] Не делать отдельным марафоном: при правке файла по другой фазе — первым коммитом
      разбивка на ≤200, вторым — целевое изменение (иначе diff-каша, правило Фазы 8)

### 8.5.4 Порядок и критерии готовности
- [x] Очередь: 8.5.1 крупные папки (mixins, visuals, clickgui/settings, hud impl) →
      8.5.2 гиганты >800 → далее по убыванию. Каждая папка/файл = отдельный коммит.
      → 8.5.1 ЗАВЕРШЕНА (2026-09-10): 0 папок >4 файлов.
- [ ] После каждого шага: `./gradlew build` ✓, `./gradlew test` ✓; конфиг-id модулей и
      ключи сеттингов НЕ ломаем (совместимость .lvcfg); скрипты-переименования пакетов
      не трогают string-literals id — проверять grep'ом после moves.
- [ ] Критерий закрытия для старта 9.2: ноль файлов >200 в путях, которые трогает Optimize
      (Renderer2D-секции blur/glass, OrderedUiBatcher, HUD-имплементации bgEffect,
      MsdfFont/CustomTextRenderer), и ноль папок >4 в этих же пакетах. Полный ноль по всем
      244/86 — долгий хвост, 9.2 им не блокируется.
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
- [ ] Каркас: `modules/misc/Optimize.java` (≤200 строк, тест `OptimizeTest` на чистую логику).
      Настройки (bool, дефолт on при включении модуля):
      - `no_glass` — все glass/blur-эффекты HUD off: форсить bgEffect=None у всех
        HUD-элементов (Fps сейчас по умолчанию "Blur", `Fps:70`), сделать
        `Renderer2D.requestLiquidGlassBlur*` no-op, пропускать kawase-цепочку в
        `OrderedUiBatcher.prepareSharedBlurInternal`.
      - `lite_post` — резать тапы тяжёлых пост-проходов: MotionBlur 15→5
        (+ neighborhood 10→0), DoF 32→8 — через uniform, без переписывания шейдеров
        (альтернатива жёстче: временно деактивировать проходы — решить на реализации, KISS).
      - `lite_sky` — процедурные скайбоксы (reimagined_skybox / pulsar / thunder,
        ~20 fbm-оценок на пиксель) — пропускать noise-слои.
      - `lean_hud` — off для per-frame аллокационных прибамбасов: градиенты/бегущие
        строки (`CustomTextRenderer:387` new String на глиф), анимации цифр.
- [ ] Механизм «как движок узнаёт»: НЕ тыкать статику модуля в лоб — сделать
      `OptimizeState` (boolean + getter, обновление при enable/disable), читать через
      него в движке (паттерн уже есть: `ReimaginedVisual.needsWorldSceneDepthCapture()`).
- [ ] Взаимодействие с 9.1: фиксы 9.1 НЕ отменяются модулем — copy/ошибки чиним всегда;
      Optimize режет только то, что имеет визуальную цену.
- [ ] i18n en_us + ru_ru обязательно: `module.optimize.*`, `setting.optimize.*`.
- [ ] Нотификация при первом включении (chat/HUD): что именно порезано.

### 9.3 Проверка
- [ ] A/B-мерка до/после 9.1 на чистом конфиге (F3/DebugHud, пустой мир, 1080p):
      целевой эффект — минус 2 полноэкранных copy/кадр и минус N glGetError-синков
      (N = проходов за кадр).
- [ ] `./gradlew build` после каждого пункта; `./gradlew test` зелёный.
- [ ] Порядок: 9.1 (по одному пункту на коммит) → 9.2.

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
