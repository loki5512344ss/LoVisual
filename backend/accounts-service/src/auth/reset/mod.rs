pub mod handlers;

use std::sync::Arc;

use sqlx::PgPool;
use tokio::sync::mpsc::UnboundedSender;
use uuid::Uuid;

use crate::auth::tokens::{EmailTokenPurpose, consume_email_token, issue_email_token};
use crate::mail::{self, Lang, Mailer, SmtpMailer, templates};

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

/// Invalidates any pending reset token of the account, then stores the hash
/// of a fresh one (atomically, see `issue_email_token`).
pub async fn issue_reset_token(pool: &PgPool, account_id: Uuid) -> Result<String, sqlx::Error> {
    issue_email_token(
        pool,
        account_id,
        EmailTokenPurpose::PasswordReset,
        TOKEN_PREFIX,
        TTL_MINUTES,
    )
    .await
}

/// Atomically marks the token as used and returns its account.
pub async fn consume_reset_token(pool: &PgPool, token: &str) -> Result<Option<Uuid>, sqlx::Error> {
    consume_email_token(pool, token, EmailTokenPurpose::PasswordReset).await
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

/// One outgoing email in the in-process queue. The custom `Debug` is a
/// security invariant, not style: the struct carries the plaintext token,
/// and a `{:?}` anywhere near a log line would publish it. Both fields are
/// therefore permanently redacted.
#[derive(Clone)]
pub struct Mail {
    pub to: String,
    pub token: String,
}

impl std::fmt::Debug for Mail {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Mail")
            .field("to", &"[redacted]")
            .field("token", &"[redacted]")
            .finish()
    }
}

/// Delivery back-end for outgoing email. Fail-closed: `Disabled` is the
/// default whenever nothing is configured, and every mail-producing endpoint
/// then answers 503 uniformly instead of silently eating the request.
#[derive(Clone)]
pub enum MailBox {
    /// No mail configured (the default). Forgot-password answers 503 for
    /// every address alike; registration simply skips the verification mail.
    Disabled,
    /// Development-only logging back-end, enabled explicitly with
    /// `RESET_MAIL_MODE=log`; `Config::validate` refuses to start with it
    /// when `COOKIE_SECURE=true`. Logs carry a short address hash only —
    /// never the email, never the token (tests get tokens via `Queue`).
    Log,
    /// In-process queue: tests receive every mail exactly as produced.
    Queue(UnboundedSender<Mail>),
    /// Real SMTP through the lettre-backed `SmtpMailer`.
    Smtp(Arc<SmtpMailer>, Lang),
}

impl MailBox {
    /// Back-end implied by the configuration: explicit `RESET_MAIL_MODE=log`
    /// wins (dev), then SMTP when `SMTP_HOST` is set, else fail-closed.
    /// Infallible for Disabled/Log; the SMTP branch fails fast on a
    /// malformed `MAIL_FROM` or TLS setup.
    pub fn from_config(cfg: &crate::config::Config) -> anyhow::Result<MailBox> {
        if cfg.reset_mail_log {
            return Ok(MailBox::Log);
        }
        if cfg.smtp_host.trim().is_empty() {
            return Ok(MailBox::Disabled);
        }
        Ok(MailBox::Smtp(
            Arc::new(SmtpMailer::new(
                &cfg.smtp_host,
                cfg.smtp_port,
                &cfg.smtp_user,
                &cfg.smtp_password,
                &cfg.mail_from,
                &cfg.public_base_url,
            )?),
            cfg.mail_lang,
        ))
    }

    /// Whether mail can go out at all. When false, mail-producing endpoints
    /// answer 503 before touching the database (no timing side channel).
    pub fn enabled(&self) -> bool {
        !matches!(self, MailBox::Disabled)
    }

    pub async fn send_reset(&self, to: &str, token: &str) {
        match self {
            MailBox::Disabled => {}
            MailBox::Log => tracing::info!(
                address_tag = %mail::address_tag(to),
                "password reset requested (LOG mailer: deliver out of band; the token is not logged)"
            ),
            MailBox::Queue(tx) => {
                let _ = tx.send(Mail {
                    to: to.to_owned(),
                    token: token.to_owned(),
                });
            }
            MailBox::Smtp(mailer, lang) => {
                let content = templates::reset_email(*lang, mailer.public_base_url(), token);
                self.deliver_smtp(mailer, to, content).await;
            }
        }
    }

    pub async fn send_verify(&self, to: &str, token: &str) {
        match self {
            MailBox::Disabled => {}
            MailBox::Log => tracing::info!(
                address_tag = %mail::address_tag(to),
                "verification email requested (LOG mailer: deliver out of band; the token is not logged)"
            ),
            MailBox::Queue(tx) => {
                let _ = tx.send(Mail {
                    to: to.to_owned(),
                    token: token.to_owned(),
                });
            }
            MailBox::Smtp(mailer, lang) => {
                let content = templates::verify_email(*lang, mailer.public_base_url(), token);
                self.deliver_smtp(mailer, to, content).await;
            }
        }
    }

    /// SMTP delivery with secret-free error logging: the recipient appears
    /// only as its address tag, and the lettre error is reduced to its
    /// permanent/transient flags because SMTP transcripts can echo the
    /// recipient address back.
    async fn deliver_smtp(&self, mailer: &SmtpMailer, to: &str, content: templates::EmailContent) {
        let draft = mail::EmailDraft {
            to: to.to_owned(),
            subject: content.subject,
            text: content.text,
            html: content.html,
        };
        if let Err(err) = mailer.deliver(draft).await {
            // The lettre error is reduced to its flags: SMTP transcripts can
            // echo the recipient address back, so the message itself stays
            // out of the log.
            let smtp = err
                .downcast_ref::<lettre::transport::smtp::Error>()
                .map(|e| (e.is_permanent(), e.is_transient()));
            let (permanent, transient) = smtp.unwrap_or((false, false));
            tracing::error!(
                address_tag = %mail::address_tag(to),
                permanent,
                transient,
                "smtp delivery failed"
            );
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn well_formed_token_shape_matches_opaque_generator() {
        let token = crate::auth::tokens::new_opaque_token(TOKEN_PREFIX);
        assert!(is_well_formed_token(&token));
        assert!(!is_well_formed_token(&format!("{token}x")));
        assert!(!is_well_formed_token("lvpr_short"));
        assert!(!is_well_formed_token(
            "XWpr_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        ));
    }

    #[test]
    fn mail_debug_never_shows_the_token_or_the_address() {
        let mail = Mail {
            to: "secret-user@example.com".into(),
            token: "lvpr_SUPER_SECRET_TOKEN_VALUE_1234567890123".into(),
        };
        let printed = format!("{mail:?}");
        assert!(!printed.contains("secret-user"));
        assert!(!printed.contains("SUPER_SECRET"));
        assert!(printed.contains("[redacted]"));
    }

    #[test]
    fn disabled_mailbox_is_the_only_disabled_one() {
        let (tx, _rx) = tokio::sync::mpsc::unbounded_channel();
        assert!(!MailBox::Disabled.enabled());
        assert!(MailBox::Log.enabled());
        assert!(MailBox::Queue(tx).enabled());
    }

    #[tokio::test]
    async fn disabled_and_log_senders_are_quiet_no_ops() {
        MailBox::Disabled
            .send_reset("a@example.com", "lvpr_x")
            .await;
        MailBox::Log.send_verify("a@example.com", "lvev_x").await;
    }
}
