use crate::accounts::{model, repo};
use crate::auth::reset;
use crate::error::{AppError, AppJson};
use axum::{Json, extract::State, http::StatusCode};
use serde::{Deserialize, Serialize};

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
/// oracle for account enumeration. Delivery happens out of band; the gateway
/// rate-limits this route per IP (3/hour) to keep the mailer from being
/// weaponised.
pub async fn forgot_password(
    State(state): State<AuthState>,
    AppJson(req): AppJson<ForgotPasswordRequest>,
) -> Result<(StatusCode, Json<AcceptedResponse>), AppError> {
    let email = model::normalize_email(&req.email)?;
    if let Some(account) = repo::find_by_email(&state.pool, &email).await? {
        let token = reset::issue_reset_token(&state.pool, account.id).await?;
        state.mail.send(&email, &token);
    }
    Ok((
        StatusCode::ACCEPTED,
        Json(AcceptedResponse {
            status: "reset email sent if the account exists",
        }),
    ))
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
