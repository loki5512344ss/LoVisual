use crate::accounts::repo;
use crate::auth::verify;
use crate::error::{AppError, AppJson};
use axum::{Json, extract::State, http::StatusCode};
use common::internal::GatewayIdentity;
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use uuid::Uuid;

use super::super::handlers::AuthState;

#[derive(Deserialize)]
pub struct VerifyEmailRequest {
    pub token: String,
}

#[derive(Serialize)]
pub struct AcceptedResponse {
    pub status: &'static str,
}

/// POST /auth/verify-email { token }
///
/// Consumes the token atomically and marks the address verified. Bad tokens
/// are a plain 400 with no distinction between unknown, expired and used.
pub async fn verify_email(
    State(state): State<AuthState>,
    AppJson(req): AppJson<VerifyEmailRequest>,
) -> Result<Json<AcceptedResponse>, AppError> {
    if !verify::is_well_formed_token(&req.token) {
        return Err(AppError::Validation("malformed verification token".into()));
    }
    let account_id = verify::consume_verify_token(&state.pool, &req.token)
        .await?
        .ok_or_else(|| AppError::Validation("verification token is invalid or expired".into()))?;
    // `AND email_verified_at IS NULL` keeps a replay racing the first click
    // from rewinding the timestamp; either way the outcome is "verified".
    sqlx::query(
        "UPDATE accounts SET email_verified_at = now()
         WHERE id = $1 AND email_verified_at IS NULL",
    )
    .bind(account_id)
    .execute(&state.pool)
    .await?;
    Ok(Json(AcceptedResponse {
        status: "email verified",
    }))
}

/// POST /auth/resend-verification (requires a valid session)
///
/// Always answers 202 for a signed-in caller, verified or not; the
/// background task simply skips the mail when there is nothing left to
/// verify. Fail-closed: 503 when no mail back-end is configured.
pub async fn resend_verification(
    State(state): State<AuthState>,
    identity: GatewayIdentity,
) -> Result<(StatusCode, Json<AcceptedResponse>), AppError> {
    if !state.mail.enabled() {
        return Err(AppError::Unavailable);
    }
    let (mail, pool) = (state.mail.clone(), state.pool.clone());
    tokio::spawn(async move {
        if let Err(err) = send_verification_email(&pool, &mail, identity.account_id).await {
            tracing::error!(
                account_id = %identity.account_id,
                "resend-verification background task failed: {err:#}"
            );
        }
    });
    Ok((
        StatusCode::ACCEPTED,
        Json(AcceptedResponse {
            status: "verification email sent",
        }),
    ))
}

/// Issues a fresh verification token and hands it to the mailer. Shared by
/// registration and resend. A no-op when the address is already verified.
pub(crate) async fn send_verification_email(
    pool: &PgPool,
    mail_box: &crate::auth::reset::MailBox,
    account_id: Uuid,
) -> anyhow::Result<()> {
    let Some(account) = repo::find_by_id(pool, account_id).await? else {
        return Ok(());
    };
    if account.email_verified_at.is_some() {
        return Ok(());
    }
    let token = verify::issue_verify_token(pool, account.id).await?;
    mail_box.send_verify(&account.email, &token).await;
    Ok(())
}
