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
- [x] Фаза 7: Новые модули по мотивам SoupVisuals — ЗАВЕРШЕНА 2026-09-16 (Cosmetics
      Overhaul 2026-09-11; Gps/HitBubbles/AmbientParticles/EndCrystal/Balancer/Trinket/
      HandsShader — 2026-09-15…16, см. ниже)
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
- [x] **Gps** (world) — ЗАВЕРШЕНО 2026-09-15: компас-стрелка + маркер к точке
      (`visuals/world/Gps.java`, `util/world/GpsBearing.java` чистая математика + тесты,
      команда `%gps <x> <y> <z> | clear`). Референс: `world/Gps.java` — идея взята
      (стрелка+маркер+дистанция+авто-удаление), код не копировался. Xaero-связка НЕ
      сделана (была опциональной идеей, не в скоупе запроса). Коммит fb0b288.
- [x] **Light** (world) — кастомный цвет освещения (у Soup 41 стр.). Референс: `world/Light.java`
- [x] **HitBubbles** (particles) — ЗАВЕРШЕНО 2026-09-15: отдельный модуль
      `visuals/effects/HitBubbles.java` (не режим HitEffect.BUBBLES — независимое включение,
      трёхфазная анимация появление/жизнь/исчезновение через новый `ThreeStageProgress`).
      Заодно вынесены общие хелперы из HitEffect (DRY): `AttackHitPoint`,
      `OrientedQuadRenderer`, `AnimatedColorModeOptions`. Референс: `particles/HitBubbles.java`
      (идея взята, код не копировался; интерполяции/множественные текстуры Soup не портированы).
      Коммит 5f2fc70.
- [x] **AmbientParticles + FireFlyParticle** (particles) — ЗАВЕРШЕНО 2026-09-15: модуль
      `visuals/particles/AmbientParticles.java`, два режима — Ambient (плывущие мошки) и
      Firefly (блуждание + кольцевой буфер шлейфа вместо Trail-объектов референса). 19
      визуальных стилей референса (Stars/Hearts/Bloom/.../Pyramid, asset-тяжёлые) НЕ
      портированы — фича-крип чит-клиента вне направления проекта; цвет через уже
      существующий AnimatedRenderColors вместо своей Sync/Custom-системы. Коммит 28ec166.
- [x] **NameProtect** (misc) — подмена своего ника и ников друзей в тексте чата/скорборда
      (у Soup 42 стр.). Референс: `other/NameProtect.java`
- [x] **EndCrystal** (visuals) — ЗАВЕРШЕНО 2026-09-15: `visuals/scene/EndCrystal.java` +
      `mixins/render/entity/EndCrystalRendererMixin.java` (WrapOperation на ModelPart.render,
      различает 2 вызова glass + 1 cube счётчиком-ordinal — тонировка/скрытие частей) +
      inject в `ClientLevelMixin.removeEntity` (осколки-вспышка на месте разрушения, т.к.
      ваниль убирает сущность мгновенно). Референс: `other/EndCrystal.java` — идея взята
      (кастомный цвет, "доиграть" разрушение), Sync/Custom×4-цвета+Wave/Vertex упрощены до
      общего AnimatedRenderColors. Коммит 50db8e3.
- [x] **Trinket** (не "2.0" — CosmeticDisplay удалён откатом 2026-09-11, апгрейдить нечего) —
      ЗАВЕРШЕНО 2026-09-16: отдельный модуль `visuals/items/Trinket.java` в стиле текущих
      ChinaHat/TazikHat. `util/physics/TrinketPhysics` (чистая, тесты) — шарик на пружине
      у ног игрока (гравитация/отскок/тетер). Рендер — тонированный billboard (наш
      AnimatedRenderColors) + затухающий шлейф (кольцевой буфер) + звук на отскок
      (ванильный SoundEvents.SLIME_JUMP). Библиотека 10 иконок-спутников референса и
      8 кастомных звуковых тем НЕ портированы — нужны новые ассеты. Референс:
      `other/Trinket.java` (там тоже почти нет реализации — только настройки).
      Коммит 840a716.
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
- [x] **Balancer** (mctiers) — ЗАВЕРШЕНО 2026-09-16: `features/module/modules/misc/Balancer.java`
      (fixType Vanilla/UHC/Pot/NethOP/SMP/Sword/Axe/Mace) + `util/mctiers/MCTiersCache`
      (асинхронный кэш, паттерн SkinManager) + `util/mctiers/MCTiersTier` (форматирование,
      тесты) + команда `%tier <ник> [режим]`. Референс `mctiers/Balancer.java` почти пуст —
      логика была в недоступной McTiersModule-базе; реальный публичный API mctiers.com
      найден и проверен через сторонний open-source плагин (sinnayuh/MCTier):
      `GET https://mctiers.com/api/search_profile/<username>`. Коммит cf81ee4.
- [x] **HandsShader** — ЗАВЕРШЕНО 2026-09-16: сверка показала, что Chams уже реализует
      9/10 режимов Soup (Solid/Chroma/Balatro/Smoke/Stripes/Glow/Glass/Snow + свои
      Metallic/Noise) и полный dual/blend-набор (Mix/Add/Multiply/Screen/Overlay/
      Difference, `HandBlendUniforms`) — сверх ожиданий. Не хватало только "Invert":
      добавлен `hand_chams_invert.frag` (RGB-инверт под маской руки через уже
      существующий UBO HandChams/u_Mix.x, без HSV-тумблеров и edge-glow референса —
      это дублировало бы наш Glow) + регистрация пайплайна + вписан в оба mode-списка
      Chams (основной и dual shader1/shader2). Референс: `visuals/HandsShader.java`.
      Коммит 76ffdec. **Это был последний пункт Фазы 7** — SoupVisuals-раздел закрыт
      полностью (Gps/HitBubbles/AmbientParticles/EndCrystal/Balancer/Trinket/HandsShader).
- [x] **Фикс ChinaHat** — текущая геометрия кривая (смахивает на самбреро/зонт);
      переделать модель в нормальную конусную шляпу по референсу Soup
      (`visuals/ChinaHat.java` + `ChinaHatModel.java`).
