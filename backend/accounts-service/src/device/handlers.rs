use crate::device::{
    links,
    store::{self, DeviceStore, PollResult},
};
use crate::error::{AppError, AppJson};
use axum::{
    Json,
    extract::{Path, State},
    http::StatusCode,
};
use common::internal::GatewayIdentity;
use serde::{Deserialize, Serialize};

#[derive(Clone)]
pub struct DeviceState {
    pub store: DeviceStore,
    pub pool: sqlx::PgPool,
}

#[derive(Serialize)]
pub struct DeviceCodeResponse {
    pub device_code: String,
    pub user_code: String,
    pub expires_in: u64,
}

pub async fn create_code(
    State(state): State<DeviceState>,
) -> Result<(StatusCode, Json<DeviceCodeResponse>), AppError> {
    let (device_code, user_code) = state.store.create().ok_or(AppError::TooManyRequests)?;
    Ok((
        StatusCode::CREATED,
        Json(DeviceCodeResponse {
            device_code,
            user_code,
            expires_in: store::TTL.as_secs(),
        }),
    ))
}

#[derive(Deserialize)]
pub struct ConfirmRequest {
    pub user_code: String,
}

pub async fn confirm(
    State(state): State<DeviceState>,
    identity: GatewayIdentity,
    AppJson(req): AppJson<ConfirmRequest>,
) -> Result<StatusCode, AppError> {
    if state.store.confirm(&req.user_code, identity.account_id) {
        Ok(StatusCode::OK)
    } else {
        Err(AppError::NotFound("unknown or expired user_code".into()))
    }
}

#[derive(Deserialize)]
pub struct TokenRequest {
    pub device_code: String,
}

#[derive(Serialize)]
pub struct TokenResponse {
    pub device_token: String,
}

pub async fn token(
    State(state): State<DeviceState>,
    AppJson(req): AppJson<TokenRequest>,
) -> Result<(StatusCode, Json<Option<TokenResponse>>), AppError> {
    match state.store.poll(&req.device_code) {
        PollResult::Unknown => Err(AppError::NotFound("unknown or expired device_code".into())),
        PollResult::Pending => Ok((StatusCode::ACCEPTED, Json(None))),
        PollResult::Confirmed(account_id) => {
            // The long-lived device token is an opaque random secret, stored
            // hashed in device_links so the gateway can revoke it per device.
            let device_token = links::create(&state.pool, account_id).await?;
            Ok((StatusCode::OK, Json(Some(TokenResponse { device_token }))))
        }
    }
}

pub async fn list_links(
    State(state): State<DeviceState>,
    identity: GatewayIdentity,
) -> Result<Json<Vec<links::DeviceLink>>, AppError> {
    Ok(Json(links::list(&state.pool, identity.account_id).await?))
}

pub async fn revoke_link(
    State(state): State<DeviceState>,
    identity: GatewayIdentity,
    Path(link_id): Path<uuid::Uuid>,
) -> Result<StatusCode, AppError> {
    if links::revoke(&state.pool, identity.account_id, link_id).await? {
        Ok(StatusCode::NO_CONTENT)
    } else {
        Err(AppError::NotFound("no such device link".into()))
    }
}
