use crate::sharing::codes::new_code;
use chrono::{DateTime, Utc};
use serde::Serialize;
use sqlx::PgPool;
use uuid::Uuid;

#[derive(Debug, Serialize, sqlx::FromRow)]
pub struct SlotSummary {
    pub slot: i16,
    pub name: String,
    pub updated_at: DateTime<Utc>,
    pub share_code: String,
    pub published: bool,
}

#[derive(Debug, Serialize, sqlx::FromRow)]
pub struct Slot {
    pub slot: i16,
    pub name: String,
    pub data: serde_json::Value,
    pub updated_at: DateTime<Utc>,
    pub share_code: String,
}

pub async fn list(pool: &PgPool, account_id: Uuid) -> Result<Vec<SlotSummary>, sqlx::Error> {
    sqlx::query_as(
        "SELECT s.slot_index AS slot, s.name, s.updated_at, s.share_code, (l.id IS NOT NULL) AS published
         FROM config_slots s
         LEFT JOIN showcase_listings l ON l.account_id = s.account_id AND l.slot_index = s.slot_index
         WHERE s.account_id = $1 ORDER BY s.slot_index",
    )
    .bind(account_id)
    .fetch_all(pool)
    .await
}

pub async fn get(pool: &PgPool, account_id: Uuid, slot: i16) -> Result<Option<Slot>, sqlx::Error> {
    sqlx::query_as(
        "SELECT slot_index AS slot, name, data, updated_at, share_code
         FROM config_slots WHERE account_id = $1 AND slot_index = $2",
    )
    .bind(account_id)
    .bind(slot)
    .fetch_optional(pool)
    .await
}

pub(crate) fn is_share_code_collision(err: &sqlx::Error) -> bool {
    matches!(err, sqlx::Error::Database(db) if db.constraint() == Some("config_slots_share_code_key"))
}

pub async fn save(
    pool: &PgPool,
    account_id: Uuid,
    slot: i16,
    name: &str,
    data: &serde_json::Value,
) -> Result<Slot, sqlx::Error> {
    // share_code is only used on INSERT; an update keeps the existing one.
    // 31^8 codes make collisions vanishingly rare, but never fatal.
    let mut attempts = 0;
    loop {
        let result = sqlx::query_as(
            "INSERT INTO config_slots (account_id, slot_index, name, data, share_code)
             VALUES ($1, $2, $3, $4, $5)
             ON CONFLICT (account_id, slot_index)
             DO UPDATE SET name = EXCLUDED.name, data = EXCLUDED.data, updated_at = now()
             RETURNING slot_index AS slot, name, data, updated_at, share_code",
        )
        .bind(account_id)
        .bind(slot)
        .bind(name)
        .bind(data)
        .bind(new_code())
        .fetch_one(pool)
        .await;
        match result {
            Err(err) if is_share_code_collision(&err) && attempts < 3 => attempts += 1,
            other => return other,
        }
    }
}

#[derive(Debug, Serialize, sqlx::FromRow)]
pub struct SharedConfig {
    pub name: String,
    pub data: serde_json::Value,
    pub updated_at: DateTime<Utc>,
}

pub async fn by_share_code(pool: &PgPool, code: &str) -> Result<Option<SharedConfig>, sqlx::Error> {
    sqlx::query_as("SELECT name, data, updated_at FROM config_slots WHERE share_code = $1")
        .bind(code)
        .fetch_optional(pool)
        .await
}

pub async fn regenerate_code(
    pool: &PgPool,
    account_id: Uuid,
    slot: i16,
) -> Result<Option<String>, sqlx::Error> {
    let mut attempts = 0;
    loop {
        let result = sqlx::query_scalar(
            "UPDATE config_slots SET share_code = $3
             WHERE account_id = $1 AND slot_index = $2 RETURNING share_code",
        )
        .bind(account_id)
        .bind(slot)
        .bind(new_code())
        .fetch_optional(pool)
        .await;
        match result {
            Err(err) if is_share_code_collision(&err) && attempts < 3 => attempts += 1,
            other => return other,
        }
    }
}
