# Слой оптимизации (Optimize): перенос референсных оптимизаций, улучшения и Vulkan

Статус: Этап 1 готов (2026-10-10), Этап 2 + начало Этапа 3 готовы (2026-10-10). Цель MC: 26.2.

## История изменений
- **2026-10-10**: Этап 1 завершён — 11 портированных файлов (`core/LruMap`, `entity/{EntityLod,CachedName,StateEntity,CachedStack}`, `particle/{ParticleLod,Workers}`, `hud/HudWorth`, `block/{TickCached,CachedLight,CachedValidity,SpawnerCache}`, `text/StableText`) + `OptimizeToggles` + `mod/NOTICE`. 53 теста. Патчи: `0001-feat-mod-...patch`, `0002-test-mod-...patch`.
- **2026-10-10**: Этап 2 завершён — `FrameBudget` (mode + target FPS + per-subsystem caps), `FrameProfiler` (5 секций EMA-таймингов), `OptimizePropertyBridge` (`-Dlovisual.opt.*` overrides), `OptimizeDebugHudMixin` (F3 overlay). 22 новых теста. `Optimize.java` расширен тумблерами `budget_mode` / `target_fps` / `show_profiler`.
- **2026-10-10**: Этап 3 (частично) — `RenderBackendOps` интерфейс + `GlRenderBackendOps` реализация (lazy clear, TBO cache, RenderTypes cache). Три mixin'а зарегистрированы в `lovisual.mixins.json`: `RenderTypesMixin`, `GlCommandEncoderClearMixin`, `GlCommandEncoderTexBufferMixin`. SkinAtlas/AnimFreeze/HudCache/FrameFence оставлены на поздний Этап 3.

## Решение
Все оптимизации из референса (слои `common`, `26.x`, `26.2`) переносятся в наш модуль `Optimize`, а не живут отдельным
модом. Sodium остаётся необязательным (`suggests`), выбор GL/Vulkan делается по бэкенду Blaze3D, а не по наличию Sodium.

## Две независимые оси
| Ось | Чем определяется | Что меняет |
|---|---|---|
| Бэкенд | `RenderSystem.getDevice() instanceof GlDevice`, иначе Vulkan | какая реализация `gl/` или `vulkan/` включается |
| Sodium | `FabricLoader.isModLoaded("sodium")` | только быстрые пути записи вершин (`VertexBufferWriter`) |

Без Sodium ландшафт остаётся ванильным. Ускорение ландшафта вне этого плана (возможный отдельный проект: форк Sodium).

## Структура
Пакет `features/module/modules/misc/optimize/`. Правила проекта: ≤4 `.java` на папку, ≤250 строк на файл,
JUnit на чистую логику, helper-классы вне `mixins.*`, миксины в `mixins/optimize/`.

```
optimize/
  Optimize.java      модуль и настройки (ключи совместимы с .lvcfg)
  core/              бэкенд-независимая логика: LOD-пороги, LRU, кэши, бюджет кадра
  entity/ particle/ hud/ block/ text/    перенос из референса по категориям
  gl/                GL-реализации: shared FBO, GpuFence, lazy clear, glCopyImageSubData
  vulkan/            Vulkan-реализации: vkCmdCopyImage, VkPipelineCache, кэш состояния
  profile/           профилировщик секций кадра
```

Выбор реализации один раз при старте через интерфейс `RenderBackendOps` (копирование текстур, фенсы, кольцо буфера).

## Что переносим (всё из референса)
1. **GL/рендер-пайплайн (то, что реально есть в слоях 26.x/26.2):** Shared Frame Fence (`FrameFence`, `RealFence`), Lazy Clear,
   FBO Sharing (`SharedDepthFbos`, `FrameBufferCacheMixin`, `GlBufferCloseMixin`), TBO Cache, RenderType Cache,
   VertexConsumer Cache, Entity Sort Skip. Immediate Ring Buffer и Ring Auto-Tune существуют только в слоях 1.21.x, в 26.x их нет
   (ванильные буферы там другие), поэтому в порт не входят; ring делаем заново только если замер покажет пользу (см. ниже).
2. **Сущности:** AnimFreeze, Entity LOD, Player LOD, Item LOD, Item Bounds Cache, Skin Atlas, Cached Name, CachedStack.
3. **Частицы:** Particle LOD, Parallel Particles, Parallel Vertices, Particle Light Cache, Particle Physics Skip,
   Bubble Column Cache.
4. **Блоки:** Spawner Cull, Spawner Tick Cache, Spawner Replay, Moving Block Cache, Piston Biome Cache, Sign Cache,
   Block Entity Cache (CachedLight, CachedValidity).
5. **HUD:** HUD Cache, HUD Worth, GUI Animated Items, GUI Item Atlas, совместимость с ImmediatelyFast Atlas.
6. **Текст:** Bidi Cache, Prepared Text Cache, StableText.
7. **Микро:** MemoryStack Cache, Sign Lookup Cache, Bubble Fluid Cache.

