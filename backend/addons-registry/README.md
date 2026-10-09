# addons-registry (planned)

Addon marketplace backend: versions, publishing, moderation (Подсистема 3).
See `TODO.md` (Фаза 10, "Подсистема 3") and `backend/STRUCTURE.md`. No
implementation plan yet.

## Publish gate (decided, applies when this service is built)

Publishing addons is allowed only for accounts with a **verified email**.
There is no `can_publish_addons` HTTP endpoint to call: the check lives in
the caller, and the data comes from accounts-service:

- `AuthenticateDeviceReply.email_verified` (gRPC, `common/proto/accounts.proto`)
  — returned alongside `account_id` since the 0006 migration. For
  site-session flows (publishing from the site), call the accounts-service
  `/me`-equivalent or extend the internal gRPC with an account lookup; do
  not re-derive verification state locally.
- Rule: `email_verified == false` → publish endpoints answer `403 Forbidden`
  with `email not verified`, mirroring `/device/confirm`.

