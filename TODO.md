# LoVisual

Проект состоит из одного компонента — мода.

| Компонент | Стек | TODO |
|-----------|------|------|
| `combatant-client-26.2/` | Java 25, Fabric (MC 26.2) | [`combatant-client-26.2/TODO.md`](combatant-client-26.2/TODO.md) |

> Лаунчер (Rust + Slint) и backend (Rust + axum) удалены из проекта — развитие только мода.

---

## Правила (для мода, продублированы в его TODO)
- KISS / DRY / SOLID
- Макс. 200 строк на файл (фасады-делегаты исключение)
- Макс. 4 файла на папку
- Никакого статика, никаких this-escape, никаких мёртвых миксинов
- Без пустышек; каждый этап компилируется (`./gradlew build`)
- Тесты обязательны для новых модулей (JUnit)

---

## Статус
- [x] Фаза 1: Ребрендинг `combatant` → `lovisual`
- [x] Фаза 2: Выкинуть Runtime/JarReplacement + Panic
- [x] Фаза 3: Разбивка 12 файлов-гигантов
- [x] Фаза 3.5: Удаление читерских модулей (легит-направление)
- [x] Фаза 4: Security (BackdoorProtection, StaffTracker)
- [x] Фаза 5: Нормализация модулей (ленивый Minecraft, GameClock)
- [x] Фаза 6: Тесты (EventBus, конфиг, утилиты) — см. `combatant-client-26.2/TODO.md`
- [ ] Фаза 7: Новые модули по мотивам SoupVisuals — см. ниже
- [ ] Фаза 8.5: Аудит структуры ≤200/≤4 — 8.5.1 (папки >4) ЗАВЕРШЕНА 2026-09-10
      (0 папок >4, коммиты 8846d53…303b17e); далее 8.5.2 (гиганты >600),
      план в `combatant-client-26.2/TODO.md`
- [ ] Фаза 9: Оптимизация FPS (9.1 фиксы мёртвых грузов → 9.2 модуль Optimize),
      план в `combatant-client-26.2/TODO.md`

---

## Фаза 7: Новые модули — референс SoupVisuals
> Референс: `ref/soupvisuals-src/` — SoupVisuals 3.2.0 (MC 1.21.11, лицензия SOUP-1.0).
> Код референса НЕ копируем (чужая лицензия + декомпил) — только пере-реализация идей
> под наши правила (≤200 строк/файл, ≤4 файла/папку, без статики, тесты).
> Модули референса лежат в `ref/soupvisuals-src/padej/soup/implement/features/modules/`.

- [x] **Hitboxes** (visuals) — цветные хитбоксы сущностей, как у Soup: ТОЛЬКО рендер
      контейнера с настройкой цвета/видимости, БЕЗ увеличения хитбокса (увеличение —
      чит, удалён в Фазе 3.5). Референс: `visuals/Hitboxes.java`
- [x] **Watermark** (HUD, draggable) — обещан в README, в коде отсутствует
- [ ] **Gps** (world) — луч/маркер направления к точке (у Soup ~400 стр.); связка
      с Xaero-waypoints и координатами. Референс: `world/Gps.java`
- [x] **Light** (world) — кастомный цвет освещения (у Soup 41 стр.). Референс: `world/Light.java`
- [ ] **HitBubbles** (particles) — пузыри при ударах по игрокам (у Soup 323 стр.).
      Референс: `particles/HitBubbles.java`
- [ ] **AmbientParticles + FireFlyParticle** (particles) — атмосферные частицы мира /
      светлячки (у Soup 775 + 241 стр.). Референс: `particles/AmbientParticles.java`,
      `particles/types/FireFlyParticle.java`
- [x] **NameProtect** (misc) — подмена своего ника и ников друзей в тексте чата/скорборда
      (у Soup 42 стр.). Референс: `other/NameProtect.java`
- [ ] **EndCrystal** (visuals) — улучшенный рендер энд-кристаллов (у Soup 217 стр.).
      Референс: `other/EndCrystal.java`
