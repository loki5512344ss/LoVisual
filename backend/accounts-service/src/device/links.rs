use crate::auth::tokens::{hash_token, new_opaque_token};
use chrono::{DateTime, Utc};
use common::internal::DEVICE_TOKEN_PREFIX;
use serde::Serialize;
use sqlx::PgPool;
use uuid::Uuid;

#[derive(Debug, Serialize, sqlx::FromRow)]
pub struct DeviceLink {
    pub id: Uuid,
    pub linked_at: DateTime<Utc>,
    pub last_seen: Option<DateTime<Utc>>,
}

pub async fn create(pool: &PgPool, account_id: Uuid) -> Result<String, sqlx::Error> {
    let token = new_opaque_token(DEVICE_TOKEN_PREFIX);
    sqlx::query("INSERT INTO device_links (account_id, device_token_hash) VALUES ($1, $2)")
        .bind(account_id)
        .bind(hash_token(&token))
        .execute(pool)
        .await?;
    Ok(token)
}

/// Resolves a device token to its account (plus the account's email
/// verification state for callers that gate capabilities on it) and bumps
/// `last_seen`. Returns `None` for unknown tokens (and for nothing else —
/// revocation deletes the row, so revoked tokens are just unknown).
pub async fn authenticate(pool: &PgPool, token: &str) -> Result<Option<(Uuid, bool)>, sqlx::Error> {
    sqlx::query_as(
        "UPDATE device_links dl SET last_seen = now()
         FROM accounts a
         WHERE dl.device_token_hash = $1 AND a.id = dl.account_id
         RETURNING dl.account_id, a.email_verified_at IS NOT NULL",
    )
    .bind(hash_token(token))
    .fetch_optional(pool)
    .await
}

pub async fn list(pool: &PgPool, account_id: Uuid) -> Result<Vec<DeviceLink>, sqlx::Error> {
    sqlx::query_as(
        "SELECT id, linked_at, last_seen FROM device_links WHERE account_id = $1 ORDER BY linked_at DESC",
    )
    .bind(account_id)
    .fetch_all(pool)
    .await
}

/// Deletes the link only if it belongs to `account_id`; `false` otherwise.
pub async fn revoke(pool: &PgPool, account_id: Uuid, link_id: Uuid) -> Result<bool, sqlx::Error> {
    let result = sqlx::query("DELETE FROM device_links WHERE id = $1 AND account_id = $2")
        .bind(link_id)
        .bind(account_id)
        .execute(pool)
        .await?;
    Ok(result.rows_affected() == 1)
}

/// Deletes the link a device token belongs to (RFC 7009-style self-revoke:
/// holding the token is the proof). `false` when the token is unknown.
pub async fn revoke_by_token(pool: &PgPool, token: &str) -> Result<bool, sqlx::Error> {
    let result = sqlx::query("DELETE FROM device_links WHERE device_token_hash = $1")
        .bind(hash_token(token))
        .execute(pool)
        .await?;
    Ok(result.rows_affected() == 1)
}