Поток кадра как в референсе: `FrameFence.endFrame` -> `SkinAtlas.endFrame` -> `AnimFreeze.frame++`
-> `OptConfig.refresh()` (флаги резолвятся один раз за кадр в примитивные поля).

## Что делаем лучше, чем в референсе
1. **Единый бюджет кадра.** Один объект раздаёт лимиты LOD, частицам и нашим эффектам (ProjectileTrails, ElytraTrails,
   DeathEffects, Blizzard, GodRays): дистанция, число точек и вершин на кадр. Режим «Выкл / Мягко / Агрессивно», по умолчанию
   «Выкл», целевой FPS задаёт пользователь. Это не авто-деградация, прежнее решение (Фаза 9) не нарушается.
2. **Профилировщик секций** (сущности, частицы, HUD, эффекты, текст) в debug-оверлее.
3. **Один конфиг** в `Optimize`, без отдельного экрана референса.
4. **Авто-порог параллельных частиц** по замеру (в референсе порог 4+ ядра взят из одного прогона).
5. **Кэш статичных HUD-виджетов в текстуру** (перерисовка только при изменении).
6. Флаги для бенчмарков: `-Dlovisual.opt.<key>`.

## Дополнительные GL-оптимизации (наши, гипотезы)
Проверяем замером на 26.2 до включения по умолчанию.
- **Кэш состояния GL** шире, чем в референсе: пропуск повторных `glUseProgram`, `glBindVertexArray`, `glBindTexture`, `glBlendFunc`,
  `glDepthMask` (LoVisual уже имеет `GlStateManagerMixin`, расширяем).
- **Сортировка draw по pipeline и текстуре** внутри слоя там, где порядок не влияет на результат (наши квады `Renderer2D`, ленты trails).
- **Persistent mapped ring** для динамических вершин наших эффектов и HUD (`GL_ARB_buffer_storage`), если профиль покажет стоимость
  `glBufferSubData`/map. Ring Auto-Tune из референса берём как идею размера.
- **Батчинг HUD:** меньше смен пайплайна между виджетами (общий batch для `Renderer2D.COLOR`/`TEXTURE`).
- **Инвалидация глубины/stencil** (`glInvalidateFramebuffer`) после проходов, чьё содержимое не читается: выгодно на тайловых и мобильных
  драйверах, на десктопе эффект мал.
- **Пропуск лишних clear** наших пост-процесс целей (`MenuBackgroundRenderer`, блюр), когда цель целиком перезаписывается.
- **Меньше FBO-переключений** в post-process цепочках (объединение проходов, общий ping-pong).
- **Uniform-блоки:** один обновляемый UBO на кадр вместо множества мелких обновлений (в LoVisual есть `DynamicUniformStorageMixin`).

## Разбор GL-части референса для 26.x/26.2 (что реально делает код)
Файлы референса:
- `26.x/.../gl/FrameFence.java` + `26.2/.../gl/RealFence.java`: один общий `GpuFence` на кадр вместо фенса на каждый запрос;
  если его ждут до конца кадра, реальный фенс создаётся сразу, как в ваниле. Подмена через `GlCommandEncoderMixin.createFence`.
- `26.x/.../mixin/gl/GlCommandEncoderClearMixin.java` (Lazy Clear): после `clearColorAndDepthTextures` не делается лишний
  `_glBindFramebuffer(.., 0)`, следующий проход всё равно привязывает свою цель.
- `26.2/.../gl/SharedDepthFbos.java` + `FrameBufferCacheMixin`, `GlBufferCloseMixin`, `GlCommandEncoderDepthClearMixin`: очистка
  глубины через кэшированный FBO и `glClearNamedFramebufferfv` (DSA), без attach/detach. Нужен GL 4.5 или `ARB_direct_state_access`.
- `common/.../gl/TexBufferCache.java` + `GlCommandEncoderTexBufferMixin`: не перепривязывает буфер к texel-buffer текстуре, если
  связка (формат, буфер) не менялась.
- `26.x/.../mixin/gl/RenderTypesMixin.java`: кэш `entityTranslucent` по `Identifier` вместо `Util.memoize` с `Pair`.
- `common/.../gl/RingAutoTune.java`: логика подбора размера кольца (в 26.x самого ring нет).
- Жёстко привязаны к GL (на Vulkan нужен свой путь или отключение): `SkinAtlas` (`glCopyImageSubData`), `SharedDepthFbos`,
  `RealFence`, TBO-миксины.

Не проверено: наш собственный GL-код (`render/engine/rhi/backend/gl`, `Renderer2D`, `GlStateManagerMixin`) почти не читался.
Перед этапом 3 нужен профиль HUD: число draw-вызовов и смен состояния на кадр, тогда решаем, какие пункты из раздела
«Дополнительные GL-оптимизации» нужны.

