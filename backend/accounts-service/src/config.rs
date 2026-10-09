use anyhow::{Context, Result};

use crate::mail::Lang;

#[derive(Clone)]
pub struct Config {
    pub database_url: String,
    pub jwt_secret: String,
    pub internal_key: String,
    pub port: u16,
    pub grpc_port: u16,
    pub s3_endpoint: String,
    pub s3_bucket: String,
    pub s3_access_key: String,
    pub s3_secret_key: String,
    pub cookie_secure: bool,
    /// Browser-reachable prefix for stored avatars. When empty, avatars fall
    /// back to the internal `<s3_endpoint>/<bucket>` (dev, where MinIO is
    /// port-forwarded). Production sets this to the same-origin `/api/media`
    /// path served back through the gateway, keeping MinIO private.
    pub avatar_public_base_url: String,
    // --- outgoing mail (SMTP; empty `smtp_host` = mail switched off) ---
    pub smtp_host: String,
    pub smtp_port: u16,
    pub smtp_user: String,
    pub smtp_password: String,
    /// `MAIL_FROM`, e.g. `LoVisual <noreply@loki-code.dev>`.
    pub mail_from: String,
    /// `PUBLIC_BASE_URL`: the only origin email links may carry (host header
    /// injection cannot forge links because headers are never consulted).
    pub public_base_url: String,
    /// `MAIL_LANG`: template language, `ru` by default.
    pub mail_lang: Lang,
    /// `RESET_MAIL_MODE=log`: the development-only logging mailer. Refused
    /// at startup whenever `COOKIE_SECURE=true` (production).
    pub reset_mail_log: bool,
}

impl Config {
    pub fn from_env() -> Result<Config> {
        Ok(Config {
            database_url: std::env::var("DATABASE_URL").context("DATABASE_URL not set")?,
            jwt_secret: std::env::var("JWT_SECRET").context("JWT_SECRET not set")?,
            internal_key: std::env::var("INTERNAL_KEY").context("INTERNAL_KEY not set")?,
            port: std::env::var("PORT")
                .unwrap_or_else(|_| "8081".into())
                .parse()
                .context("PORT must be a number")?,
            grpc_port: std::env::var("GRPC_PORT")
                .unwrap_or_else(|_| "50051".into())
                .parse()
                .context("GRPC_PORT must be a number")?,
            s3_endpoint: std::env::var("S3_ENDPOINT").context("S3_ENDPOINT not set")?,
            s3_bucket: std::env::var("S3_BUCKET").context("S3_BUCKET not set")?,
            s3_access_key: std::env::var("S3_ACCESS_KEY").context("S3_ACCESS_KEY not set")?,
            s3_secret_key: std::env::var("S3_SECRET_KEY").context("S3_SECRET_KEY not set")?,
            cookie_secure: std::env::var("COOKIE_SECURE")
                .map(|v| v != "false")
                .unwrap_or(true),
            avatar_public_base_url: std::env::var("AVATAR_PUBLIC_BASE_URL").unwrap_or_default(),
            smtp_host: std::env::var("SMTP_HOST").unwrap_or_default(),
            smtp_port: std::env::var("SMTP_PORT")
                .unwrap_or_else(|_| "587".into())
                .parse()
                .context("SMTP_PORT must be a number")?,
            smtp_user: std::env::var("SMTP_USER").unwrap_or_default(),
            smtp_password: std::env::var("SMTP_PASSWORD").unwrap_or_default(),
            mail_from: std::env::var("MAIL_FROM").unwrap_or_default(),
            public_base_url: std::env::var("PUBLIC_BASE_URL").unwrap_or_default(),
            mail_lang: Lang::parse(&std::env::var("MAIL_LANG").unwrap_or_else(|_| "ru".into()))
                .context("MAIL_LANG must be ru or en")?,
            reset_mail_log: std::env::var("RESET_MAIL_MODE")
                .map(|v| v.trim().eq_ignore_ascii_case("log"))
                .unwrap_or(false),
        })
    }
}

const MIN_JWT_SECRET_BYTES: usize = 32;

