//! Minimal client for the LoVisual backend API.

use anyhow::Context as _;
use serde::Deserialize;

/// Default backend base URL for local development.
pub const DEFAULT_API_URL: &str = "http://127.0.0.1:8765";

/// Health response from `GET /health`.
#[derive(Debug, Deserialize)]
pub struct Health {
    pub status: String,
    pub version: String,
}

/// Fetch backend health status.
pub async fn health(base_url: &str) -> anyhow::Result<Health> {
    let client = reqwest::Client::builder()
        .timeout(std::time::Duration::from_secs(5))
        .build()
        .context("failed to build http client")?;

    let url = format!("{}/health", base_url.trim_end_matches('/'));
    let response = client
        .get(&url)
        .send()
        .await
        .context("health request failed")?;
    response
        .error_for_status()
        .context("health returned error status")?
        .json()
        .await
        .context("invalid health response")
}
