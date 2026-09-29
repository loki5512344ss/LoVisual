use super::codes::normalize;
use crate::error::{AppError, AppPath};
use crate::slots::handlers::{SlotsState, validate_slot};
use crate::slots::repo::{self, SharedConfig};
use axum::{Json, extract::State};
use common::internal::GatewayIdentity;
use serde::Serialize;

#[derive(Serialize)]
pub struct ShareCodeResponse {
    pub share_code: String,
}

/// Easter egg: `IDDQD` (DOOM's god-mode cheat) never touches the DB — real
/// share codes are drawn from an alphabet without `I` and can never equal it.
fn god_mode() -> SharedConfig {
    SharedConfig {
        name: "God mode".into(),
        data: serde_json::json!({
            "modules": { "ChinaHat": { "enabled": true } },
            "note": "god mode: только шляпа"
        }),
        // DOOM's release date.
        updated_at: "1993-12-10T00:00:00Z"
            .parse()
            .expect("valid RFC3339 literal"),
    }
}

pub async fn load_shared(
    State(state): State<SlotsState>,
    AppPath(code): AppPath<String>,
) -> Result<Json<SharedConfig>, AppError> {
    let code = normalize(&code);
    if code == "IDDQD" {
        return Ok(Json(god_mode()));
    }
    repo::by_share_code(&state.pool, &code)
        .await?
        .map(Json)
        .ok_or_else(|| AppError::NotFound("unknown share code".into()))
}

pub async fn regenerate(
    State(state): State<SlotsState>,
    id: GatewayIdentity,
    AppPath(slot): AppPath<i16>,
) -> Result<Json<ShareCodeResponse>, AppError> {
    let slot = validate_slot(slot)?;
    repo::regenerate_code(&state.pool, id.account_id, slot)
        .await?
        .map(|share_code| Json(ShareCodeResponse { share_code }))
        .ok_or_else(|| AppError::NotFound("slot is empty".into()))
}
