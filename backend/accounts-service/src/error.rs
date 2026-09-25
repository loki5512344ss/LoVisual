use axum::{
    Json,
    extract::{FromRequest, rejection::JsonRejection},
    http::StatusCode,
    response::{IntoResponse, Response},
};
use serde_json::json;

#[derive(Debug)]
pub enum AppError {
    Validation(String),
    Conflict(String),
    Unauthorized,
    NotFound(String),
    TooManyRequests,
    Internal(anyhow::Error),
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let (status, message) = match self {
            AppError::Validation(msg) => (StatusCode::BAD_REQUEST, msg),
            AppError::Conflict(msg) => (StatusCode::CONFLICT, msg),
            AppError::Unauthorized => (StatusCode::UNAUTHORIZED, "unauthorized".into()),
            AppError::NotFound(msg) => (StatusCode::NOT_FOUND, msg),
            AppError::TooManyRequests => (
                StatusCode::TOO_MANY_REQUESTS,
                "too many pending device codes".into(),
            ),
            AppError::Internal(err) => {
                tracing::error!("internal error: {err:?}");
                (StatusCode::INTERNAL_SERVER_ERROR, "internal error".into())
            }
        };
        (status, Json(json!({ "error": message }))).into_response()
    }
}

impl From<sqlx::Error> for AppError {
    fn from(err: sqlx::Error) -> Self {
        if let sqlx::Error::Database(ref db_err) = err
            && db_err.constraint() == Some("accounts_email_lower_idx")
        {
            return AppError::Conflict("email already registered".into());
        }
        AppError::Internal(err.into())
    }
}

impl From<JsonRejection> for AppError {
    fn from(rejection: JsonRejection) -> Self {
        AppError::Validation(rejection.body_text())
    }
}

/// Drop-in replacement for `axum::Json` that reports malformed/invalid JSON
/// bodies as our `{"error": ...}` shape instead of axum's plain-text default.
#[derive(FromRequest)]
#[from_request(via(axum::Json), rejection(AppError))]
pub struct AppJson<T>(pub T);

impl<T> IntoResponse for AppJson<T>
where
    Json<T>: IntoResponse,
{
    fn into_response(self) -> Response {
        Json(self.0).into_response()
    }
}
