use super::repo;
use crate::error::AppError;
use axum::{
    Json,
    extract::{Path, State},
};
use chrono::{DateTime, Utc};
use common::internal::GatewayIdentity;
use serde::Serialize;
use uuid::Uuid;

#[derive(Clone)]
pub struct AccountsState {
    pub pool: sqlx::PgPool,
    pub avatar_base_url: String,
}

#[derive(Serialize)]
pub struct MeResponse {
    pub id: Uuid,
    pub email: String,
    pub display_nick: String,
    pub role: String,
    pub avatar_url: Option<String>,
    pub created_at: DateTime<Utc>,
}

pub async fn me(
    State(state): State<AccountsState>,
    identity: GatewayIdentity,
) -> Result<Json<MeResponse>, AppError> {
    let account = repo::find_by_id(&state.pool, identity.account_id)
        .await?
        .ok_or(AppError::Unauthorized)?;
    let avatar_url = repo::avatar_key(&state.pool, account.id)
        .await?
        .map(|key| format!("{}/{key}", state.avatar_base_url));
    Ok(Json(MeResponse {
        id: account.id,
        email: account.email,
        display_nick: account.display_nick,
        role: account.role,
        avatar_url,
        created_at: account.created_at,
    }))
}

/// Public profile for the site's `/u/:id` page: no identity required, and no
/// private fields (email, role) — only what showcase viewers may see.
#[derive(Serialize)]
pub struct PublicProfileResponse {
    pub id: Uuid,
    pub display_nick: String,
    pub avatar_url: Option<String>,
    pub created_at: DateTime<Utc>,
    pub badges: Vec<String>,
}

/// Accounts are handed out in `created_at` order, so the first 1000 to sign
/// up get the `early` badge.
const EARLY_ADOPTER_LIMIT: i64 = 1000;

pub async fn public_profile(
    State(state): State<AccountsState>,
    Path(raw): Path<String>,
) -> Result<Json<PublicProfileResponse>, AppError> {
    let not_found = || AppError::NotFound("no such account".into());
    let id = Uuid::parse_str(&raw).map_err(|_| not_found())?;
    let account = repo::find_by_id(&state.pool, id)
        .await?
        .ok_or_else(not_found)?;
    let avatar_url = repo::avatar_key(&state.pool, account.id)
        .await?
        .map(|key| format!("{}/{key}", state.avatar_base_url));
    let rank = repo::account_rank(&state.pool, account.created_at).await?;
    let badges = if rank < EARLY_ADOPTER_LIMIT {
        vec!["early".to_owned()]
    } else {
        Vec::new()
    };
    Ok(Json(PublicProfileResponse {
        id: account.id,
        display_nick: account.display_nick,
        avatar_url,
        created_at: account.created_at,
        badges,
    }))
}
