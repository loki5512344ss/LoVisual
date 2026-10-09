use axum_extra::extract::cookie::{Cookie, SameSite};
use base64::{Engine, engine::general_purpose::URL_SAFE_NO_PAD};
use chrono::{DateTime, Utc};
use rand::RngExt;
use sha2::{Digest, Sha256};
use sqlx::PgPool;
use uuid::Uuid;

/// A refresh token revoked by rotation less than this long ago is treated as
/// a benign race between two concurrent refreshes of the same token (e.g.
/// two open tabs), not token theft: the second caller gets a plain 401
/// without the reuse-detection cascade that would kill every session.
const ROTATION_GRACE: chrono::Duration = chrono::Duration::seconds(10);

pub const REFRESH_COOKIE: &str = "lv_refresh";

/// 256-bit random token: nothing to brute-force, so a slow hash would only
/// add latency to every refresh — plain SHA-256 for storage is enough.
pub fn new_opaque_token(prefix: &str) -> String {
    let mut bytes = [0u8; 32];
    rand::rng().fill(&mut bytes);
    format!("{prefix}{}", URL_SAFE_NO_PAD.encode(bytes))
}

pub fn hash_token(token: &str) -> String {
    hex::encode(Sha256::digest(token.as_bytes()))
}

/// Purposes of the single-use email-bound tokens in `email_tokens`
/// (mirrors the CHECK constraint in migrations/0006_email_tokens.sql).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum EmailTokenPurpose {
    PasswordReset,
    EmailVerify,
}

impl EmailTokenPurpose {
    pub fn as_str(self) -> &'static str {
        match self {
            EmailTokenPurpose::PasswordReset => "password_reset",
            EmailTokenPurpose::EmailVerify => "email_verify",
        }
    }
}

/// Issues a fresh single-use token of `purpose` inside one transaction: any
/// still-pending token of the same purpose is invalidated and the new hash
/// stored atomically, so two concurrent requests can never leave two live
/// tokens behind. Returns the plaintext token for the mailer only — it is
/// never persisted in clear form.
pub async fn issue_email_token(
    pool: &PgPool,
    account_id: Uuid,
    purpose: EmailTokenPurpose,
    prefix: &str,
    ttl_minutes: i64,
) -> Result<String, sqlx::Error> {
    let mut tx = pool.begin().await?;
    sqlx::query(
        "UPDATE email_tokens SET used_at = now()
         WHERE account_id = $1 AND purpose = $2 AND used_at IS NULL",
    )
    .bind(account_id)
    .bind(purpose.as_str())
    .execute(&mut *tx)
    .await?;
    let token = new_opaque_token(prefix);
    sqlx::query(
        "INSERT INTO email_tokens (account_id, purpose, token_hash, expires_at)
         VALUES ($1, $2, $3, now() + make_interval(mins => $4::int))",
    )
    .bind(account_id)
    .bind(purpose.as_str())
    .bind(hash_token(&token))
    .bind(ttl_minutes)
    .execute(&mut *tx)
    .await?;
    tx.commit().await?;
    Ok(token)
}

/// Atomically marks the token as used and returns its account. The single
/// conditional UPDATE makes double-spend impossible even for concurrent
/// callers: exactly one of them gets the row back. `purpose` participates in
/// the WHERE clause, so a verification token can never be replayed as a
/// reset token (or vice versa) even though both live in the same table.
pub async fn consume_email_token(
    pool: &PgPool,
    token: &str,
    purpose: EmailTokenPurpose,
) -> Result<Option<Uuid>, sqlx::Error> {
    let row: Option<(Uuid,)> = sqlx::query_as(
        "UPDATE email_tokens SET used_at = now()
         WHERE token_hash = $1 AND purpose = $2 AND used_at IS NULL AND expires_at > now()
         RETURNING account_id",
    )
    .bind(hash_token(token))
    .bind(purpose.as_str())
    .fetch_optional(pool)
    .await?;
    Ok(row.map(|(id,)| id))
}

pub async fn store_refresh(pool: &PgPool, account_id: Uuid) -> Result<String, sqlx::Error> {
    let token = new_opaque_token("lvr_");
    sqlx::query(
        "INSERT INTO refresh_tokens (account_id, token_hash, expires_at)
         VALUES ($1, $2, now() + interval '30 days')",
    )
    .bind(account_id)
    .bind(hash_token(&token))
    .execute(pool)
    .await?;
    Ok(token)
}

pub enum RotateOutcome {
    Rotated { account_id: Uuid, new_token: String },
    Invalid,
}

