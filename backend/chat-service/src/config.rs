use anyhow::{Context, Result};

#[derive(Clone)]
pub struct Config {
    pub port: u16,
    pub internal_key: String,
}

impl Config {
    pub fn from_env() -> Result<Config> {
        Ok(Config {
            port: std::env::var("CHAT_PORT")
                .unwrap_or_else(|_| "8083".into())
                .parse()
                .context("CHAT_PORT")?,
            internal_key: std::env::var("INTERNAL_KEY").context("INTERNAL_KEY not set")?,
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

    #[test]
    fn short_key_is_rejected() {
        let c = Config {
            port: 0,
            internal_key: "short".into(),
        };
        assert!(c.validate().is_err());
        let c = Config {
            port: 0,
            internal_key: "k".repeat(32),
        };
        assert!(c.validate().is_ok());
    }
}
