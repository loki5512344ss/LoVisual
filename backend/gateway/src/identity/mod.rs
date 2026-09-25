pub mod device;

use axum::{
    Json,
    extract::{Request, State},
    http::{HeaderValue, StatusCode, header::AUTHORIZATION},
    middleware::Next,
    response::{IntoResponse, Response},
};
use common::internal::{ACCOUNT_ID_HEADER, DEVICE_TOKEN_PREFIX, INTERNAL_KEY_HEADER};
use common::jwt::{TokenType, bearer_token, verify_token};
use device::{DeviceAuth, DeviceAuthenticator};
use serde_json::json;
use std::sync::Arc;
use uuid::Uuid;

/// Who made the request, as far as the gateway could verify.
#[derive(Clone, Copy)]
pub struct Identity(pub Option<Uuid>);

/// How the caller's credentials resolved. Recorded by `identify`, acted on by
/// `authorize` — resolution must not reject on its own, because it sits before
/// rate limiting and an unknown device token already cost a gRPC round trip.
#[derive(Clone, Copy, PartialEq, Eq)]
pub enum Credentials {
    /// No `Authorization` header at all.
    Anonymous,
    /// Verified; `Identity` carries the account.
    Valid,
    /// Present but not verifiable (expired/garbage/unknown device token).
    Invalid,
    /// Device lookup failed; the caller is neither allowed nor blamed.
    Unavailable,
}

#[derive(Clone)]
pub struct IdentityState {
    pub jwt_secret: Arc<str>,
    pub devices: Arc<dyn DeviceAuthenticator>,
}

fn reject(status: StatusCode, message: &str) -> Response {
    (status, Json(json!({ "error": message }))).into_response()
}

/// Public auth endpoints: stale credentials there must not lock the caller
/// out of logout/refresh — they are treated as anonymous instead of 401.
fn is_auth_path(path: &str) -> bool {
    path.starts_with("/auth/")
}

/// Resolves the caller into `Identity` + `Credentials` extensions, never
/// rejecting; the gate is `authorize`.
pub async fn identify(
    State(state): State<IdentityState>,
    mut req: Request,
    next: Next,
) -> Response {
    // Client-supplied copies of trusted headers are never forwarded.
    req.headers_mut().remove(ACCOUNT_ID_HEADER);
    req.headers_mut().remove(INTERNAL_KEY_HEADER);

    let (account, credentials) = match bearer_token(req.headers()) {
        None => (None, Credentials::Anonymous),
        Some(token) if token.starts_with(DEVICE_TOKEN_PREFIX) => {
            match state.devices.authenticate(token).await {
                DeviceAuth::Valid(id) => (Some(id), Credentials::Valid),
                DeviceAuth::Invalid => (None, Credentials::Invalid),
                DeviceAuth::Unavailable => (None, Credentials::Unavailable),
            }
        }
        Some(token) => {
            let id = verify_token(token, &state.jwt_secret, TokenType::Access)
                .and_then(|claims| Uuid::parse_str(&claims.sub).ok());
            match id {
                Some(id) => (Some(id), Credentials::Valid),
                None => (None, Credentials::Invalid),
            }
        }
    };

    // The client's bearer token stops here either way.
    req.headers_mut().remove(AUTHORIZATION);
    if let Some(id) = account
        && let Ok(value) = HeaderValue::from_str(&id.to_string())
    {
        req.headers_mut().insert(ACCOUNT_ID_HEADER, value);
    }
    req.extensions_mut().insert(Identity(account));
    req.extensions_mut().insert(credentials);
    next.run(req).await
}

/// Turns a failed resolution into an HTTP rejection — positioned after rate
/// limiting so that a rejected credential still costs the caller a token.
pub async fn authorize(req: Request, next: Next) -> Response {
    let credentials = req
        .extensions()
        .get::<Credentials>()
        .copied()
        .unwrap_or(Credentials::Anonymous);
    let allowed = credentials == Credentials::Anonymous
        || credentials == Credentials::Valid
        || is_auth_path(req.uri().path());
    if !allowed {
        return match credentials {
            Credentials::Unavailable => {
                reject(StatusCode::SERVICE_UNAVAILABLE, "auth backend unavailable")
            }
            _ => reject(StatusCode::UNAUTHORIZED, "unauthorized"),
        };
    }
    next.run(req).await
}
