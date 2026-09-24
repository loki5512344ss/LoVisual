//! Contract between the gateway and internal services. Services trust the
//! identity header ONLY because `require_internal_key` guarantees the request
//! came through the gateway (which strips client-supplied copies of both).

use axum::{
    extract::{FromRequestParts, Request, State},
    http::{request::Parts, StatusCode},
    middleware::Next,
    response::{IntoResponse, Response},
    Json,
};
use serde_json::json;
use std::sync::Arc;
use subtle::ConstantTimeEq;
use tonic::{
    metadata::{Ascii, MetadataValue},
    service::Interceptor,
    Status,
};
use uuid::Uuid;

pub const INTERNAL_KEY_HEADER: &str = "x-lovisual-internal-key";
pub const ACCOUNT_ID_HEADER: &str = "x-lovisual-account-id";
pub const DEVICE_TOKEN_PREFIX: &str = "lvd_";

fn keys_match(given: &[u8], expected: &[u8]) -> bool {
    given.ct_eq(expected).into()
}

#[derive(Clone)]
pub struct InternalKey(Arc<str>);

impl InternalKey {
    pub fn new(key: String) -> Self {
        InternalKey(key.into())
    }
}

pub async fn require_internal_key(State(key): State<InternalKey>, req: Request, next: Next) -> Response {
    let ok = req
        .headers()
        .get(INTERNAL_KEY_HEADER)
        .is_some_and(|v| keys_match(v.as_bytes(), key.0.as_bytes()));
    if !ok {
        return (StatusCode::FORBIDDEN, Json(json!({ "error": "forbidden" }))).into_response();
    }
    next.run(req).await
}

/// The authenticated account, as resolved by the gateway. Rejects with 401
/// when the gateway forwarded the request anonymously.
pub struct GatewayIdentity {
    pub account_id: Uuid,
}

impl<S: Send + Sync> FromRequestParts<S> for GatewayIdentity {
    type Rejection = Response;

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        parts
            .headers
            .get(ACCOUNT_ID_HEADER)
            .and_then(|v| v.to_str().ok())
            .and_then(|s| Uuid::parse_str(s).ok())
            .map(|account_id| GatewayIdentity { account_id })
            .ok_or_else(|| {
                (StatusCode::UNAUTHORIZED, Json(json!({ "error": "unauthorized" }))).into_response()
            })
    }
}

/// Server-side tonic interceptor: rejects calls without the internal key.
#[derive(Clone)]
pub struct GrpcKeyCheck(Arc<str>);

impl GrpcKeyCheck {
    pub fn new(key: &str) -> Self {
        GrpcKeyCheck(key.into())
    }
}

impl Interceptor for GrpcKeyCheck {
    fn call(&mut self, req: tonic::Request<()>) -> Result<tonic::Request<()>, Status> {
        match req.metadata().get(INTERNAL_KEY_HEADER) {
            Some(v) if keys_match(v.as_bytes(), self.0.as_bytes()) => Ok(req),
            _ => Err(Status::permission_denied("missing or invalid internal key")),
        }
    }
}

/// Client-side tonic interceptor: attaches the internal key to every call.
#[derive(Clone)]
pub struct GrpcKeyAttach(MetadataValue<Ascii>);

impl GrpcKeyAttach {
    pub fn new(key: &str) -> Result<Self, tonic::metadata::errors::InvalidMetadataValue> {
        Ok(GrpcKeyAttach(MetadataValue::try_from(key)?))
    }
}

impl Interceptor for GrpcKeyAttach {
    fn call(&mut self, mut req: tonic::Request<()>) -> Result<tonic::Request<()>, Status> {
        req.metadata_mut().insert(INTERNAL_KEY_HEADER, self.0.clone());
        Ok(req)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use axum::{routing::get, Router};
    use axum_test::TestServer;

    const KEY: &str = "internal-key-internal-key-internal!!";

    fn app() -> Router {
        Router::new()
            .route("/whoami", get(|id: GatewayIdentity| async move { id.account_id.to_string() }))
            .route("/open", get(|| async { "open" }))
            .layer(axum::middleware::from_fn_with_state(InternalKey::new(KEY.into()), require_internal_key))
    }

    #[tokio::test]
    async fn request_without_internal_key_is_forbidden() {
        let server = TestServer::new(app());
        server.get("/open").await.assert_status(axum::http::StatusCode::FORBIDDEN);
    }

    #[tokio::test]
    async fn request_with_wrong_internal_key_is_forbidden() {
        let server = TestServer::new(app());
        server
            .get("/open")
            .add_header(INTERNAL_KEY_HEADER, "nope")
            .await
            .assert_status(axum::http::StatusCode::FORBIDDEN);
    }

    #[tokio::test]
    async fn correct_key_passes_and_identity_is_read() {
        let server = TestServer::new(app());
        let id = Uuid::new_v4();
        let res = server
            .get("/whoami")
            .add_header(INTERNAL_KEY_HEADER, KEY)
            .add_header(ACCOUNT_ID_HEADER, id.to_string())
            .await;
        res.assert_status_ok();
        assert_eq!(res.text(), id.to_string());
    }

    #[tokio::test]
    async fn missing_or_garbage_identity_is_401() {
        let server = TestServer::new(app());
        server
            .get("/whoami")
            .add_header(INTERNAL_KEY_HEADER, KEY)
            .await
            .assert_status_unauthorized();
        server
            .get("/whoami")
            .add_header(INTERNAL_KEY_HEADER, KEY)
            .add_header(ACCOUNT_ID_HEADER, "not-a-uuid")
            .await
            .assert_status_unauthorized();
    }

    #[test]
    fn grpc_key_check_accepts_only_the_right_key() {
        let mut check = GrpcKeyCheck::new(KEY);
        let mut attach = GrpcKeyAttach::new(KEY).unwrap();
        let ok = attach.call(tonic::Request::new(())).unwrap();
        assert!(check.call(ok).is_ok());
        assert!(check.call(tonic::Request::new(())).is_err());
    }
}
