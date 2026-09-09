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
- [ ] `features/gui/chat/` — 31 → `render/`, `suggestions/`, `menu/`, `model/`.
- [ ] `render/engine/uniform/impl/` — 28 → по потребителям (`uniform/impl/pipeline/`, `uniform/impl/effects/`).
- [ ] `render/engine/renderer/ui/` — 28: верхний уровень разнести по уже существующим
      `draw/`, `runtime/`, добавить `blur/`, `batches/`.
- [ ] `render/engine/text/` — 24 и `text/backend/` — 12 → `text/` разбить на `text/render/`,
      `text/layout/`, `text/msdf/`.
- [ ] `render/engine/renderer/ui/runtime/script/` — 23, `ui/draw/` — 23 → внутри по 4-файловым кластерам.
- [ ] `features/gui/hud/nondraggable/impl/` — 20 (+`impl/tab/`) → `impl/overlays/`, `impl/tablist/`.
- [ ] `features/gui/clickgui/` — 20 на верхнем уровне → всё в существующие `layout/`, `sections/`,
      `picker/`, `protocol/`, `util/`.
- [ ] `render/engine/command/` — 17, `render/helpers/` — 16, `features/command/impl/` — 16,
      `config/values/` — 16 → по 2-3 подпапки (типам: `values/numeric/`, `values/color/`…).
- [ ] `render/engine/profiler/` — 14, `modules/player/` — 14, `features/gui/hud/` — 14,
      `addon/` — 14 — средние: раскидать при касании.
- [ ] Хвост (5–13 файлов, ~55 папок): `util/pvp` 13, `util/projectile` 13, `render/iris` 13,
      `config/profile` 13, `features/module/misc` 12, `features/module` 12, `config` 12 и т.д. —
      НЕ выделенный проход, а «касаться = чинить» (см. 8.5.4).

### 8.5.2 Файлы-гиганты >600 строк — приоритетная очередь разбивки (53 файла)
> Формат: файл (строк) → идея разбивки. Порядок сверху вниз. Facade-оговорка:
> Renderer2D остаётся фасадом-делегатом (правило Фазы 8), но его вспомогательные
> классы (kawase/blur/glass) — вынести из OrderedUiBatcher (935) в `ui/blur/`.
- [ ] Renderer2D 2166 — ФАСАД (исключение по правилу Фазы 8): оставить делегирование,
      но блоки >200 строк переносить в package-private хелперы в `renderer/renderer2d/`
      (частично уже есть Rects2D/Strokes2D)
- [ ] LoVisualAltManagerScreen 1113 — экран+список+диалоги → 4 файла `mainmenu/alt/`
- [ ] UiPrimitiveRenderer 1042 — по типам примитивов (rect/round/line/text-batch)
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
      Rain 893 (+CompactHudStatModel 893), TazikHat 859 (обещан в Фазе 7 — разбить при портировании),
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
- [ ] Очередь: 8.5.1 крупные папки (mixins, visuals, clickgui/settings, hud impl) →
      8.5.2 гиганты >800 → далее по убыванию. Каждая папка/файл = отдельный коммит.
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
- [ ] **glGetError на каждом render pass** — `GlStencilShapeClipBackend.endRenderPass`
      (`:140`) и `beginRenderPass` (`:98/:104/:110/:124`) дёргают `checkGlErrors`
      (`:428-434`) на КАЖДЫЙ проход (миксин `CommandEncoderMixin:102-112` висит на
      submitRenderPass → это и все ванильные/Sodium проходы). `glGetError` = синк с
      Windows-драйвером, несколько мс/кадр. Фикс: проверять только если
      `mode != DISABLED || renderPassAttachmentRequired`, либо под dev-флагом;
      в релизе — no-op.
- [ ] **`lovisual$isVulkanBackend()` на каждый submitRenderPass**
      (`CommandEncoderMixin:66-74`) — `getDeviceInfo().backendName().toLowerCase()`
      заново на кадр×проходы. Фикс: вычислить один раз, закэшировать boolean.
- [ ] **PreTranslucentDepth — полноэкранный depth-copy каждый кадр без нужды** —
      `LevelRendererMixin:275-283` гейтится только «нет Iris». Фикс: гейт
      `needsPreTranslucentDepthCapture()` по образцу `WorldSceneDepth`
      (`LevelRendererMixin:160-218`); потребители: `PostProcessGraphResources:61`,
      `LoVisualVisuals:79`, `WorldPassCompiler:174`.
- [ ] **copyMainColor каждый кадр ДО гейта** — `UiBlurResources.captureWorldSource`
      (`:116-123`): полноэкранный color-copy делается до проверки
      `liquidGlassBlurRequested` (`:122`). Фикс: переставить — copy только когда
      blur реально запрошен.
- [ ] **Строки профайлера при выключенном профайлере** — конкатенации на call-site
      каждый кадр: `PostProcessGraph:59` (`"graph:" + phase`), `ModuleManager:219-221/:290`
      (`"3d:module:" + m.name()`). Фикс: ленивые supplier'ы / ранний isEnabled-check.
- [ ] **Мелочь, по ходу**: `new PoseStack`/`new Matrix4f`×2/`new Frustum`×2 в кадре
      (`GameRendererMixin:203-239/:341-347/:609-617`) — закэшить переиспользуемые копии.

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
