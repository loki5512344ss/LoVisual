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
