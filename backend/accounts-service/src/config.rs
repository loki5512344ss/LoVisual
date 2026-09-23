use anyhow::{Context, Result};

#[derive(Clone)]
pub struct Config {
    pub database_url: String,
    pub jwt_secret: String,
    pub port: u16,
    pub s3_endpoint: String,
    pub s3_bucket: String,
    pub s3_access_key: String,
    pub s3_secret_key: String,
}

impl Config {
    pub fn from_env() -> Result<Config> {
        Ok(Config {
            database_url: std::env::var("DATABASE_URL")
                .context("DATABASE_URL not set")?,
            jwt_secret: std::env::var("JWT_SECRET")
                .context("JWT_SECRET not set")?,
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
        })
    }
}
