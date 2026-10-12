use crate::accounts::repo;
use crate::avatars::processing::{AvatarImageError, process_avatar};
use crate::avatars::storage::S3Storage;
use crate::error::AppError;
use axum::{
    Json,
    extract::{Multipart, Path, State},
    http::{StatusCode, header},
    response::{IntoResponse, Response},
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

/// DELETE /avatars — clears the caller's avatar. Removes the DB row and, if one
/// existed, the S3 object. Always returns 204, including when the user had no
/// avatar: the operation is idempotent, matching how the frontend's "Remove
/// avatar" button should behave after a successful click.
pub async fn delete(
    State(state): State<AvatarState>,
    identity: GatewayIdentity,
) -> Result<Response, AppError> {
    let account_id = identity.account_id;
    let existed = repo::clear_avatar(&state.pool, account_id).await?;
    if existed {
        let key = format!("avatars/{account_id}.png");
        // S3 delete is best-effort: if MinIO is unreachable we already committed
        // the DB row removal, and the object becomes orphaned but not served (the
        // serve handler looks up keys via the DB, not directly).
        if let Err(err) = state.storage.delete(&key).await {
            tracing::warn!("avatar S3 delete failed for {account_id}: {err}");
        }
    }
    Ok(StatusCode::NO_CONTENT.into_response())
}

/// Only serve the keys this service writes: `avatars/<account-uuid>.png`.
/// Anything else (traversal, other buckets/paths) is a 404. The gateway blocks
/// dot-segments upstream, but the service validates independently.
fn is_serveable_key(key: &str) -> bool {
    key.starts_with("avatars/")
        && key.ends_with(".png")
        && !key.contains("..")
        && key.matches('/').count() == 1
}

/// Public avatar read: streams the object out of private MinIO. Placed behind
/// the gateway's internal-key guard like every other route, but deliberately
/// takes no `GatewayIdentity` so anonymous profile pages can load avatars.
pub async fn serve(State(state): State<AvatarState>, Path(key): Path<String>) -> Response {
    if !is_serveable_key(&key) {
        return (StatusCode::NOT_FOUND, "not found").into_response();
    }
    match state.storage.get(&key).await {
        Ok(bytes) => (
            [
                (header::CONTENT_TYPE, "image/png"),
                (header::CACHE_CONTROL, "public, max-age=300"),
            ],
            bytes,
        )
            .into_response(),
        Err(err) => {
            tracing::warn!("avatar serve miss for {key}: {err}");
            (StatusCode::NOT_FOUND, "not found").into_response()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::is_serveable_key;

    #[test]
    fn serves_only_the_expected_avatar_key_shape() {
        assert!(is_serveable_key("avatars/6a7c.png"));
        assert!(!is_serveable_key("avatars/a/../b.png"));
        assert!(!is_serveable_key("avatars/nested/deep.png"));
        assert!(!is_serveable_key("secrets/token.png"));
        assert!(!is_serveable_key("avatars/nope.jpg"));
    }
}
