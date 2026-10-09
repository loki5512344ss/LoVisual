# accounts-service: исходящая почта, подтверждение email и сброс пароля

Статус: черновик на согласование (2026-10-09).

## Решения владельца
- Отправка через внешний SMTP (Resend, `smtp.resend.com`, логин `resend`, пароль = API-ключ), адрес `noreply@loki-code.dev`.
- Неподтверждённый email: **логин и обычное использование разрешены**, ограничены **привязка аккаунта к моду (`/device/confirm`)**
  и **публикация аддонов**.
- DNS (Cloudflare) и секреты настраивает владелец. Корневой SPF не нужен (Resend работает на поддомене `send`). DMARC без `rua`:
  `v=DMARC1; p=none;` (отчёты на внешний адрес без разрешения принимающего домена не доставляются).

## Этап 1: подтверждение email
1. Миграция `0005_email_tokens.sql`:
   - `ALTER TABLE accounts ADD COLUMN email_verified_at TIMESTAMPTZ;`
   - `email_tokens(id UUID PK, account_id UUID FK ON DELETE CASCADE, purpose TEXT CHECK in ('email_verify','password_reset'),
     token_hash TEXT UNIQUE, expires_at, used_at, created_at)`; хранится только SHA-256 токена (как `refresh_tokens`).
   - Существующие аккаунты остаются неподтверждёнными (решение по миграции старых: см. вопросы).
2. Модуль `src/mail/`: трейт `Mailer`, `SmtpMailer` на `lettre` (rustls), `NoopMailer` (пишет в лог, если `SMTP_HOST` пуст:
   dev и тесты), шаблоны писем. Конфиг: `SMTP_HOST`, `SMTP_PORT` (587 STARTTLS по умолчанию, 465 implicit TLS), `SMTP_USER`,
   `SMTP_PASSWORD`, `MAIL_FROM`, `PUBLIC_BASE_URL` для ссылок. Секреты только из env, `.env.example` с пустыми значениями.
3. Токены: `src/auth/email_tokens.rs`: генерация (32 случайных байта, base64url), хеш, TTL (verify 24 ч, reset 1 ч), одноразовость
   (`used_at`), при выдаче нового токена того же `purpose` старые инвалидируются.
4. Эндпоинты:
   - `POST /auth/verify-email {token}`: помечает `email_verified_at`, токен одноразовый.
   - `POST /auth/resend-verification` (нужен вход): новое письмо, лимит частоты на аккаунт и IP.
   - `register` ставит отправку в фоновую задачу, ответ не ждёт SMTP; ошибка отправки только логируется.
5. Ограничения для неподтверждённых:
   - `/device/confirm` возвращает 403 `email not verified`.
   - Публикация аддонов: проверка в `addons-registry` через данные аккаунта из gRPC (поле `email_verified` добавить в ответ
     `accounts-service`); `can_publish_addons` в `accounts-service` сам по себе нигде не проверяется, проверка живёт на стороне
     вызывающего сервиса.
   - `/me` отдаёт `email_verified`, чтобы сайт и мод показали плашку «подтвердите email».
6. Сайт: страница `/verify?token=...`, вызывающая эндпоинт, и плашка с кнопкой «выслать письмо снова».
7. Тесты: чистая логика токенов (хеш, TTL, одноразовость) и флоу на `axum-test` по образцу `tests/auth_flow`.

## Этап 2: сброс пароля
- `POST /auth/forgot-password {email}`: всегда одинаковый ответ (нет перечисления email), письмо только если аккаунт есть;
  лимит частоты; задержка выравнивается как в `login`.
- `POST /auth/reset-password {token, new_password}`: проверка токена, смена хеша пароля, отзыв всех refresh-токенов аккаунта.

## Безопасность
- Токены только хешем, сравнение по хешу в БД, одноразовые, с коротким TTL.
- Ответы эндпоинтов не раскрывают, существует ли email.
- Ссылки только на `PUBLIC_BASE_URL` из конфига, не из заголовков запроса (защита от host header injection).
- API-ключ Resend: права только на отправку, хранится в `.env` на VPS (`chmod 600`), не в репозитории.

## Открытые вопросы
1. Старые аккаунты: считать подтверждёнными автоматически (миграция ставит `email_verified_at = now()`) или требовать
   подтверждения? Рекомендую пометить подтверждёнными, чтобы не заблокировать существующих.
2. Где проверяется публикация аддонов в `addons-registry` (нужно прочитать код перед этапом 1, п.5).
