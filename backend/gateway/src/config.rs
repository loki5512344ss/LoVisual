use anyhow::{Context, Result};

#[derive(Clone)]
pub struct Config {
    pub port: u16,
    pub jwt_secret: String,
    pub internal_key: String,
    pub accounts_http_url: String,
    pub accounts_grpc_url: String,
    pub configs_http_url: String,
    pub site_origin: String,
    /// Behind a reverse proxy (nginx/caddy) that appends the client IP to
    /// X-Forwarded-For. Never enable when the gateway is exposed directly.
    pub trust_proxy: bool,
}

fn var(name: &str) -> Result<String> {
    std::env::var(name).with_context(|| format!("{name} not set"))
}

impl Config {
    pub fn from_env() -> Result<Config> {
        Ok(Config {
            port: std::env::var("GATEWAY_PORT")
                .unwrap_or_else(|_| "8080".into())
                .parse()
                .context("GATEWAY_PORT")?,
            jwt_secret: var("JWT_SECRET")?,
            internal_key: var("INTERNAL_KEY")?,
            accounts_http_url: var("ACCOUNTS_HTTP_URL")?,
            accounts_grpc_url: var("ACCOUNTS_GRPC_URL")?,
            configs_http_url: var("CONFIGS_HTTP_URL")?,
            site_origin: var("SITE_ORIGIN")?,
            trust_proxy: std::env::var("TRUST_PROXY").is_ok_and(|v| v == "true"),
        })
    }

    pub fn validate(&self) -> Result<()> {
        for (name, value) in [
            ("JWT_SECRET", &self.jwt_secret),
            ("INTERNAL_KEY", &self.internal_key),
        ] {
            if value.len() < 32 {
                anyhow::bail!("{name} must be at least 32 bytes");
            }
        }
        // Both are sent as raw header values (INTERNAL_KEY on every upstream
        // call, SITE_ORIGIN in the CORS layer); checked here so a bad value
        // is a startup error, not a panic deep in request handling.
        axum::http::HeaderValue::from_str(&self.internal_key)
            .context("INTERNAL_KEY is not a valid header value")?;
        axum::http::HeaderValue::from_str(&self.site_origin)
            .context("SITE_ORIGIN is not a valid header value")?;
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    pub fn sample() -> Config {
        Config {
            port: 0,
            jwt_secret: "j".repeat(32),
            internal_key: "k".repeat(32),
            accounts_http_url: String::new(),
            accounts_grpc_url: String::new(),
            configs_http_url: String::new(),
            site_origin: "http://localhost:5173".into(),
            trust_proxy: false,
        }
    }

    #[test]
    fn valid_config_passes() {
        assert!(sample().validate().is_ok());
    }

    #[test]
    fn weak_secrets_are_rejected() {
        let mut c = sample();
        c.internal_key = "short".into();
        assert!(c.validate().is_err());
        let mut c = sample();
        c.jwt_secret = "short".into();
        assert!(c.validate().is_err());
    }
}
