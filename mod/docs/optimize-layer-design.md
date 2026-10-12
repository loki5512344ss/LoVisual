# Слой оптимизации (Optimize): перенос референсных оптимизаций, улучшения и Vulkan

Статус: Этап 1 готов (2026-10-10), Этап 2 + начало Этапа 3 готовы (2026-10-10). Цель MC: 26.2.

## История изменений
- **2026-10-12**: референсная реализация обновлена с 1.0.1 до 1.0.3 (сверка исходников). Корректировки плана ниже, в разделе
  «Что нового в референсе 1.0.2-1.0.3».
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

## Что нового в референсе 1.0.2-1.0.3 (сверено 2026-10-12)
Референс обновился с 1.0.1 до 1.0.3 (+166 файлов). Что важно для нашего плана:
- **FrameFence/RealFence для 26.2 не нужен.** Комментарий в `26.1.2/gl/GlCommandEncoderMixin`: «26.1 ещё делает GL-sync
  на каждый fence; с 26.2 ваниль делает один fence на submit и ждёт его по индексу submit'а». Хук `createFence` переехал из
  `26.x` в модуль `26.1.2`, в 26.2 его нет; в changelog прямо сказано, что опция «Один fence на кадр» оставлена только для
  26.1.2, с 26.2 это делает игра. Плюс история краша Xaero's World Map («Cannot wait on a fence for the current submit»)
  при общем fence — риск, который ваниль уже закрыла. => Пункт «FrameFence/RealFence» из Этапа 3 вычеркиваем; остаётся
  только хук конца кадра (`MinecraftFrameMixin`: `SkinAtlas.endFrame`, `AnimFreeze.frame++`, `refresh()` конфига).
- **Счётчики draw-вызовов (`DrawStats`, `common/gl/DrawStats.java`, 75 строк)** — ровно наше заявленное предусловие
  «перед Этапом 3 нужен профиль HUD: число draw-вызовов и смен состояния на кадр». Считает draws/flush'и слоёв/FBO-bind'ы,
  средние за секунду, топ-5 слоёв, рисует только пока открыт F3. Дёшево, даёт данные для раздела «Дополнительные
  GL-оптимизации». => Включить в Этап 3 первым пунктом.
- **Parallel Models (крупное новое, Impact HIGH).** `26.2/ModelFeatureRendererMixin` перехватывает `buildGroup` и собирает
  группу submit'ов на нескольких ядрах (`ParallelModelHook` + `ParallelModels` + два invoker'а). Заявлено +20-50% FPS в
  толпе бронированных мобов, отдельная опция. Отключается при <4 ядрах и при активном шейдерпаке Iris
  (`!(IRIS && IrisApi…isShaderPackInUse())` — Iris собирает расширенные атрибуты из вершин, отданных по одной).
  Зависит от `AnimFreeze.freezes()` (часть, которую нельзя реплеить) — ставить после AnimFreeze.
- **ItemLod получил точный таргет в 26.2**: `entity/ItemFeatureRendererMixin` — `@WrapOperation` на `Submit.quads()` в
  `prepareMainSubmit`/`prepareOutlineSubmit`/`prepareFoilSubmit`, плоские квады дальше `ItemLod.distanceSq()`.
- **Выброшенные предметы (новое):** `no_item_sort` (пропуск сортировки `item_entity_translucent_cull`, по умолчанию вкл,
  до +40% FPS на кучах), `item_single_copy` (стак одним предметом), `ItemBounds` (кэш AABB модели предмета, IdentityHashMap
  с полной очисткой >4096 — render state пересоздаётся каждый кадр, его собственный кэш умирает вместе с ним).
  `BufferSourceMixin`+`RenderTypeAccessor` живут в модуле `26.1.2`, в `26.x` их нет — перед переносом проверить, есть ли
  в 26.2 `MultiBufferSource.BufferSource.endBatch(RenderType, BufferBuilder)` и `RenderType.sortOnUpload()`.
