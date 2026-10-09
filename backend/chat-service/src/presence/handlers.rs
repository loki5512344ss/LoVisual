use super::{
    payload::{MAX_WANT, normalize_server, valid_payload},
    store::{PublishError, Store},
};
use axum::{
    Json,
    extract::State,
    http::StatusCode,
    response::{IntoResponse, Response},
};
use common::internal::GatewayIdentity;
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::{sync::Arc, time::Instant};
use uuid::Uuid;

#[derive(Deserialize)]
pub struct SyncRequest {
    pub server: String,
    pub mc: Uuid,
    /// Base64 GUI payload, or null when this client has no menu open (still
    /// marks the player as a LoVisual user for a few seconds).
    #[serde(default)]
    pub gui: Option<String>,
    /// Minecraft uuids of nearby players the caller wants state for.
    #[serde(default)]
    pub want: Vec<Uuid>,
    /// False for a read-only poll: the caller sees others but does not appear itself.
    #[serde(default = "yes")]
    pub publish: bool,
}

fn yes() -> bool {
    true
}

#[derive(Serialize)]
pub struct PlayerState {
    pub mc: Uuid,
    pub gui: Option<String>,
    pub age: u64,
}

fn error(status: StatusCode, message: &str) -> Response {
    (status, Json(json!({ "error": message }))).into_response()
}

/// One round trip both publishes the caller's state and returns the state of
/// the players the caller can see (same request as the mod's poll loop).
pub async fn sync(
    State(store): State<Arc<Store>>,
    id: GatewayIdentity,
    Json(req): Json<SyncRequest>,
) -> Response {
    let Some(server) = normalize_server(&req.server) else {
        return error(StatusCode::BAD_REQUEST, "invalid server");
    };
    if req.want.len() > MAX_WANT {
        return error(StatusCode::BAD_REQUEST, "too many players requested");
    }
    if req.gui.as_deref().is_some_and(|g| !valid_payload(g)) {
        return error(StatusCode::BAD_REQUEST, "invalid gui payload");
    }
    let now = Instant::now();
    if req.publish
        && let Err(PublishError::Claimed) =
            store.publish(id.account_id, &server, req.mc, req.gui, now)
    {
        return error(
            StatusCode::CONFLICT,
            "player uuid is bound to another account",
        );
    }
    let players: Vec<PlayerState> = store
        .lookup(&server, &req.want, now)
        .into_iter()
        .filter(|s| s.mc != req.mc)
        .map(|s| PlayerState {
            mc: s.mc,
            gui: s.gui,
            age: s.age_ms,
        })
        .collect();
    Json(json!({ "players": players })).into_response()
}
