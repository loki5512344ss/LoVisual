use crate::device::store::{self, DeviceStore, PollResult};
use crate::error::{AppError, AppJson};
use axum::{extract::State, http::StatusCode, Json};
use common::internal::GatewayIdentity;
use common::jwt;
use serde::{Deserialize, Serialize};

#[derive(Clone)]
pub struct DeviceState {
    pub store: DeviceStore,
    pub jwt_secret: String,
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
        Json(DeviceCodeResponse { device_code, user_code, expires_in: store::TTL.as_secs() }),
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
            // The long-lived device_token is just a refresh-style JWT for now;
            // the gateway plan is where per-device revocation via
            // device_links.device_token_hash gets enforced on every request.
            let device_token = jwt::issue_refresh_token(account_id, &state.jwt_secret);
            Ok((StatusCode::OK, Json(Some(TokenResponse { device_token }))))
        }
    }
}
