use crate::accounts::{model, repo};
use crate::auth::reset::{self, MailBox};
use crate::error::{AppError, AppJson};
use crate::mail;
use axum::{Json, extract::State, http::StatusCode};
use serde::{Deserialize, Serialize};
use sqlx::PgPool;

use super::super::handlers::AuthState;
#[derive(Deserialize)]
pub struct ForgotPasswordRequest {
    pub email: String,
}

#[derive(Serialize)]
pub struct AcceptedResponse {
    pub status: &'static str,
}

/// POST /auth/forgot-password { email }
///
/// The response is identical whether or not the email is registered: no
/// oracle for account enumeration. The account lookup, token issuance and
/// delivery all run in a background task, and the handler answers 202 before
/// any database access, so response timing carries no trace of account
/// existence either. Delivery errors are logged with an address tag only —
/// never the email, never the token.
///
/// Fail-closed: when no mail back-end is configured the endpoint answers the
/// same 503 for every address.
pub async fn forgot_password(
    State(state): State<AuthState>,
    AppJson(req): AppJson<ForgotPasswordRequest>,
) -> Result<(StatusCode, Json<AcceptedResponse>), AppError> {
    let email = model::normalize_email(&req.email)?;
    if !state.mail.enabled() {
        return Err(AppError::Unavailable);
    }
    let (mail, pool) = (state.mail.clone(), state.pool.clone());
    tokio::spawn(async move {
        if let Err(err) = forgot_task(&pool, &mail, &email).await {
            tracing::error!(
                address_tag = %mail::address_tag(&email),
                "forgot-password background task failed: {err:#}"
            );
        }
    });
    Ok((
        StatusCode::ACCEPTED,
        Json(AcceptedResponse {
            status: "reset email sent if the account exists",
        }),
    ))
}

/// The off-request-path half of `forgot_password`. An unknown address is a
/// silent no-op: the caller already got the same 202 as everyone else.
async fn forgot_task(pool: &PgPool, mail: &MailBox, email: &str) -> anyhow::Result<()> {
    let Some(account) = repo::find_by_email(pool, email).await? else {
        return Ok(());
    };
    let token = reset::issue_reset_token(pool, account.id).await?;
    mail.send_reset(email, &token).await;
    Ok(())
}

#[derive(Deserialize)]
pub struct ResetPasswordRequest {
    pub token: String,
    pub new_password: String,
}

/// POST /auth/reset-password { token, new_password }
///
/// Consumes the token atomically, rewrites the password hash and revokes
/// every refresh session of the account. A bad token is a plain 400 with no
/// distinction between unknown, expired and already-used — all three are the
/// same "try again" situation from an attacker's point of view.
pub async fn reset_password(
    State(state): State<AuthState>,
    AppJson(req): AppJson<ResetPasswordRequest>,
) -> Result<Json<AcceptedResponse>, AppError> {
    if !reset::is_well_formed_token(&req.token) {
        return Err(AppError::Validation("malformed reset token".into()));
    }
    model::validate_password(&req.new_password)?;

    let account_id = reset::consume_reset_token(&state.pool, &req.token)
        .await?
        .ok_or_else(|| AppError::Validation("reset token is invalid or expired".into()))?;

    let hash = state
        .hasher
        .hash(req.new_password)
        .await
        .map_err(AppError::Internal)?;
    let updated = sqlx::query("UPDATE accounts SET password_hash = $2 WHERE id = $1")
        .bind(account_id)
        .bind(&hash)
        .execute(&state.pool)
        .await?;
    if updated.rows_affected() != 1 {
        // The token row referenced a cascade-deleted account.
        return Err(AppError::Validation(
            "reset token is invalid or expired".into(),
        ));
    }
    reset::revoke_all_sessions(&state.pool, account_id).await?;
    Ok(Json(AcceptedResponse {
        status: "password updated",
    }))
}
