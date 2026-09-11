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
- [ ] Фаза 7: Новые модули по мотивам SoupVisuals — Cosmetics Overhaul ЗАВЕРШЁН 2026-09-11,
      см. ниже (остатки: Gps, HitBubbles, AmbientParticles, EndCrystal, Trinket, Balancer, HandsShader)
- [ ] Фаза 8.5: Аудит структуры ≤200/≤4 — 8.5.1 (папки >4) ЗАВЕРШЕНА 2026-09-10
      (0 папок >4, коммиты 8846d53…303b17e); далее 8.5.2 (гиганты >600),
      план в `combatant-client-26.2/TODO.md`
- [ ] Фаза 9: Оптимизация FPS — 9.1 (мёртвые грузы) ЗАВЕРШЕНА 2026-09-11 (137121e…4d60d3d);
      9.2 Optimize гейтится Фазой 8.5, 9.3 A/B-мерка needs запуск на Windows

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
- [x] **Cosmetics Overhaul (Unified Cosmetics)** — ЗАВЕРШЁН 2026-09-11:
      слоты `CosmeticSlot` (MODEL/PET/HEAD/SHOULDERS/BACK/WAIST/LEGS/HELD), парсинг
      `slot` из avatar.json + эвристика по имени папки, `CosmeticManager` на EnumMap
      (аксессуары — несколько одновременно), `CosmeticsScreen` (3D-превью drag/zoom +
      хотспоты + правая панель live-apply с "None" первым), per-slot offset/rot/scale
      через NumberValue-поля, built-in ChinaHat/TazikHat/BackSword в `cosmetics/builtin/`
      (старые модули chinahat/tazikhat/backsword удалены), i18n en+ru, тесты
      CosmeticSlotTest/CosmeticsHotspotTest.
      - ~~CosmeticDisplay 232 / TazikHatRenderer 242 / BackSwordRenderer 231 >200~~ —
        разбиты: CosmeticDisplay 196 + CosmeticBuiltins (значения+диспетчер built-in),
        TazikHat 186 (палитра-математика переиспользует ChinaHatPalette), BackSword 200
        (edge/glow слиты в параметризованный drawKatana).
      - Не сделано из плана (следующий заход): иконки-превью built-in в списке слота,
        правка слотов SHOULDERS/LEGS/PET из экрана (сейчас только HEAD/BACK built-in),
        предпросмотр до эквипа.
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
