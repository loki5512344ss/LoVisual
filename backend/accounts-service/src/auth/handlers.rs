use crate::accounts::repo;
use crate::auth::{password, tokens};
use crate::error::{AppError, AppJson};
use axum::{extract::State, http::StatusCode, Json};
use axum_extra::extract::cookie::CookieJar;
use common::jwt;
use serde::{Deserialize, Serialize};

#[derive(Clone)]
pub struct AuthState {
    pub pool: sqlx::PgPool,
    pub jwt_secret: String,
    pub cookie_secure: bool,
    pub hasher: password::PasswordHasher,
    // A real Argon2id hash of a throwaway string. `login` verifies against
    // it when the email is unknown so that "no such account" costs the same
    // ~100ms as "wrong password" — otherwise response time leaks which
    // emails are registered (user enumeration via timing).
    dummy_hash: String,
}

impl AuthState {
    pub fn new(pool: sqlx::PgPool, jwt_secret: String, cookie_secure: bool) -> Self {
        // Hashing a fixed, short constant with fixed valid params cannot
        // fail; this is not user input, so the expect is a startup invariant.
        let dummy_hash = password::hash_password("timing-equalizer-not-a-real-password")
            .expect("hashing a constant with pinned params cannot fail");
        AuthState {
            pool,
            jwt_secret,
            cookie_secure,
            hasher: password::PasswordHasher::new(),
            dummy_hash,
        }
    }
}

#[derive(Deserialize)]
pub struct RegisterRequest {
    pub email: String,
    pub password: String,
    pub nick: String,
}

#[derive(Serialize)]
pub struct RegisterResponse {
    pub id: uuid::Uuid,
    pub email: String,
    pub display_nick: String,
}

/// Validates and normalizes a register request. Returns the trimmed
/// `(email, nick)` on success.
fn validate_register(req: &RegisterRequest) -> Result<(String, String), AppError> {
    let email = req.email.trim();
    if email.is_empty() {
        return Err(AppError::Validation("email must not be empty".into()));
    }
    if email.len() > 254 {
        return Err(AppError::Validation("email must be at most 254 characters".into()));
    }
    let mut parts = email.split('@');
    let (Some(local), Some(domain)) = (parts.next(), parts.next()) else {
        return Err(AppError::Validation("email must contain '@'".into()));
    };
    if parts.next().is_some() || local.is_empty() || domain.is_empty() {
        return Err(AppError::Validation("email must have exactly one '@' with non-empty parts".into()));
    }

    let nick = req.nick.trim();
    let nick_len = nick.chars().count();
    if nick_len == 0 || nick_len > 32 {
        return Err(AppError::Validation("nick must be 1 to 32 characters".into()));
    }
    if nick.chars().any(|c| c.is_control()) {
        return Err(AppError::Validation("nick must not contain control characters".into()));
    }

    if req.password.chars().count() < 8 {
        return Err(AppError::Validation("password must be at least 8 characters".into()));
    }
    if req.password.len() > password::MAX_PASSWORD_BYTES {
        return Err(AppError::Validation(format!(
            "password must be at most {} bytes",
            password::MAX_PASSWORD_BYTES
        )));
    }

    Ok((email.to_string(), nick.to_string()))
}

pub async fn register(
    State(state): State<AuthState>,
    AppJson(req): AppJson<RegisterRequest>,
) -> Result<(StatusCode, Json<RegisterResponse>), AppError> {
    let (email, nick) = validate_register(&req)?;
    let hash = state.hasher.hash(req.password.clone()).await.map_err(AppError::Internal)?;
    let account = repo::create(&state.pool, &email, &hash, &nick).await?;
    Ok((
        StatusCode::CREATED,
        Json(RegisterResponse {
            id: account.id,
            email: account.email,
            display_nick: account.display_nick,
        }),
    ))
}

#[derive(Deserialize)]
pub struct LoginRequest {
    pub email: String,
    pub password: String,
}

#[derive(Serialize)]
pub struct LoginResponse {
    pub access_token: String,
}

pub async fn login(
    State(state): State<AuthState>,
    jar: CookieJar,
    AppJson(req): AppJson<LoginRequest>,
) -> Result<(CookieJar, Json<LoginResponse>), AppError> {
    let Some(account) = repo::find_by_email(&state.pool, &req.email).await? else {
        // Burn the same Argon2 cost as a real check, then fail identically.
        state
            .hasher
            .verify(req.password.clone(), state.dummy_hash.clone())
            .await
            .map_err(AppError::Internal)?;
        return Err(AppError::Unauthorized);
    };
    if !state
        .hasher
        .verify(req.password.clone(), account.password_hash.clone())
        .await
        .map_err(AppError::Internal)?
    {
        return Err(AppError::Unauthorized);
    }
    let refresh = tokens::store_refresh(&state.pool, account.id).await?;
    Ok((
        jar.add(tokens::refresh_cookie(refresh, state.cookie_secure)),
        Json(LoginResponse { access_token: jwt::issue_access_token(account.id, &state.jwt_secret) }),
    ))
}

pub async fn refresh(
    State(state): State<AuthState>,
    jar: CookieJar,
) -> Result<(CookieJar, Json<LoginResponse>), AppError> {
    let token = jar.get(tokens::REFRESH_COOKIE).map(|c| c.value().to_owned()).ok_or(AppError::Unauthorized)?;
    match tokens::rotate_refresh(&state.pool, &token).await? {
        tokens::RotateOutcome::Rotated { account_id, new_token } => Ok((
            jar.add(tokens::refresh_cookie(new_token, state.cookie_secure)),
            Json(LoginResponse { access_token: jwt::issue_access_token(account_id, &state.jwt_secret) }),
        )),
        tokens::RotateOutcome::Invalid => Err(AppError::Unauthorized),
    }
}

pub async fn logout(
    State(state): State<AuthState>,
    jar: CookieJar,
) -> Result<(CookieJar, StatusCode), AppError> {
    if let Some(cookie) = jar.get(tokens::REFRESH_COOKIE) {
        tokens::revoke_refresh(&state.pool, cookie.value()).await?;
    }
    Ok((
        jar.remove(axum_extra::extract::cookie::Cookie::build(tokens::REFRESH_COOKIE).path("/auth")),
        StatusCode::NO_CONTENT,
    ))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn valid_request() -> RegisterRequest {
        RegisterRequest {
            email: "user@example.com".into(),
            password: "password123".into(),
            nick: "Rider".into(),
        }
    }

    #[test]
    fn valid_request_passes() {
        assert!(validate_register(&valid_request()).is_ok());
    }

    #[test]
    fn short_password_is_rejected() {
        let mut req = valid_request();
        req.password = "short12".into();
        assert!(validate_register(&req).is_err());
    }

    #[test]
    fn empty_email_is_rejected() {
        let mut req = valid_request();
        req.email = "   ".into();
        assert!(validate_register(&req).is_err());
    }

    #[test]
    fn email_without_at_is_rejected() {
        let mut req = valid_request();
        req.email = "not-an-email".into();
        assert!(validate_register(&req).is_err());
    }

    #[test]
    fn empty_nick_is_rejected() {
        let mut req = valid_request();
        req.nick = "   ".into();
        assert!(validate_register(&req).is_err());
    }

    #[test]
    fn thirty_three_char_nick_is_rejected() {
        let mut req = valid_request();
        req.nick = "a".repeat(33);
        assert!(validate_register(&req).is_err());
    }
}