- [ ] **Trinket 2.0** — апгрейд нашего CosmeticDisplay до уровня Soup Trinket:
      иконки-спутники (Atom/Ball/Cat/Donut/Flame/Flower/Gem/Ghost/MCTiers), трейл
      (цвет sync/custom, длина, толщина), звуки (типы + громкость), частицы
      (у Soup 315 стр.). Референс: `other/Trinket.java`
- [ ] **Cosmetics Overhaul (Unified Cosmetics)** — сейчас модуль бесполезен (next/prev
      keybind без экрана). План (обсуждён в брейнсторме, черновик, детали могут
      измениться):
      - **Слоты вместо типов**: новый `CosmeticSlot` enum — `MODEL, PET, HEAD,
        SHOULDERS, BACK, WAIST, LEGS, HELD` (8 шт). `CosmeticType` (MODEL/PET/ACCESSORY)
        остаётся внутри `CosmeticModel` как есть (анимация/рендер-логика не трогается).
      - Аксессуары (ACCESSORY) получают поле `slot` — парсится из `avatar.json`
        (`"slot": "head"`), при отсутствии — вывод по имени папки (существующая
        эвристика witch/hat→HEAD, wing→BACK, wheel→WAIST, иначе→HELD).
      - `CosmeticManager`: `EnumMap<CosmeticSlot, CosmeticModel>` вместо 3 полей.
        MODEL/PET — по одному активному. 6 accessory-слотов — **можно носить
        несколько одновременно** (шляпа + крылья + пояс разом).
      - Экран `CosmeticsScreen`: левая панель — живая 3D-модель игрока
        (`InventoryScreen.renderEntityInInventoryFollowsMouse` или свой вызов
        EntityRenderDispatcher), drag ЛКМ по превью — вращение (yaw/pitch,
        pitch ограничен ±30°), скролл — zoom (0.5–2.0), кнопка сброса угла.
        Открывается через **ПКМ по модулю в ClickGUI** (спецкейс вместо обычной
        settings-панели — прецедент есть в `ClickGuiEditorScreen`).
      - Hotspot-зоны поверх 3D-модели (HEAD/SHOULDERS/BACK/WAIST/LEGS + иконка
        HELD сбоку) — клик открывает правую панель выбора модели для этого слота.
      - Offset/rotation/scale — **на слот** (не на модель, т.к. модулей может быть
        десятки, а статичных SettingDef-полей на каждую не сделать): 6 стандартных
        num()-хелперов модуля (per-slot), visibleWhen(активный слот). Смена модели
        в слоте может потребовать подстройки — ок, вписывается в существующий
        SettingDef/SettingFactory конвейер без новой инфраструктуры.
      - Цвета из Theme.theme() (windowBg/accent/textMuted), файлы по правилам
        ≤200 строк/≤4 файла на папку: `CosmeticsModule`, `CosmeticsScreen`,
        `CosmeticsSlot`(=CosmeticSlot), `CosmeticsSlotPanel`.
      - i18n: `module.cosmetics.*`, `setting.cosmetics.active_slot`,
        `offset_x/y/z`, `rotation`, `scale`, `setting.cosmetics.slot.<slot>` и т.д.
        (en_us + ru_ru обязательно).
      - Открытые вопросы для следующего захода: точные хитбокс-координаты зон
        по проценту региона превью, формат хранения per-slot offset в .lvcfg,
        нужен ли предпросмотр модели до эквипа в правой панели.
      - **Портирование процедурных head/back-модулей в слоты** — из всех
        `visuals/*` только 3 модуля реально являются «надетыми на игрока»
        процедурными косметиками (нашёл через grep по HeadAnchor-хелперу):
        - `ChinaHat` (+`ChinaHatModel`, `ChinaHatPalette`, 295+107+144 стр.) —
          конус на голове → встроенная опция слота **HEAD**.
        - `TazikHat` (859 стр.! требует разбивки на файлы ≤200 стр. при
          портировании — геометрия/палитра/модуль отдельно, как у ChinaHat) —
          «тазик»/чаша на голове → **HEAD**.
        - `BackSword` (287 стр.) — катана на спине → **BACK**.
        Остальные visuals-модули (ESP, Chams, Trails, Rain, PostFX и т.д.) —
        НЕ переносятся, это общие эффекты, а не надеваемые предметы.
        План: каждый становится встроенным вариантом (built-in entry, не
        .bbmodel) внутри соответствующего слота — юзер выбирает между
        своими .bbmodel-моделями И этими процедурными built-in вариантами
        в одном и том же слоте. Настройки самого варианта (цвет/размер/сегменты
        и т.д.) показываются в правой панели слота, когда built-in выбран
        активным. Старые `@ModuleInfo`-модули `chinahat`/`tazikhat`/`backsword`
        удаляются как отдельные модули из списка Visuals (их функциональность
        полностью переезжает в Cosmetics) — конфиг-миграция/бэккомпат не нужны
        (легит-клиент, публичного релиза со старыми конфигами ещё не было —
        уточнить у пользователя перед удалением).
      - **"None" в каждом слоте** — обязательный первый вариант в списке выбора
        любого слота (Model/Pet/Head/Shoulders/Back/Waist/Legs/Held): "ничего
        не надето", слот пуст, `CosmeticManager.active` для этого слота = null.
        Не путать с выключенным модулем `CosmeticDisplay` целиком — None это
        per-slot состояние, остальные слоты продолжают рендериться.
      - Подтверждено пользователем: точно только 3 built-in кандидата
        (ChinaHat/TazikHat/BackSword), остальные visuals-модули не переносятся.
      - **Решённые вопросы (финал черновика):**
        1. Hotspot-координаты — **вариант B**: проекция костей игрока (голова/тело/
           ноги vanilla-модели) в экранные координаты через `ScreenProjection`
           (уже есть в `render/helpers/`), а не фиксированные проценты региона —
           точнее держит зоны при вращении/приседе. Fallback на фикс-проценты,
           если в реализации окажется дороже, чем стоит.
        2. Хранение offset/rot/scale — **обычные `NumberValue`-поля модуля**
           (6 слотов × offset_x/y/z/rot/scale), сохраняются в `.lvcfg` как
           обычные настройки — без нового ConfigValue-типа.
        3. Правая панель — **live-apply**: клик по модели в списке слота сразу
           применяет и показывает на превью, без отдельной кнопки Apply.
        4. Built-in процедурные варианты (ChinaHat/TazikHat/BackSword) — **просто
           обычные записи в списке слота** (с иконкой-превью), не выделяются в
           отдельную секцию; настройки открываются так же, как у .bbmodel-моделей.
        5. `TazikHat` (859 стр.) при портировании — **разбить как ChinaHat**:
           `TazikHatGeometry` + `TazikHatPalette` + встраиваемый рендер-класс без
           собственного `@ModuleInfo`/Module-обвязки (вызывается из слота HEAD).
        6. Старые модули `chinahat`/`tazikhat`/`backsword` — **удаляются
           полностью** из списка Visuals, функциональность только в Cosmetics.
- [ ] **Balancer** (mctiers) — тиры игроков с mctiers.info (у Soup отдельная категория
      MCTiers). Референс: `mctiers/Balancer.java`
- [ ] **HandsShader: новые шейдеры руки** — у Soup 10 режимов (Solid, Chroma, Balatro,
      Smoke, Stripes, Glow, Glass, Invert, Snow) + dual-режим с blend-режимами
      (Mix/Add/Multiply/Screen/Overlay/Difference); шейдеры лежат в
      `ref/soupvisuals-src/assets/soupapi/shaders/hand/`. Пере-реализовать недостающие
      и добавить к нашим (BackSword/Chams hand shaders уже есть dual — сверить набор).
      Референс: `visuals/HandsShader.java`
- [x] **Фикс ChinaHat** — текущая геометрия кривая (смахивает на самбреро/зонт);
      переделать модель в нормальную конусную шляпу по референсу Soup
      (`visuals/ChinaHat.java` + `ChinaHatModel.java`).
