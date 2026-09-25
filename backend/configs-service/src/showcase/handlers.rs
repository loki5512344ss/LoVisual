use super::profiles::{Author, ProfileSource};
use super::repo::{self, CopyOutcome, ListingRow, PAGE_SIZE, PublishOutcome, Sort};
use crate::error::{AppError, AppJson, AppPath, AppQuery};
use crate::slots::handlers::{has_control_chars, validate_slot};
use axum::{Json, extract::State, http::StatusCode};
use chrono::{DateTime, Utc};
use common::internal::GatewayIdentity;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use uuid::Uuid;

#[derive(Clone)]
pub struct ShowcaseState {
    pub pool: sqlx::PgPool,
    pub profiles: Arc<dyn ProfileSource>,
}

#[derive(Serialize)]
pub struct Listing {
    pub id: Uuid,
    pub title: String,
    pub description: String,
    pub config_name: String,
    pub copies_count: i32,
    pub published_at: DateTime<Utc>,
    pub author: Option<Author>,
}

fn to_listing(row: ListingRow, author: Option<Author>) -> Listing {
    Listing {
        id: row.id,
        title: row.title,
        description: row.description,
        config_name: row.name,
        copies_count: row.copies_count,
        published_at: row.published_at,
        author,
    }
}

#[derive(Deserialize)]
pub struct PublishRequest {
    pub title: String,
    #[serde(default)]
    pub description: String,
}

pub async fn publish(
    State(state): State<ShowcaseState>,
    id: GatewayIdentity,
    AppPath(slot): AppPath<i16>,
    AppJson(req): AppJson<PublishRequest>,
) -> Result<Json<serde_json::Value>, AppError> {
    let slot = validate_slot(slot)?;
    let title = req.title.trim();
    if title.is_empty() || title.chars().count() > 64 || has_control_chars(title) {
        return Err(AppError::Validation(
            "title must be 1..64 characters, no control characters".into(),
        ));
    }
    let description = req.description.trim().to_owned();
    if description.chars().count() > 500 || has_control_chars(&description) {
        return Err(AppError::Validation(
            "description must be at most 500 characters, no control characters".into(),
        ));
    }
    match repo::publish(&state.pool, id.account_id, slot, title, &description).await? {
        PublishOutcome::Published(listing) => {
            Ok(Json(serde_json::json!({ "listing_id": listing })))
        }
        PublishOutcome::SlotEmpty => Err(AppError::NotFound("slot is empty".into())),
        PublishOutcome::Hidden => Err(AppError::Forbidden(
            "listing is hidden by moderation".into(),
        )),
    }
}

pub async fn unpublish(
    State(state): State<ShowcaseState>,
    id: GatewayIdentity,
    AppPath(slot): AppPath<i16>,
) -> Result<StatusCode, AppError> {
    let slot = validate_slot(slot)?;
    if repo::unpublish(&state.pool, id.account_id, slot).await? {
        Ok(StatusCode::NO_CONTENT)
    } else {
        Err(AppError::NotFound("slot is not published".into()))
    }
}

#[derive(Deserialize)]
pub struct PageQuery {
    pub sort: Option<String>,
    pub page: Option<i64>,
    pub author: Option<Uuid>,
}

#[derive(Serialize)]
pub struct PageResponse {
    pub items: Vec<Listing>,
    pub page: i64,
    pub has_more: bool,
}

pub async fn browse(
    State(state): State<ShowcaseState>,
    AppQuery(q): AppQuery<PageQuery>,
) -> Result<Json<PageResponse>, AppError> {
    let sort = match q.sort.as_deref() {
        None | Some("new") => Sort::New,
        Some("popular") => Sort::Popular,
        Some(_) => return Err(AppError::Validation("sort must be new or popular".into())),
    };
    let page = q.page.unwrap_or(0).clamp(0, 10_000);
    let mut rows = repo::page(&state.pool, sort, page, q.author).await?;
    let has_more = rows.len() as i64 > PAGE_SIZE;
    rows.truncate(PAGE_SIZE as usize);
    let ids: Vec<Uuid> = rows.iter().map(|r| r.account_id).collect();
    let authors = state.profiles.profiles(&ids).await;
    let items = rows
        .into_iter()
        .map(|r| {
            let author = authors.get(&r.account_id).cloned();
            to_listing(r, author)
        })
        .collect();
    Ok(Json(PageResponse {
        items,
        page,
        has_more,
    }))
}

pub async fn detail(
    State(state): State<ShowcaseState>,
    AppPath(id): AppPath<Uuid>,
) -> Result<Json<serde_json::Value>, AppError> {
    let (row, data) = repo::detail(&state.pool, id)
        .await?
        .ok_or_else(|| AppError::NotFound("no such listing".into()))?;
    let author = state
        .profiles
        .profiles(&[row.account_id])
        .await
        .remove(&row.account_id);
    let mut body =
        serde_json::to_value(to_listing(row, author)).map_err(|e| AppError::Internal(e.into()))?;
    body["data"] = data;
    Ok(Json(body))
}

#[derive(Deserialize)]
pub struct CopyRequest {
    pub slot: i16,
}

pub async fn copy(
    State(state): State<ShowcaseState>,
    id: GatewayIdentity,
    AppPath(listing): AppPath<Uuid>,
    AppJson(req): AppJson<CopyRequest>,
) -> Result<(StatusCode, Json<crate::slots::repo::Slot>), AppError> {
    let slot = validate_slot(req.slot)?;
    match repo::copy_into(&state.pool, listing, id.account_id, slot).await? {
        CopyOutcome::ListingMissing => Err(AppError::NotFound("no such listing".into())),
        CopyOutcome::SlotOccupied => Err(AppError::Conflict("target slot is not empty".into())),
        CopyOutcome::Copied => {
            let saved = crate::slots::repo::get(&state.pool, id.account_id, slot)
                .await?
                .ok_or_else(|| AppError::Internal(anyhow::anyhow!("copied slot vanished")))?;
            Ok((StatusCode::CREATED, Json(saved)))
        }
    }
}
