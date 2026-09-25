use chrono::{DateTime, Utc};
use sqlx::{PgPool, Postgres, query::QueryAs, query_as};
use uuid::Uuid;

pub const PAGE_SIZE: i64 = 20;

#[derive(Debug, Clone, Copy)]
pub enum Sort {
    New,
    Popular,
}

#[derive(Debug, sqlx::FromRow)]
pub struct ListingRow {
    pub id: Uuid,
    pub account_id: Uuid,
    pub title: String,
    pub description: String,
    pub copies_count: i32,
    pub published_at: DateTime<Utc>,
    pub name: String,
}

const LISTING_FROM: &str = " FROM showcase_listings l
     JOIN config_slots s ON s.account_id = l.account_id AND s.slot_index = l.slot_index";
const LISTING_COLS: &str =
    "l.id, l.account_id, l.title, l.description, l.copies_count, l.published_at, s.name";

pub enum PublishOutcome {
    Published(Uuid),
    SlotEmpty,
    Hidden,
}

pub async fn publish(
    pool: &PgPool,
    account_id: Uuid,
    slot: i16,
    title: &str,
    description: &str,
) -> Result<PublishOutcome, sqlx::Error> {
    // The FK to config_slots makes this fail for an empty slot; check first for a clean 404.
    let exists: bool = sqlx::query_scalar(
        "SELECT EXISTS(SELECT 1 FROM config_slots WHERE account_id = $1 AND slot_index = $2)",
    )
    .bind(account_id)
    .bind(slot)
    .fetch_one(pool)
    .await?;
    if !exists {
        return Ok(PublishOutcome::SlotEmpty);
    }
    let hidden: Option<bool> = sqlx::query_scalar(
        "SELECT hidden FROM showcase_listings WHERE account_id = $1 AND slot_index = $2",
    )
    .bind(account_id)
    .bind(slot)
    .fetch_optional(pool)
    .await?;
    if hidden == Some(true) {
        return Ok(PublishOutcome::Hidden);
    }
    let id = sqlx::query_scalar(
        "INSERT INTO showcase_listings (account_id, slot_index, title, description)
         VALUES ($1, $2, $3, $4)
         ON CONFLICT (account_id, slot_index)
         DO UPDATE SET title = EXCLUDED.title, description = EXCLUDED.description
         RETURNING id",
    )
    .bind(account_id)
    .bind(slot)
    .bind(title)
    .bind(description)
    .fetch_one(pool)
    .await?;
    Ok(PublishOutcome::Published(id))
}

pub async fn unpublish(pool: &PgPool, account_id: Uuid, slot: i16) -> Result<bool, sqlx::Error> {
    let r = sqlx::query(
        "DELETE FROM showcase_listings WHERE account_id = $1 AND slot_index = $2 AND NOT hidden",
    )
    .bind(account_id)
    .bind(slot)
    .execute(pool)
    .await?;
    Ok(r.rows_affected() == 1)
}

/// Returns up to PAGE_SIZE + 1 rows; the caller uses the extra one for `has_more`.
pub async fn page(
    pool: &PgPool,
    sort: Sort,
    page: i64,
    author: Option<Uuid>,
) -> Result<Vec<ListingRow>, sqlx::Error> {
    // The dynamic parts are an enum-derived ORDER BY and a fixed author filter
    // constant; all values stay bound parameters, so AssertSqlSafe is honest.
    let order = match sort {
        Sort::New => "l.published_at DESC",
        Sort::Popular => "l.copies_count DESC, l.published_at DESC",
    };
    let author_filter = if author.is_some() {
        "AND l.account_id = $3"
    } else {
        ""
    };
    let sql = format!(
        "SELECT {LISTING_COLS}{LISTING_FROM} WHERE NOT l.hidden {author_filter} ORDER BY {order}, l.id \
         LIMIT $1 OFFSET $2"
    );
    let mut query: QueryAs<'_, Postgres, ListingRow, _> =
        query_as::<Postgres, ListingRow>(sqlx::AssertSqlSafe(sql))
            .bind(PAGE_SIZE + 1)
            .bind(page * PAGE_SIZE);
    if let Some(id) = author {
        query = query.bind(id);
    }
    query.fetch_all(pool).await
}

pub async fn detail(
    pool: &PgPool,
    id: Uuid,
) -> Result<Option<(ListingRow, serde_json::Value)>, sqlx::Error> {
    let Some((row, data)) = sqlx::query_as::<_, (Uuid, Uuid, String, String, i32, DateTime<Utc>, String, serde_json::Value)>(
        "SELECT l.id, l.account_id, l.title, l.description, l.copies_count, l.published_at, s.name, s.data
         FROM showcase_listings l
         JOIN config_slots s ON s.account_id = l.account_id AND s.slot_index = l.slot_index
         WHERE l.id = $1 AND NOT l.hidden",
    )
    .bind(id)
    .fetch_optional(pool)
    .await?
    .map(|(id, account_id, title, description, copies_count, published_at, name, data)| {
        (
            ListingRow {
                id,
                account_id,
                title,
                description,
                copies_count,
                published_at,
                name,
            },
            data,
        )
    })
    else {
        return Ok(None);
    };
    Ok(Some((row, data)))
}

pub enum CopyOutcome {
    Copied,
    ListingMissing,
    SlotOccupied,
}

pub async fn copy_into(
    pool: &PgPool,
    listing: Uuid,
    account_id: Uuid,
    slot: i16,
) -> Result<CopyOutcome, sqlx::Error> {
    let mut tx = pool.begin().await?;
    let Some((owner, name, data)): Option<(Uuid, String, serde_json::Value)> = sqlx::query_as(
        "SELECT l.account_id, s.name, s.data FROM showcase_listings l JOIN config_slots s
         ON s.account_id = l.account_id AND s.slot_index = l.slot_index
         WHERE l.id = $1 AND NOT l.hidden",
    )
    .bind(listing)
    .fetch_optional(&mut *tx)
    .await?
    else {
        return Ok(CopyOutcome::ListingMissing);
    };
    // Share codes are globally unique; retry a handful of times on collision
    // like `save` does, rather than failing the whole copy.
    let mut attempts = 0;
    let inserted_rows = loop {
        let result = sqlx::query(
            "INSERT INTO config_slots (account_id, slot_index, name, data, share_code)
             VALUES ($1, $2, $3, $4, $5)
             ON CONFLICT (account_id, slot_index) DO NOTHING",
        )
        .bind(account_id)
        .bind(slot)
        .bind(&name)
        .bind(&data)
        .bind(crate::sharing::codes::new_code())
        .execute(&mut *tx)
        .await;
        match result {
            Err(err) if crate::slots::repo::is_share_code_collision(&err) && attempts < 3 => {
                attempts += 1;
            }
            other => break other?.rows_affected(),
        }
    };
    if inserted_rows == 0 {
        return Ok(CopyOutcome::SlotOccupied);
    }
    if owner != account_id {
        sqlx::query("UPDATE showcase_listings SET copies_count = copies_count + 1 WHERE id = $1")
            .bind(listing)
            .execute(&mut *tx)
            .await?;
    }
    tx.commit().await?;
    Ok(CopyOutcome::Copied)
}
