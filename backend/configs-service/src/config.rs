use anyhow::{Context, Result};

#[derive(Clone)]
pub struct Config {
    pub database_url: String,
    pub port: u16,
    pub internal_key: String,
    pub accounts_grpc_url: String,
}

impl Config {
    pub fn from_env() -> Result<Config> {
        Ok(Config {
            database_url: std::env::var("CONFIGS_DATABASE_URL")
                .context("CONFIGS_DATABASE_URL not set")?,
            port: std::env::var("CONFIGS_PORT")
                .unwrap_or_else(|_| "8082".into())
                .parse()
                .context("CONFIGS_PORT")?,
            internal_key: std::env::var("INTERNAL_KEY").context("INTERNAL_KEY not set")?,
            accounts_grpc_url: std::env::var("ACCOUNTS_GRPC_URL")
                .context("ACCOUNTS_GRPC_URL not set")?,
        })
    }

    pub fn validate(&self) -> Result<()> {
        if self.internal_key.len() < 32 {
            anyhow::bail!("INTERNAL_KEY must be at least 32 bytes");
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn sample(key: &str) -> Config {
        Config {
            database_url: String::new(),
            port: 0,
            internal_key: key.into(),
            accounts_grpc_url: String::new(),
        }
    }

    #[test]
    fn strong_key_passes() {
        assert!(sample(&"k".repeat(32)).validate().is_ok());
    }

    #[test]
    fn short_key_is_rejected() {
        assert!(sample("short").validate().is_err());
    }
}
