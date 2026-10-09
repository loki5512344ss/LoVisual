use crate::auth::password::MAX_PASSWORD_BYTES;
use crate::error::AppError;
use chrono::{DateTime, Utc};
use serde::Serialize;
use uuid::Uuid;

#[derive(Debug, Clone, sqlx::FromRow, Serialize)]
pub struct Account {
    pub id: Uuid,
    pub email: String,
    #[serde(skip_serializing)]
    pub password_hash: String,
    pub display_nick: String,
    pub role: String,
    pub can_publish_addons: bool,
    pub created_at: DateTime<Utc>,
}

/// Validates and normalizes a registration request. Returns the trimmed
/// `(email, nick)` on success. Lives here (rather than `auth::handlers`) so
/// that handler file stays small and this stays testable independent of axum.
pub fn validate_register(
    email: &str,
    password: &str,
    nick: &str,
) -> Result<(String, String), AppError> {
    let email = normalize_email(email)?;
    let nick = validate_nick(nick)?;
    validate_password(password)?;
    Ok((email, nick))
}

/// Trims and shape-checks an email address the same way for every auth flow
/// that accepts one (register, password reset request). The lookup itself is
/// case-insensitive: `accounts_email_lower_idx` is a unique index on
/// `lower(email)`, and `repo::find_by_email` lowercases the query value.
pub fn normalize_email(email: &str) -> Result<String, AppError> {
    let email = email.trim();
    if email.is_empty() {
        return Err(AppError::Validation("email must not be empty".into()));
    }
    if email.len() > 254 {
        return Err(AppError::Validation(
            "email must be at most 254 characters".into(),
        ));
    }
    let mut parts = email.split('@');
    let (Some(local), Some(domain)) = (parts.next(), parts.next()) else {
        return Err(AppError::Validation("email must contain '@'".into()));
    };
    if parts.next().is_some() || local.is_empty() || domain.is_empty() {
        return Err(AppError::Validation(
            "email must have exactly one '@' with non-empty parts".into(),
        ));
    }
    Ok(email.to_string())
}

fn validate_nick(nick: &str) -> Result<String, AppError> {
    let nick = nick.trim();
    let nick_len = nick.chars().count();
    if nick_len == 0 || nick_len > 32 {
        return Err(AppError::Validation(
            "nick must be 1 to 32 characters".into(),
        ));
    }
    if nick.chars().any(|c| c.is_control()) {
        return Err(AppError::Validation(
            "nick must not contain control characters".into(),
        ));
    }
    Ok(nick.to_string())
}

/// Password policy shared by registration and password reset.
pub fn validate_password(password: &str) -> Result<(), AppError> {
    if password.chars().count() < 8 {
        return Err(AppError::Validation(
            "password must be at least 8 characters".into(),
        ));
    }
    if password.len() > MAX_PASSWORD_BYTES {
        return Err(AppError::Validation(format!(
            "password must be at most {MAX_PASSWORD_BYTES} bytes"
        )));
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn valid() -> (String, String, String) {
        (
            "user@example.com".into(),
            "password123".into(),
            "Rider".into(),
        )
    }

    #[test]
    fn valid_request_passes() {
        let (email, password, nick) = valid();
        assert!(validate_register(&email, &password, &nick).is_ok());
    }

    #[test]
    fn short_password_is_rejected() {
        let (email, _, nick) = valid();
        assert!(validate_register(&email, "short12", &nick).is_err());
    }

    #[test]
    fn empty_email_is_rejected() {
        let (_, password, nick) = valid();
        assert!(validate_register("   ", &password, &nick).is_err());
    }

    #[test]
    fn email_without_at_is_rejected() {
        let (_, password, nick) = valid();
        assert!(validate_register("not-an-email", &password, &nick).is_err());
    }

    #[test]
    fn empty_nick_is_rejected() {
        let (email, password, _) = valid();
        assert!(validate_register(&email, &password, "   ").is_err());
    }

    #[test]
    fn thirty_three_char_nick_is_rejected() {
        let (email, password, _) = valid();
        assert!(validate_register(&email, &password, &"a".repeat(33)).is_err());
    }
}
