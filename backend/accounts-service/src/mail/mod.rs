//! Outgoing email: the `Mailer` transports, the SMTP/no-op implementations
//! and the shared, non-reversible log tag for addresses.
//!
//! Secrets discipline: no caller ever passes a plaintext token into a log
//! line; templates are the only place user-visible mail text exists, and
//! their dynamic input is escaped there.

pub mod layout;
pub mod templates;

use std::future::Future;

use anyhow::{Context, Result};
use lettre::{
    AsyncSmtpTransport, AsyncTransport, Tokio1Executor,
    message::{Mailbox, Message, MultiPart},
    transport::smtp::{
        authentication::Credentials,
        client::{Tls, TlsParameters},
    },
};
use sha2::{Digest, Sha256};

/// Language of the built-in email templates (`MAIL_LANG`).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Lang {
    #[default]
    Ru,
    En,
}

impl Lang {
    pub fn parse(s: &str) -> Option<Lang> {
        match s.trim().to_lowercase().as_str() {
            "ru" => Some(Lang::Ru),
            "en" => Some(Lang::En),
            _ => None,
        }
    }
}

/// A fully rendered email ready for any transport. Templates escape dynamic
/// content, so a draft is safe to hand to a transport as-is.
pub struct EmailDraft {
    pub to: String,
    pub subject: String,
    pub text: String,
    pub html: String,
}

/// Transport abstraction. One method keeps fakes trivial. Desugared to an
/// explicit `Send` future (rather than AFIT) so the trait stays usable from
/// `tokio::spawn`ed background tasks without hidden auto-trait surprises.
pub trait Mailer {
    fn deliver(&self, draft: EmailDraft) -> impl Future<Output = Result<()>> + Send;
}

/// Real SMTP via lettre. TLS only: 465 speaks implicit TLS, every other port
/// uses required STARTTLS (no plaintext downgrade for password mail). rustls
/// everywhere — no native-tls in the dependency tree.
pub struct SmtpMailer {
    transport: AsyncSmtpTransport<Tokio1Executor>,
    from: Mailbox,
    public_base_url: String,
}

impl SmtpMailer {
    /// `from` is a full mailbox (`LoVisual <noreply@loki-code.dev>` or a bare
    /// address); `public_base_url` is the only link origin emails may carry.
    pub fn new(
        host: &str,
        port: u16,
        user: &str,
        password: &str,
        from: &str,
        public_base_url: &str,
    ) -> Result<Self> {
        let tls = TlsParameters::builder(host.to_owned())
            .build()
            .context("building SMTP TLS parameters")?;
        let mut builder = AsyncSmtpTransport::<Tokio1Executor>::builder_dangerous(host.to_owned())
            .port(port)
            .tls(if port == 465 {
                Tls::Wrapper(tls)
            } else {
                Tls::Required(tls)
            });
        if !user.trim().is_empty() {
            builder = builder.credentials(Credentials::new(user.to_owned(), password.to_owned()));
        }
        Ok(SmtpMailer {
            transport: builder.build(),
            from: from.parse().context("MAIL_FROM is not a valid mailbox")?,
            public_base_url: public_base_url.trim().trim_end_matches('/').to_owned(),
        })
    }

    /// Link origin for email buttons, taken from config only — never from
    /// request headers (host header injection would forge phishing links).
    pub fn public_base_url(&self) -> &str {
        &self.public_base_url
    }
}

impl Mailer for SmtpMailer {
    async fn deliver(&self, draft: EmailDraft) -> Result<()> {
        let mail = Message::builder()
            .from(self.from.clone())
            .to(draft
                .to
                .parse()
                .context("recipient is not a valid mailbox")?)
            .subject(draft.subject)
            .multipart(MultiPart::alternative_plain_html(draft.text, draft.html))?;
        self.transport.send(mail).await?;
        Ok(())
    }
}

/// Stand-in transport for dev runs that deliberately skip SMTP: reports
/// success and logs the fact, so callers need no disabled branch. Only
/// non-confidential metadata is logged.
pub struct NoopMailer;

impl Mailer for NoopMailer {
    async fn deliver(&self, draft: EmailDraft) -> Result<()> {
        tracing::info!(subject = %draft.subject, "no-op mailer: delivery suppressed (no SMTP configured)");
        Ok(())
    }
}

/// Short non-reversible log tag for an address: enough to correlate log
/// lines for the same recipient across requests, useless for rebuilding the
/// address or matching it against a candidate list.
pub fn address_tag(email: &str) -> String {
    let digest = Sha256::digest(email.trim().to_lowercase().as_bytes());
    digest[..4].iter().map(|b| format!("{b:02x}")).collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn lang_parses_both_locales_and_nothing_else() {
        assert_eq!(Lang::parse("ru"), Some(Lang::Ru));
        assert_eq!(Lang::parse("EN"), Some(Lang::En));
        assert_eq!(Lang::parse("de"), None);
        assert_eq!(Lang::parse(""), None);
    }

    #[test]
    fn address_tag_is_short_stable_and_not_the_address() {
        let a = address_tag("User@Example.com ");
        let b = address_tag("user@example.com");
        assert_eq!(a, b, "tag must normalize case and whitespace");
        assert_eq!(a.len(), 8);
        assert!(!a.contains("user"));
    }

    #[test]
    fn address_tag_differs_per_address() {
        assert_ne!(address_tag("a@example.com"), address_tag("b@example.com"));
    }

    #[tokio::test]
    async fn noop_mailer_reports_success_without_sending() {
        NoopMailer
            .deliver(EmailDraft {
                to: "a@example.com".into(),
                subject: "s".into(),
                text: "t".into(),
                html: "<p>t</p>".into(),
            })
            .await
            .expect("no-op delivery must succeed");
    }
}