impl Config {
    /// Fail fast at startup on a secret too weak to sign HS256 tokens with
    /// (the `.env.example` placeholder must never reach production), and on
    /// mail settings that would silently break or weaken delivery.
    pub fn validate(&self) -> Result<()> {
        if self.jwt_secret.len() < MIN_JWT_SECRET_BYTES {
            anyhow::bail!("JWT_SECRET must be at least {MIN_JWT_SECRET_BYTES} bytes");
        }
        if self.internal_key.len() < MIN_JWT_SECRET_BYTES {
            anyhow::bail!("INTERNAL_KEY must be at least {MIN_JWT_SECRET_BYTES} bytes");
        }
        // The log mailer prints no tokens at all, but it also delivers
        // nothing: on production it would strand every reset/verification
        // request, so it is dev-only by construction.
        if self.reset_mail_log && self.cookie_secure {
            anyhow::bail!(
                "RESET_MAIL_MODE=log is a development-only mailer and must not run with COOKIE_SECURE=true"
            );
        }
        if !self.smtp_host.trim().is_empty() {
            if self.mail_from.trim().is_empty() {
                anyhow::bail!("MAIL_FROM is required when SMTP_HOST is set");
            }
            if self.public_base_url.trim().is_empty() {
                anyhow::bail!("PUBLIC_BASE_URL is required when SMTP_HOST is set");
            }
        }
        Ok(())
    }

    /// Public prefix of stored avatars. Prefers the browser-facing
    /// `AVATAR_PUBLIC_BASE_URL`; otherwise `<endpoint>/<bucket>` (dev).
    pub fn avatar_base_url(&self) -> String {
        let public = self.avatar_public_base_url.trim().trim_end_matches('/');
        if !public.is_empty() {
            return public.to_string();
        }
        format!(
            "{}/{}",
            self.s3_endpoint.trim_end_matches('/'),
            self.s3_bucket
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn config_with_secret(secret: &str) -> Config {
        Config {
            database_url: String::new(),
            jwt_secret: secret.into(),
            internal_key: "k".repeat(32),
            port: 0,
            grpc_port: 0,
            s3_endpoint: String::new(),
            s3_bucket: String::new(),
            s3_access_key: String::new(),
            s3_secret_key: String::new(),
            cookie_secure: false,
            avatar_public_base_url: String::new(),
            smtp_host: String::new(),
            smtp_port: 587,
            smtp_user: String::new(),
            smtp_password: String::new(),
            mail_from: String::new(),
            public_base_url: String::new(),
            mail_lang: Lang::default(),
            reset_mail_log: false,
        }
    }

    #[test]
    fn short_internal_key_is_rejected() {
        let mut cfg = config_with_secret(&"x".repeat(32));
        cfg.internal_key = "short".into();
        assert!(cfg.validate().is_err());
    }

    #[test]
    fn short_jwt_secret_is_rejected() {
        assert!(config_with_secret("too-short").validate().is_err());
    }

    #[test]
    fn strong_jwt_secret_is_accepted() {
        assert!(config_with_secret(&"x".repeat(32)).validate().is_ok());
    }

    #[test]
    fn log_mail_mode_is_refused_in_production() {
        let mut cfg = config_with_secret(&"x".repeat(32));
        cfg.reset_mail_log = true;
        assert!(cfg.validate().is_ok(), "log mode is fine for dev");
        cfg.cookie_secure = true;
        assert!(cfg.validate().is_err(), "log mode must not run in prod");
    }

    #[test]
    fn smtp_requires_sender_and_base_url() {
        let mut cfg = config_with_secret(&"x".repeat(32));
        cfg.smtp_host = "smtp.resend.com".into();
        assert!(cfg.validate().is_err());
        cfg.mail_from = "LoVisual <noreply@loki-code.dev>".into();
        assert!(cfg.validate().is_err());
        cfg.public_base_url = "https://visual.loki-code.dev".into();
        assert!(cfg.validate().is_ok());
    }

    #[test]
    fn mail_off_by_default_passes_validation() {
        assert!(config_with_secret(&"x".repeat(32)).validate().is_ok());
    }

    #[test]
    fn avatar_base_url_joins_endpoint_and_bucket_without_double_slash() {
        let mut cfg = config_with_secret(&"x".repeat(32));
        cfg.s3_endpoint = "http://localhost:9000/".into();
        cfg.s3_bucket = "avatars".into();
        assert_eq!(cfg.avatar_base_url(), "http://localhost:9000/avatars");
    }

    #[test]
    fn avatar_base_url_prefers_public_base_when_set() {
        let mut cfg = config_with_secret(&"x".repeat(32));
        cfg.s3_endpoint = "http://minio:9000".into();
        cfg.s3_bucket = "lovisual-avatars".into();
        cfg.avatar_public_base_url = "https://visual.loki-code.dev/api/media/".into();
        assert_eq!(
            cfg.avatar_base_url(),
            "https://visual.loki-code.dev/api/media"
        );
    }
}
