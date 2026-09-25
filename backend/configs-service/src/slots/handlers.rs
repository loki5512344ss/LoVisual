use super::repo::{self, Slot, SlotSummary};
use crate::error::{AppError, AppJson, AppPath};
use axum::{Json, extract::State};
use common::internal::GatewayIdentity;
use serde::Deserialize;

pub const MAX_DATA_BYTES: usize = 256 * 1024;
const MAX_NAME_CHARS: usize = 32;

#[derive(Clone)]
pub struct SlotsState {
    pub pool: sqlx::PgPool,
}

pub fn validate_slot(slot: i16) -> Result<i16, AppError> {
    if (1..=4).contains(&slot) {
        Ok(slot)
    } else {
        Err(AppError::Validation("slot must be 1..4".into()))
    }
}

/// Rejects any ASCII/Unicode control character (including a bare CR/LF), which
/// would otherwise corrupt log lines or list rendering downstream.
pub fn has_control_chars(s: &str) -> bool {
    s.chars().any(|c| c.is_control())
}

pub fn validate_name(name: &str) -> Result<String, AppError> {
    let name = name.trim();
    let len = name.chars().count();
    if len == 0 || len > MAX_NAME_CHARS {
        return Err(AppError::Validation(format!(
            "name must be 1..{MAX_NAME_CHARS} characters"
        )));
    }
    if has_control_chars(name) {
        return Err(AppError::Validation(
            "name must not contain control characters".into(),
        ));
    }
    Ok(name.to_owned())
}

#[derive(Deserialize)]
pub struct SaveRequest {
    pub name: String,
    pub data: serde_json::Value,
}

pub async fn list(
    State(state): State<SlotsState>,
    id: GatewayIdentity,
) -> Result<Json<Vec<SlotSummary>>, AppError> {
    Ok(Json(repo::list(&state.pool, id.account_id).await?))
}

pub async fn get(
    State(state): State<SlotsState>,
    id: GatewayIdentity,
    AppPath(slot): AppPath<i16>,
) -> Result<Json<Slot>, AppError> {
    let slot = validate_slot(slot)?;
    repo::get(&state.pool, id.account_id, slot)
        .await?
        .map(Json)
        .ok_or_else(|| AppError::NotFound("slot is empty".into()))
}

pub async fn save(
    State(state): State<SlotsState>,
    id: GatewayIdentity,
    AppPath(slot): AppPath<i16>,
    AppJson(req): AppJson<SaveRequest>,
) -> Result<Json<Slot>, AppError> {
    let slot = validate_slot(slot)?;
    let name = validate_name(&req.name)?;
    let size = serde_json::to_vec(&req.data)
        .map_err(|e| AppError::Internal(e.into()))?
        .len();
    if size > MAX_DATA_BYTES {
        return Err(AppError::Validation(format!(
            "config data must be at most {MAX_DATA_BYTES} bytes"
        )));
    }
    Ok(Json(
        repo::save(&state.pool, id.account_id, slot, &name, &req.data).await?,
    ))
}
