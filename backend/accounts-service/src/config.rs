use anyhow::{Context, Result};

#[derive(Clone)]
pub struct Config {
    pub database_url: String,
    pub jwt_secret: String,
    pub internal_key: String,
    pub port: u16,
    pub s3_endpoint: String,
    pub s3_bucket: String,
    pub s3_access_key: String,
    pub s3_secret_key: String,
    pub cookie_secure: bool,
}

impl Config {
    pub fn from_env() -> Result<Config> {
        Ok(Config {
            database_url: std::env::var("DATABASE_URL")
                .context("DATABASE_URL not set")?,
            jwt_secret: std::env::var("JWT_SECRET")
                .context("JWT_SECRET not set")?,
            internal_key: std::env::var("INTERNAL_KEY")
                .context("INTERNAL_KEY not set")?,
            port: std::env::var("PORT")
                .unwrap_or_else(|_| "8081".into())
                .parse()
                .context("PORT must be a number")?,
            s3_endpoint: std::env::var("S3_ENDPOINT")
                .context("S3_ENDPOINT not set")?,
            s3_bucket: std::env::var("S3_BUCKET")
                .context("S3_BUCKET not set")?,
            s3_access_key: std::env::var("S3_ACCESS_KEY")
                .context("S3_ACCESS_KEY not set")?,
            s3_secret_key: std::env::var("S3_SECRET_KEY")
                .context("S3_SECRET_KEY not set")?,
            cookie_secure: std::env::var("COOKIE_SECURE").map(|v| v != "false").unwrap_or(true),
        })
    }
}

const MIN_JWT_SECRET_BYTES: usize = 32;

impl Config {
    /// Fail fast at startup on a secret too weak to sign HS256 tokens with
    /// (the `.env.example` placeholder must never reach production).
    pub fn validate(&self) -> Result<()> {
        if self.jwt_secret.len() < MIN_JWT_SECRET_BYTES {
            anyhow::bail!("JWT_SECRET must be at least {MIN_JWT_SECRET_BYTES} bytes");
        }
        if self.internal_key.len() < MIN_JWT_SECRET_BYTES {
            anyhow::bail!("INTERNAL_KEY must be at least {MIN_JWT_SECRET_BYTES} bytes");
        }
        Ok(())
    }

    /// Public prefix of stored avatars: `<endpoint>/<bucket>`.
    pub fn avatar_base_url(&self) -> String {
        format!("{}/{}", self.s3_endpoint.trim_end_matches('/'), self.s3_bucket)
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
            s3_endpoint: String::new(),
            s3_bucket: String::new(),
            s3_access_key: String::new(),
            s3_secret_key: String::new(),
            cookie_secure: false,
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
    fn avatar_base_url_joins_endpoint_and_bucket_without_double_slash() {
        let mut cfg = config_with_secret(&"x".repeat(32));
        cfg.s3_endpoint = "http://localhost:9000/".into();
        cfg.s3_bucket = "avatars".into();
        assert_eq!(cfg.avatar_base_url(), "http://localhost:9000/avatars");
    }
}