## Vulkan
Следующие пункты гипотезы, каждый подтверждаем профилированием и чтением классов `com.mojang.blaze3d.vulkan` в 26.2.

| Пункт | Зачем | Приоритет |
|---|---|---|
| `vkCmdCopyImage` вместо `glCopyImageSubData` (SkinAtlas) | без него атлас скинов работает только на GL | обязательно |
| Фенсы (`FrameFence`) через общий интерфейс | `RealFence` и `SharedDepthFbos` завязаны на GL, на Vulkan нужен отдельный путь или отключение | обязательно |
| Персистентный `VkPipelineCache` на диск | меньше фризов компиляции шейдеров, у нас уже есть `pipelineVariants` | высокий |
| Кэш состояния (pipeline, descriptor set, viewport/scissor) в `VulkanRenderPass` | пропуск повторных bind | средний |
| Лишние барьеры и layout-переходы между проходами | замер в RenderDoc | средний |
| Staging через host-visible VMA одним большим кольцом | кольцо стейджинга для динамических данных | средний |
| Асинхронная загрузка текстур на transfer-очереди | риск, делаем последним | низкий |

Существующие Vulkan-миксины LoVisual (`VulkanDeviceMixin` и др.) не переписываем, новый код подключается рядом.

## Этапы
1. ✅ `core/` + helpers + adapter interfaces + `OptimizeToggles` (бэкенд-независимые части). Тесты, сборка. **Завершён 2026-10-10** — 11 портированных .java, 53 теста, 0 падений, `checkFolderLimit` OK.
2. ✅ Бюджет кадра, профилировщик, `-Dlovisual.opt.*`. **Завершён 2026-10-10** — `FrameBudget` (3 режима: OFF/SOFT/AGGRESSIVE, target FPS, per-subsystem caps для entities/particles/HUD/effects/text/trails), `FrameProfiler` (5 секций, EMA, decay через 2 с idle), `OptimizePropertyBridge` (10 системных свойств + `all=false` отключает всё), `OptimizeDebugHudMixin` (F3 overlay показывает тумблеры + бюджет + тайминги секций + override'ы). 22 новых теста.
3. 🔄 `gl/`: shared FBO, FrameFence, lazy clear, TBO cache, SkinAtlas через GL; затем новые GL-идеи из раздела ниже. **Частично готово 2026-10-10**: `RenderBackendOps` интерфейс (GL/Vulkan стратегия), `GlRenderBackendOps` реализация, 3 mixin'а (`RenderTypesMixin`, `GlCommandEncoderClearMixin`, `GlCommandEncoderTexBufferMixin`), зарегистрированы в `lovisual.mixins.json`. **Осталось**: FrameFence/RealFence (shared GPU fence на кадр), SharedDepthFbos (DSA depth clear через cached FBO), SkinAtlas (glCopyImageSubData), AnimFreeze, ItemLod, LivingEntityLodMixin, ParticleMixin, SpawnerRendererMixin, BubbleColumnUpParticleMixin, VertexConsumerUtilsMixin, SodiumEntityRendererMixin, BlockEntity* mixins, HUD cache mixins, LevelRendererOutlineMixin, MinecraftFrameMixin.
4. ❌ `vulkan/`: `vkCmdCopyImage`, затем `VkPipelineCache`, затем остальное по замерам. **Не начат** — дизайн готов, ждёт профилирования GL-части чтобы понять, какие из Vulkan-оптимизаций реально нужны.
5. ❌ Бенчмарки до и после, отчёт цифр в этот документ. **Не начат** — требует запуска Minecraft на Windows, задача владельца.

## Оценки для Этапов 3-4 (после Stage 1-2)
- **Этап 3 (остальное)**: ~25 mixin'ов + ~8 helper-классов, ~80-100 новых .java-файлов. Сложные: `AnimFreeze` (vertex format + MemoryUtil), `SkinAtlas` (glCopyImageSubData + LRU), `HudCache` (RenderPipeline + RenderPass + TextureTarget). Безопасные простые: `LevelRendererOutlineMixin`, `BubbleColumnUpParticleMixin`, `SodiumEntityRendererMixin`. ~3-4 рабочих сессии.
- **Этап 4**: 6 пунктов, каждый требует профилирования. Минимум 1 сессия на `vkCmdCopyImage` (чтобы SkinAtlas заработал на Vulkan), остальное по мере замеров.

## Риски
- Миксины 26.2 проверяются только компиляцией и запуском владельцем (`./gradlew runClient`), вживую я не смотрю.
- Совместимость с ImmediatelyFast и Iris в 26.2 не проверена.
- Лимиты размера файлов проекта потребуют дробить крупные классы референса (фасад + package-private хелперы).
- Выигрыш по Vulkan не гарантирован, пока нет замеров.

## Вне объёма
Ускорение ландшафта (замена Sodium), авто-деградация без явного включения пользователем.