- **Фиксы HudCache, которые надо взять сразу** (иначе воспроизведём баги): главный target снимается в `beginFrame`
  (`main = gameRenderer.mainRenderTarget()`) и сравнение идёт с ним, а не с «текущим» — иначе редиректим чужой target
  (Xaero подменяет свой, пока рисует); двойной троттлинг `GuiItemRefresh`+`HudCache` приводил к пропавшим зачарованным
  предметам хотбара (фикс: пропускать свой троттлинг, когда `HudCache.active()`); composite — premultiplied + write mask
  7 (RGB), depth clear 0 (reversed depth в 26.x); кэш выключается при чужих HUD-кэшах (`axolotlclient`, `exordium`,
  `gnetum`) и под VulkanMod; интервал = рефреш-рейт монитора (`MonitorRate`, GLFW-опрос 1 раз в секунду).
- **Правило утечек:** кэши с нативной памятью/GL-объектами чистить и на перезагрузку ресурсов (F3+T), и на смену мира.
  Фикс 1.0.3: `BubbleColumnUpParticleMixin` держал `ClientLevel` в статике через сильную ссылку — теперь `WeakReference`
  + очистка карты при смене уровня; `MovingBlockCache`/`ParallelModels` чинили утечки native/model.
- **Порог параллельных частиц:** план п.4 («авто-порог по замеру») упрощаем до того, что сделано в референсе —
  `CORES >= 4` + `-D…=auto|true|false`; 6+ ядер оказалось догадкой (замер на 4 ядрах дал +12%), вернули к 4.
  `Workers` получил `stream()` — публикация задач по мере готовности, рабочие берут их сразу, пока готовится остальное.
- **MixinPlugin** — условная регистрация миксинов (Sodium/Vulkan/Lunar, `shouldApplyMixin` + `getMixins`), а тумблеры в UI
  прячутся, если миксин не построен/не применяется (`available()` → `MixinPlugin::applies`). У нас уже есть
  `mixins/LoVisualMixinPlugin` — расширить для Vulkan (Этап 4) и для «миксин есть не всегда».
- **Не берём:** Lunar multi-draw (лаунчер Lunar, не наш случай); `blockEntityEarlyCull` перенесён только в `1.21.9+`,
  в `26.x` его нет — при случае проверить, применим ли к 26.2; `26.3` поддержка и `26.3/renames.txt` пригодятся при
  апгрейде версии (пакет `com.mojang.blaze3d` → `com.mojang.renderpearl`).

