# Backend Structure (продумано заранее, 2026-09-23)

Rust workspace (`backend/Cargo.toml`), один сервис — один крейт. Планировка
на все три подсистемы платформы (см. `TODO.md`, Фаза 10) сразу, чтобы не
переизобретать layout при переходе от Подсистемы 1 к 2/3.

```
backend/
  Cargo.toml              # workspace root — members растёт по мере реализации
  STRUCTURE.md             # этот файл
  common/                 # РЕАЛИЗОВАН — общий контракт: JWT, внутренние заголовки,
                            # proto/accounts.proto (gRPC AuthenticateDevice,
                            # GetPublicProfiles для авторов витрины)
  accounts-service/        # РЕАЛИЗОВАН — backend/PLAN.md (Подсистема 1, часть 1)
  gateway/                # РЕАЛИЗОВАН — gateway/PLAN.md: единственная публичная
                            # точка, identity, рейт-лимиты, CORS, reverse proxy
  configs-service/         # РЕАЛИЗОВАН — configs-service/PLAN.md: 4 слота конфигов,
                            # share-коды, витрина publish/browse/detail/copy
                            # (Подсистема 1, часть 2)
  chat-service/            # ПЛАН НЕ НАПИСАН — RPC-чат, друзья, presence,
                            # виджеты-телеметрия (Подсистема 2). Единственный
                            # сервис с WebSocket, а не только REST.
  addons-registry/         # ПЛАН НЕ НАПИСАН — витрина аддонов, версии,
                            # публикация/модерация (Подсистема 3)
```

## Внутренний контракт (gateway ↔ сервисы)
Гейтвей — единственная публичная точка: он один раз резолвит вызывавшего
(JWT-access или `lvd_`-device-токен через gRPC `AuthenticateDevice`) и кладёт
аккаунт в заголовок `x-lovisual-account-id`; каждый запрос несёт общий секрет
`x-lovisual-internal-key`. Сервисы отвергают всё без этого ключа (403), поэтому
напрямую в них стучаться бесполезно. Снаружи — только REST/JSON на `api.<domain>`,
внутри — тот же REST сервисов + gRPC там, где публичного REST нет.

## Почему не добавлены в `[workspace] members` сразу
Пустой крейт без реализации — то же самое "без пустышек", что запрещено
правилами мода (см. `TODO.md`) — просто в Rust-обёртке: `cargo build
--workspace` не должен спотыкаться о нереализованные заглушки. Каждый
сервис входит в `members` в своём implementation-плане первым шагом (как
`accounts-service` в Task 1 `backend/PLAN.md`), не раньше.

## Общий код между сервисами (`common`, создан 2026-09-24)
Второй реальный потребитель появился вместе с `gateway`, поэтому общий код
уже вынесен: JWT `Claims`/`issue_access_token`/`verify_token`/`bearer_token`,
внутренний контракт (`require_internal_key`, `GatewayIdentity`, gRPC-перехватчики
ключа) и `proto/accounts.proto`. Следующий кандидат — типовой `AppError`:
сознательно НЕ вынесен, т.к. маппинг `From<sqlx::Error>` сервис-специфичен,
а `sqlx` не должен попадать в `gateway` (см. Global Constraints
`configs-service/PLAN.md`).

## Модульная структура внутри сервиса (эталон — `accounts-service`)
Домен, не технический слой: `auth/`, `accounts/`, `device/`, `avatars/` —
каждый со своими `mod.rs`/`handlers.rs`/`repo.rs` и т.п. (см. File Structure
в `backend/PLAN.md`). Тот же принцип — для `configs-service` (`slots/`,
`sharing/`, `showcase/`) и `chat-service` (`chat/`, `friends/`,
`telemetry/`) при их реализации.