pub async fn rotate_refresh(pool: &PgPool, token: &str) -> Result<RotateOutcome, sqlx::Error> {
    let mut tx = pool.begin().await?;
    let row: Option<(Uuid, bool, bool, Option<DateTime<Utc>>)> = sqlx::query_as(
        "SELECT account_id, revoked_at IS NOT NULL, expires_at <= now(), rotated_at
         FROM refresh_tokens WHERE token_hash = $1 FOR UPDATE",
    )
    .bind(hash_token(token))
    .fetch_optional(&mut *tx)
    .await?;

    let outcome = match row {
        None => RotateOutcome::Invalid,
        Some((_, true, _, Some(rotated_at))) if Utc::now() - rotated_at < ROTATION_GRACE => {
            // Two concurrent refreshes of the same token (e.g. two tabs): the
            // first already rotated it moments ago. Reject this one without
            // the reuse-detection cascade, so the first caller's new token
            // (and every other session) stays valid.
            RotateOutcome::Invalid
        }
        Some((account_id, true, _, _)) => {
            // Reuse of a rotated token outside the grace window: someone else
            // holds a copy. Kill all sessions.
            sqlx::query(
                "UPDATE refresh_tokens SET revoked_at = now()
                 WHERE account_id = $1 AND revoked_at IS NULL",
            )
            .bind(account_id)
            .execute(&mut *tx)
            .await?;
            RotateOutcome::Invalid
        }
        Some((_, false, true, _)) => RotateOutcome::Invalid,
        Some((account_id, false, false, _)) => {
            sqlx::query(
                "UPDATE refresh_tokens SET revoked_at = now(), rotated_at = now()
                 WHERE token_hash = $1",
            )
            .bind(hash_token(token))
            .execute(&mut *tx)
            .await?;
            let new_token = new_opaque_token("lvr_");
            sqlx::query(
                "INSERT INTO refresh_tokens (account_id, token_hash, expires_at)
                 VALUES ($1, $2, now() + interval '30 days')",
            )
            .bind(account_id)
            .bind(hash_token(&new_token))
            .execute(&mut *tx)
            .await?;
            RotateOutcome::Rotated {
                account_id,
                new_token,
            }
        }
    };
    tx.commit().await?;
    Ok(outcome)
}

pub async fn revoke_refresh(pool: &PgPool, token: &str) -> Result<(), sqlx::Error> {
    sqlx::query(
        "UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = $1 AND revoked_at IS NULL",
    )
    .bind(hash_token(token))
    .execute(pool)
    .await?;
    Ok(())
}

/// The refresh token never touches JS: httpOnly, Strict, scoped to /auth,
/// so an XSS on the site cannot exfiltrate it.
pub fn refresh_cookie(token: String, secure: bool) -> Cookie<'static> {
    Cookie::build((REFRESH_COOKIE, token))
        .http_only(true)
        .secure(secure)
        .same_site(SameSite::Strict)
        .path("/auth")
        .max_age(time::Duration::days(30))
        .build()
}

/// Removal cookie for logout: same attributes as `refresh_cookie` (minus the
/// value/max-age) so the browser actually matches and clears it — a cookie
/// removal with mismatched attributes is silently ignored.
pub fn removal_cookie(secure: bool) -> Cookie<'static> {
    Cookie::build((REFRESH_COOKIE, ""))
        .http_only(true)
        .secure(secure)
        .same_site(SameSite::Strict)
        .path("/auth")
        .max_age(time::Duration::ZERO)
        .build()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn opaque_tokens_are_prefixed_unique_and_long() {
        let a = new_opaque_token("lvr_");
        let b = new_opaque_token("lvr_");
        assert!(a.starts_with("lvr_"));
        assert_eq!(a.len(), 4 + 43);
        assert_ne!(a, b);
    }

    #[test]
    fn hash_is_stable_hex_sha256() {
        assert_eq!(hash_token("x"), hash_token("x"));
        assert_eq!(hash_token("x").len(), 64);
        assert_ne!(hash_token("x"), hash_token("y"));
    }

    #[test]
    fn refresh_cookie_has_the_hardened_attributes() {
        let cookie = refresh_cookie("t".into(), true);
        assert_eq!(cookie.name(), REFRESH_COOKIE);
        assert_eq!(cookie.http_only(), Some(true));
        assert_eq!(cookie.secure(), Some(true));
        assert_eq!(cookie.same_site(), Some(SameSite::Strict));
        assert_eq!(cookie.path(), Some("/auth"));
    }
}
