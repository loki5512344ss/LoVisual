# Backend Structure (продумано заранее, 2026-09-23)

Rust workspace (`backend/Cargo.toml`), один сервис — один крейт. Планировка
на все три подсистемы платформы (см. `TODO.md`, Фаза 10) сразу, чтобы не
переизобретать layout при переходе от Подсистемы 1 к 2/3.

```
backend/
  Cargo.toml              # workspace root — members растёт по мере реализации
  STRUCTURE.md             # этот файл
  accounts-service/        # РЕАЛИЗУЕТСЯ — backend/PLAN.md (Подсистема 1, часть 1)
  gateway/                 # ПЛАН: gateway/PLAN.md — routing + единая точка JWT-проверки
                            # + рейт-лимиты (TODO.md §6). Начинается после
                            # accounts-service, т.к. фронтит уже готовый сервис.
  configs-service/         # ПЛАН: configs-service/PLAN.md — 4 слота конфигов, share-коды,
                            # витрина (Подсистема 1, часть 2)
  chat-service/            # ПЛАН НЕ НАПИСАН — RPC-чат, друзья, presence,
                            # виджеты-телеметрия (Подсистема 2). Единственный
                            # сервис с WebSocket, а не только REST.
  addons-registry/         # ПЛАН НЕ НАПИСАН — витрина аддонов, версии,
                            # публикация/модерация (Подсистема 3)
```

## Почему не добавлены в `[workspace] members` сразу
Пустой крейт без реализации — то же самое "без пустышек", что запрещено
правилами мода (см. `TODO.md`) — просто в Rust-обёртке: `cargo build
--workspace` не должен спотыкаться о нереализованные заглушки. Каждый
сервис входит в `members` в своём implementation-плане первым шагом (как
`accounts-service` в Task 1 `backend/PLAN.md`), не раньше.

## Общий код между сервисами (`common`, пока не создан)
Кандидаты на вынос в `backend/common/` **когда появится второй реальный
потребитель** (не раньше — YAGNI): JWT `Claims`/`verify_token` (сейчас
только в `accounts-service`, но `gateway` тоже будет его проверять — при
старте `gateway` вынести в `common`), типовой `AppError`. До этого момента
дублирование двух сервисов — не проблема, преждевременная общая библиотека
между одним реальным потребителем — проблема.

## Модульная структура внутри сервиса (эталон — `accounts-service`)
Домен, не технический слой: `auth/`, `accounts/`, `device/`, `avatars/` —
каждый со своими `mod.rs`/`handlers.rs`/`repo.rs` и т.п. (см. File Structure
в `backend/PLAN.md`). Тот же принцип — для `configs-service` (`slots/`,
`sharing/`, `showcase/`) и `chat-service` (`chat/`, `friends/`,
`telemetry/`) при их реализации.
