# Frontend Architecture (продумано заранее, реализация — следующий план)

> Статус: структура согласована до написания implementation-плана фронтенда,
> чтобы план сразу проектировался под правильные границы, а не переписывался
> после первого клубка спагетти. Стек: React + Vite + TypeScript + Tailwind
> (см. `TODO.md`, Фаза 10). Правила платформы (≤250 строк/файл и т.д.) — там же.

## Принцип: структура по фичам, не по типу файла

Раскладывать `src/` на `/components`, `/hooks`, `/services`, `/types` вперемешку —
самая частая ошибка в React-проектах: одна фича размазывается по всему дереву
папок, и её нельзя удалить или вынести, не задев остальное. Вместо этого —
каждая фича владеет всем, что ей нужно, в одной папке.

```
frontend/src/
  features/
    auth/                    # регистрация/логин, device-link экран
      components/
      hooks/
      api.ts                 # вызовы к accounts-service через gateway
      types.ts
    configs/                 # 4 слота, share-коды, витрина
      components/
      hooks/
      api.ts
      types.ts
    avatars/
    admin/                   # /admin, видим только role=admin
  shared/                    # только то, что реально используют 2+ фичи
    components/              # кнопки, инпуты, модалки — дизайн-система
    hooks/                   # useAuth (общий контекст сессии), useDebounce и т.п.
    api/
      client.ts              # общий fetch-wrapper: base URL gateway, JWT в заголовке,
                              # обработка 401 (refresh) и 429 (rate-limit) в одном месте
    types.ts                 # общие DTO, если совпадают на нескольких фичах
    i18n/                    # LanguageSwitch, common.ru.ts / common.en.ts (typed dictionaries)
  pages/                     # роуты верхнего уровня, тонкие — просто собирают
                              # фичи в layout, никакой бизнес-логики
  app/                       # App.tsx, routes.tsx, i18n.ts (initI18n), i18next.d.ts (typed keys)
  main.tsx
```

Каждая фича, у которой есть строки для пользователя, также владеет своей
`i18n/` подпапкой (`ru.ts`, `en.ts`) — одно пространство имён i18next на
фичу (`auth`, `landing`, ...), русские словари как источник (`as const`),
английские типизированы через `satisfies Translation<typeof ru>` из
`shared/i18n/common.ru.ts`, так что расхождение ключей — ошибка компиляции.

Правило раздела shared/feature: если код используется ровно в одной фиче —
он живёт в этой фиче, а не в `shared/`. Переносить в `shared/` только когда
появился второй потребитель — не заранее "на всякий случай" (YAGNI).

**Лимиты из правил платформы (`TODO.md`) действуют и тут**: ≤250 строк на
файл, ≤4 файла на папку (подпапки не считаются) — если в `components/`
внутри фичи появляется 5-й файл, выносим смысловую подпапку, а не копим.

## Антипаттерны, которых сознательно избегаем

- **Barrel-файлы `index.ts` на каждую папку** — удобны на вид, но ломают
  tree-shaking и IDE go-to-definition на больших проектах; экспортируем
  напрямую из файла, где определено
- **Prop drilling через 4+ уровня** — если проп передаётся через компоненты,
  которым он сам не нужен (просто транзитом) — это сигнал на React Context
  (для auth-сессии) или на подъём компонента ниже по дереву, а не терпеть
  drilling
- **Бизнес-логика в `pages/`** — страницы только компонуют фичи и layout;
  если в файле страницы появляется `fetch`/сложная derived-логика — она
  переезжает в соответствующую фичу
- **Один гигантский `api.ts` на весь проект** — обращения к API живут в
  папке своей фичи (`features/configs/api.ts` и т.д.), общий только
  low-level `shared/api/client.ts` (fetch-wrapper)
- **any вместо нормальных типов** — DTO с бэкенда типизируются по контрактам
  из `backend/PLAN.md` (Task 6: `RegisterResponse`, `LoginResponse` и т.д.),
  не `any`/`unknown` без сужения

## Тестирование
Тесты живут в подпапке `tests/` внутри фичи (или `shared/*`), которую они
проверяют — не рядом с файлом (`Component.test.tsx`) и не в отдельном дереве
на верхнем уровне. Это всё ещё удаляется вместе с фичей при её удалении/
переносе, но не съедает бюджет "≤4 файла на папку" — колокация `X.test.tsx`
рядом с `X.tsx` вдвое сокращала бы вместимость каждой папки под этим правилом.

```
frontend/src/
  features/
    auth/
      components/
      hooks/
      api.ts
      types.ts
      tests/
        api.test.ts
        components.test.tsx
  shared/
    ui/
      Button.tsx
      text-field/
        TextField.tsx
        tests/
          text-field.test.tsx
```

Источники: [Robin Wieruch — React Folder Structure Best Practices 2026](https://www.robinwieruch.de/react-folder-structure/), [Mastering Modern React + Vite Folder Structure, Medium](https://sandeshrathnayake.medium.com/mastering-modern-react-vite-folder-structure-a-production-ready-guide-for-scalable-applications-9ad8e233f8b9).

## Единственное допустимое ребро shared → feature
`shared/layout/` (шапка и каркас приложения) импортирует `useSession` из
`features/auth/session`: шапке нужно знать, вошёл ли пользователь. Это
единственный случай, когда `shared/` зависит от фичи; остальной `shared/`
от фич не зависит.
