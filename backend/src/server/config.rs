//! Runtime configuration, loaded from environment variables.
//!
//! Parsed into validated types at the boundary; invalid values fail fast
//! at startup instead of blowing up mid-request.

use std::env;
use std::net::{IpAddr, SocketAddr};

use anyhow::{Context as _, Result, bail};
use axum::http::HeaderValue;
use tower_http::cors::AllowOrigin;

/// Application configuration.
#[derive(Debug, Clone)]
pub struct Config {
    pub host: IpAddr,
    pub port: u16,
    pub cors_origins: AllowOrigin,
}

impl Config {
    /// Default host for local development.
    pub const DEFAULT_HOST: &'static str = "127.0.0.1";
    /// Default port for local development.
    pub const DEFAULT_PORT: u16 = 8765;

    /// Load configuration from the environment.
    pub fn from_env() -> Result<Self> {
        Self::from_vars(
            env::var("LOVISUAL_HOST").ok(),
            env::var("LOVISUAL_PORT").ok(),
            env::var("LOVISUAL_CORS_ORIGINS").ok(),
        )
    }

    /// Build config from raw option values (injectable for tests).
    fn from_vars(host: Option<String>, port: Option<String>, cors: Option<String>) -> Result<Self> {
        let host = host
            .unwrap_or_else(|| Self::DEFAULT_HOST.to_owned())
            .parse()
            .context("LOVISUAL_HOST is not a valid IP address")?;
        let port: u16 = match port {
            Some(p) => p
                .parse()
                .context("LOVISUAL_PORT is not a valid port number")?,
            None => Self::DEFAULT_PORT,
        };
        let origins = parse_origins(&cors)?;

        Ok(Self {
            host,
            port,
            cors_origins: origins,
        })
    }

    /// Resolved listen address.
    pub fn listen_addr(&self) -> SocketAddr {
        SocketAddr::new(self.host, self.port)
    }
}

fn parse_origins(raw: &Option<String>) -> Result<AllowOrigin> {
    let Some(raw) = raw else {
        // No origins configured: allow same-origin requests only.
        return Ok(AllowOrigin::any());
    };

    let origins = raw
        .split(',')
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .collect::<Vec<_>>();
    if origins.is_empty() {
        bail!("LOVISUAL_CORS_ORIGINS is set but contains no origins");
    }

    Ok(AllowOrigin::list(
        origins
            .into_iter()
            .map(|o| o.parse::<HeaderValue>().context("invalid CORS origin"))
            .collect::<Result<Vec<_>, _>>()?,
    ))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn empty_origins_allows_any() {
        assert!(parse_origins(&None).is_ok());
    }

    #[test]
    fn empty_list_origin_string_fails() {
        assert!(parse_origins(&Some(String::from(",,"))).is_err());
    }

    #[test]
    fn invalid_port_fails() {
        let result = Config::from_vars(None, Some("not-a-port".into()), None);
        assert!(result.is_err());
    }

    #[test]
    fn invalid_host_fails() {
        let result = Config::from_vars(Some("not-an-ip".into()), None, None);
        assert!(result.is_err());
    }

    #[test]
    fn defaults_apply_when_empty() {
        let config = Config::from_vars(None, None, None).unwrap();
        assert_eq!(
            config.listen_addr(),
            SocketAddr::from(([127, 0, 0, 1], 8765))
        );
    }
}