## Этапы
1. ✅ `core/` + helpers + adapter interfaces + `OptimizeToggles` (бэкенд-независимые части). Тесты, сборка. **Завершён 2026-10-10** — 11 портированных .java, 53 теста, 0 падений, `checkFolderLimit` OK.
2. ✅ Бюджет кадра, профилировщик, `-Dlovisual.opt.*`. **Завершён 2026-10-10** — `FrameBudget` (3 режима: OFF/SOFT/AGGRESSIVE, target FPS, per-subsystem caps для entities/particles/HUD/effects/text/trails), `FrameProfiler` (5 секций, EMA, decay через 2 с idle), `OptimizePropertyBridge` (10 системных свойств + `all=false` отключает всё), `OptimizeDebugHudMixin` (F3 overlay показывает тумблеры + бюджет + тайминги секций + override'ы). 22 новых теста.
3. 🔄 `gl/` + остаток рендера. **Готово 2026-10-10**: `RenderBackendOps`, `GlRenderBackendOps`, `RenderTypesMixin`, `GlCommandEncoderClearMixin`, `GlCommandEncoderTexBufferMixin`. **Готово 2026-10-10…10-11**: миксины `LivingEntityLodMixin`, `ParticleMixin`, `BubbleColumnUpParticleMixin`, `SodiumEntityRendererMixin`, `BlockEntity*`, `ChestRendererMixin`, `LevelRendererOutlineMixin`, `BaseSpawnerMixin`, `EntityName/State/Renderer*`. **Осталось, в порядке приоритета** (порядок пересмотрен 2026-10-12 по референсу 1.0.3):
   1. ✅ `DrawStats` + строки в F3 — счётчики draw/flush/bind за кадр (предусловие для решений о GL-оптимизациях; дёшево). **Готово 2026-10-12** — `DrawStats`, миксины `mixins/optimize/stats/*` (executeDraw/Draws/Indirect/Multiple, setPipeline, glBindFramebuffer), строка F3 `draws/pipelines/fbo binds`, подключены `FrameProfiler.beginFrame/endFrame` (баг Стадии 2: не вызывались в проде), 7 тестов.
   2. ✅ `SharedDepthFbos` кластер — `SharedDepthFbos` (DSA `glClearNamedFramebufferfv`, fallback на ваниль без GL4.5/DSA), `FrameBufferCacheMixin` (remember/forget по `getFbo`/`destroyFbo`), `GlCommandEncoderDepthClearMixin` (`clearDepthTexture` HEAD, cancellable), `GlBufferCloseMixin` (`forgetTboCache()` при `close` GlBuffer.Direct — чинит протухание TBO-кэша, id буфера переиспользуется). **Готово 2026-10-12** — тумблер `fbo_share` (модуль + `-Dlovisual.opt.fbo_share`), методы `RenderBackendOps.fboShareSupported/clearDepthInPlace`, миксины в `mixins/optimize/gl/` и `mixins/optimize/gl/fbo/`, 8+3 тестов.
   3. ~~`SkinAtlas` кластер~~ **Выкинут 2026-10-12** — в референсе 1.0.3 фичи нет на 26.2 (в `26.2/` нет `SkinAtlas.java`/`LivingEntityRendererSkinMixin`, флаг `skinAtlas()` никем не вызывается; атлас скинов был только для 1.21.4, см. историю версий референса, раздел «1.21.4»). Как и FrameFence, не переносим.
   4. ✅ `AnimFreeze` кластер — `AnimFreeze` (capture раз в тик, реплей в вершины), `EntityAnimHolderMixin`, `AnimHolder`, метод `freezes(state, pose)` (часть, построенная каждый кадр в другом месте, не реплеим — это и чинит рябь брони), `RawVertices`/`SodiumVertices`. **Готово 2026-10-12** — `AnimFreeze.configure(enabled, distance)` (свой холдер, не OptimizeToggles), тумблеры `entity_anim_freeze` (по умолчанию выкл) + `entity_anim_distance` (16), `frame++` в `MinecraftMixin.renderFrame` HEAD, вызов из `ModelFeatureRendererMixin.prepareModel` (`@WrapOperation` на `Model.renderToBuffer`), инфраструктура сырых вершин из п.7 (`RawBuffer`/`BufferBuilderRawMixin`/`RawVertices`/`SodiumVertices`/`VertexConsumerUtilsMixin`), 3 теста. TypeBuilders не выделялся — свёрнут в приватные хелперы `AnimFreeze`.
   5. ✅ `ItemLod` — `ItemLod` + `ItemFeatureRendererMixin` (`@WrapOperation` на `Submit.quads()` в `prepareMain/Outline/FoilSubmit`) + `ItemBounds`. **Готово 2026-10-12** — `ItemLod`/`ItemBounds` в `render/engine/optimize/entity/` (в `entity/` теперь ровно 4 файла — лимит исчерпан), LOD-хук добавлен в существующий `ItemFeatureRendererMixin`, `ItemStackRenderStateMixin`+`ItemLayerAccessor` в `mixins/render/item/bounds/`, тумблеры `item_lod`/`item_lod_distance` (8)/`item_bounds`, 9 тестов.
   6. ✅ `HudCache` кластер — `HudCache`, `MonitorRate`, `HudWorth` (есть), миксины `GameRendererHudMixin`, `GuiHudMixin`, `GuiRendererTargetMixin`, `HudInvoker`, `HudLiveMixin`; все фиксы совместимости из раздела выше — сразу. **Готово 2026-10-12** — тумблер `hud_cache` **по умолчанию выкл** (взаимодействует с нашими deferred-HUD-маркерами в `GuiRendererMixin`); гейт на `axolotlclient`/`exordium`/`gnetum`/`vulkanmod` через FabricLoader; main target снимается в `beginFrame`; composite premultiplied + write mask 7, depth clear 0; `HudInvoker` в подпапке `hud/cache/invoker/` (лимит 4 файлов), 9 тестов (MonitorRate clamp+троттлинг, HudWorth-гистерезис).
   7. ✅ `VertexConsumerUtilsMixin` (кэш `VertexBufferWriter`, только при Sodium — в `OPTIONAL_SODIUM_MIXINS`) + `BufferBuilderRawMixin`/`RawBuffer` (запись без Sodium) + `RawVertices`/`SodiumVertices`. **Готово 2026-10-12** (упреждённо, как предусловие п.4) — гейт на `micro_opts`, 2 миксина в `mixins/optimize/gl/raw/`.
   8. ✅ `ParallelModels` — `ParallelModelHook`, `ModelFeatureRendererMixin` (`buildGroup` cancellable), invoker'ы; после `AnimFreeze` (зависит от `freezes()`). **Готово 2026-10-12** — `ParallelModels`/`ParallelModelHook`/`ModelGeometry`/`ModelBuilder` в `render/engine/optimize/parallel/` (оригинальный 348-строчный класс разбит на 4 файла, лимит 250), `Workers` получил `stream()` (публикация по мере готовности), 3 интефейса в `mixins/optimize/entity/parallel/`, `buildGroup`-inject добавлен в существующий `ModelFeatureRendererMixin`, тумблер `parallel_models` (on) с внутренними гейтами: ≥4 ядер, нет активного Iris-шейдерпака, взаимоисключение с `AnimFreeze`, формат `DefaultVertexFormat.ENTITY` (36 байт, оффсеты сверены javap), `parts[0]=null` против утечки weak-map, native-страницы освобождаются в `reset()`, 7 тестов (3 stream + 4 гейтинг).
   9. По желанию/по замерам: `no_item_sort` + `item_single_copy` (`BufferSourceMixin`, `ItemClusterRenderStateMixin`) — сначала проверить наличие `endBatch(RenderType, BufferBuilder)` в 26.2. **Проверено 2026-10-12**: двухаргументного `endBatch(RenderType, BufferBuilder)` в 26.2 `BufferSource` **нет**, `MeshData.DrawState` без sort-методов; пункт остаётся отложенным до замеров и дизайна под 26.2-API.
   Хук конца кадра не нужен отдельным `MinecraftFrameMixin`: `renderFrame` у нас уже перехвачен (`mixins/render/main/MinecraftMixin`), туда же вешаем `SkinAtlas.endFrame`/`AnimFreeze.frame++`/конфиг-`refresh()`. **FrameFence/RealFence вычеркнут** — см. «Что нового», ваниль 26.2 делает это сама.
4. ❌ `vulkan/`: `vkCmdCopyImage`, затем `VkPipelineCache`, затем остальное по замерам. **Не начат** — дизайн готов, ждёт профилирования GL-части чтобы понять, какие из Vulkan-оптимизаций реально нужны.
5. ❌ Бенчмарки до и после, отчёт цифр в этот документ. **Не начат** — требует запуска Minecraft на Windows, задача владельца.

## Оценки для Этапов 3-4 (после Stage 1-2)
- **Этап 3 (остальное)**: порядка 20 mixin'ов + ~10 helper-классов, ~70-90 новых .java-файлов. Сложные: `AnimFreeze` (vertex format + MemoryUtil), `SkinAtlas` (GPU-to-GPU копия + LRU), `HudCache` (RenderPipeline + RenderPass + TextureTarget), `ParallelModels`. Простые: `DrawStats`, `SharedDepthFbos`, `ItemLod`. ~3 рабочие сессии (FrameFence вычеркнут).
- **Этап 4**: 6 пунктов, каждый требует профилирования. Минимум 1 сессия на `vkCmdCopyImage` (чтобы SkinAtlas заработал на Vulkan), остальное по мере замеров.

## Риски
- Миксины 26.2 проверяются только компиляцией и запуском владельцем (`./gradlew runClient`), вживую я не смотрю.
- Совместимость с ImmediatelyFast и Iris в 26.2 не проверена.
- Лимиты размера файлов проекта потребуют дробить крупные классы референса (фасад + package-private хелперы).
- Выигрыш по Vulkan не гарантирован, пока нет замеров.

## Вне объёма
Ускорение ландшафта (замена Sodium), авто-деградация без явного включения пользователем.
