use crate::accounts::repo;
use crate::avatars::processing::{AvatarImageError, process_avatar};
use crate::avatars::storage::S3Storage;
use crate::error::AppError;
use axum::{
    Json,
    extract::{Multipart, State},
};
use common::internal::GatewayIdentity;
use serde::Serialize;

pub const MAX_UPLOAD_BYTES: usize = 5 * 1024 * 1024;

#[derive(Clone)]
pub struct AvatarState {
    pub pool: sqlx::PgPool,
    pub storage: S3Storage,
    pub base_url: String,
}

#[derive(Serialize)]
pub struct AvatarResponse {
    pub avatar_url: String,
}

pub async fn upload(
    State(state): State<AvatarState>,
    identity: GatewayIdentity,
    mut multipart: Multipart,
) -> Result<Json<AvatarResponse>, AppError> {
    let account_id = identity.account_id;

    let field = multipart
        .next_field()
        .await
        .map_err(|_| AppError::Validation("malformed multipart body".into()))?
        .filter(|f| f.name() == Some("file"))
        .ok_or_else(|| AppError::Validation("expected a multipart part named `file`".into()))?;
    let bytes = field
        .bytes()
        .await
        .map_err(|_| AppError::Validation("could not read the uploaded file".into()))?;
    if bytes.len() > MAX_UPLOAD_BYTES {
        return Err(AppError::Validation("file too large (max 5MB)".into()));
    }

    // Decoding is CPU-bound and the input is untrusted: keep it off the async workers.
    let png = tokio::task::spawn_blocking(move || process_avatar(&bytes))
        .await
        .map_err(|e| AppError::Internal(e.into()))?
        .map_err(|e| match e {
            AvatarImageError::Unsupported => {
                AppError::Validation("unsupported image format (png, jpeg, webp)".into())
            }
            AvatarImageError::Invalid => AppError::Validation("invalid or too large image".into()),
        })?;

    let key = format!("avatars/{account_id}.png");
    state
        .storage
        .put(&key, png, "image/png")
        .await
        .map_err(AppError::Internal)?;
    repo::set_avatar(&state.pool, account_id, &key).await?;

    Ok(Json(AvatarResponse {
        avatar_url: format!("{}/{key}", state.base_url),
    }))
}
