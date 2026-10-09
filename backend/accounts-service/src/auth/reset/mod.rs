pub mod handlers;

use sqlx::PgPool;
use tokio::sync::mpsc::UnboundedSender;
use uuid::Uuid;

use crate::auth::tokens::{hash_token, new_opaque_token};

/// One outgoing reset email. `token` is the plaintext token: the only place
/// it ever exists outside the response of `issue_reset_token`.
#[derive(Debug, Clone)]
pub struct Mail {
    pub to: String,
    pub token: String,
}

/// Delivery back-end for reset emails.
#[derive(Clone)]
pub enum MailBox {
    /// Development delivery: a structured log line carrying the token, which
    /// local/dev stacks pick up from the container logs. When a real provider
    /// is wired in (lettre/SES/anything), this variant is the single switch
    /// point - the endpoint contract does not change.
    Log,
    /// In-process queue: tests (and a future in-process mail worker) receive
    /// every mail exactly as the handler produced it.
    Queue(UnboundedSender<Mail>),
}

impl MailBox {
    pub fn send(&self, to: &str, token: &str) {
        match self {
            MailBox::Log => {
                tracing::info!(
                    account = %to,
                    reset_token = %token,
                    "password reset requested; deliver the reset link to the account owner"
                );
            }
            MailBox::Queue(tx) => {
                let _ = tx.send(Mail {
                    to: to.to_string(),
                    token: token.to_string(),
                });
            }
        }
    }
}

/// Reset links must be used quickly: long windows turn a leaked email into
/// an account takeover. 30 minutes is the common industry compromise.
const TTL_MINUTES: i64 = 30;

pub const TOKEN_PREFIX: &str = "lvpr_";

/// A reset token is base64url like every other opaque token in this service
/// ("lvpr_" prefix + 43 chars), so the length check alone filters out most
/// junk before the database is ever touched.
pub fn is_well_formed_token(token: &str) -> bool {
    token.len() == TOKEN_PREFIX.len() + 43 && token.starts_with(TOKEN_PREFIX)
}

/// Invalidates any token still pending for the account, then stores the hash
/// of a fresh one. Returns the plaintext token for the mailer only — it is
/// never persisted in clear form.
pub async fn issue_reset_token(pool: &PgPool, account_id: Uuid) -> Result<String, sqlx::Error> {
    sqlx::query(
        "UPDATE password_reset_tokens SET used_at = now()
         WHERE account_id = $1 AND used_at IS NULL",
    )
    .bind(account_id)
    .execute(pool)
    .await?;
    let token = new_opaque_token(TOKEN_PREFIX);
    sqlx::query(
        "INSERT INTO password_reset_tokens (account_id, token_hash, expires_at)
         VALUES ($1, $2, now() + make_interval(mins => $3::int))",
    )
    .bind(account_id)
    .bind(hash_token(&token))
    .bind(TTL_MINUTES)
    .execute(pool)
    .await?;
    Ok(token)
}

/// Atomically marks the token as used and returns its account. The single
/// conditional UPDATE makes double-spend impossible even for concurrent
/// callers: exactly one of them gets the row back.
pub async fn consume_reset_token(pool: &PgPool, token: &str) -> Result<Option<Uuid>, sqlx::Error> {
    let row: Option<(Uuid,)> = sqlx::query_as(
        "UPDATE password_reset_tokens SET used_at = now()
         WHERE token_hash = $1 AND used_at IS NULL AND expires_at > now()
         RETURNING account_id",
    )
    .bind(hash_token(token))
    .fetch_optional(pool)
    .await?;
    Ok(row.map(|(id,)| id))
}

/// Kill every refresh session of the account: whoever holds a stolen session
/// cookie must not survive a password change.
pub async fn revoke_all_sessions(pool: &PgPool, account_id: Uuid) -> Result<(), sqlx::Error> {
    sqlx::query(
        "UPDATE refresh_tokens SET revoked_at = now()
         WHERE account_id = $1 AND revoked_at IS NULL",
    )
    .bind(account_id)
    .execute(pool)
    .await?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn well_formed_token_shape_matches_opaque_generator() {
        let token = new_opaque_token(TOKEN_PREFIX);
        assert!(is_well_formed_token(&token));
        assert!(!is_well_formed_token(&format!("{token}x")));
        assert!(!is_well_formed_token("lvpr_short"));
        assert!(!is_well_formed_token(
            "XWpr_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        ));
    }
}
