mod common;

use axum::http::StatusCode;
use std::sync::Arc;
use uuid::Uuid;

async fn server(devices: common::FakeDevices) -> axum_test::TestServer {
    let echo = common::spawn_echo().await;
    let app = gateway::build_app(&common::config(&echo, &echo), Arc::new(devices));
    axum_test::TestServer::new(app)
}

fn devices() -> common::FakeDevices {
    common::FakeDevices {
        token: "lvd_good".into(),
        account: Uuid::new_v4(),
    }
}

#[tokio::test]
async fn access_jwt_becomes_account_header_and_authorization_is_dropped() {
    let account = Uuid::new_v4();
    let token = common::jwt::issue_access_token(account, common::JWT_SECRET);
    let echo: serde_json::Value = server(devices())
        .await
        .get("/me")
        .authorization_bearer(token)
        .await
        .json();
    assert_eq!(echo["account_id"], account.to_string());
    assert!(echo["authorization"].is_null());
}

#[tokio::test]
async fn device_token_is_resolved_via_authenticator() {
    let d = devices();
    let account = d.account;
    let echo: serde_json::Value = server(d)
        .await
        .get("/configs")
        .authorization_bearer("lvd_good")
        .await
        .json();
    assert_eq!(echo["account_id"], account.to_string());
}

#[tokio::test]
async fn bad_credentials_are_rejected_at_the_gateway() {
    let s = server(devices()).await;
    s.get("/me")
        .authorization_bearer("lvd_bad")
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
    s.get("/me")
        .authorization_bearer("not.a.jwt")
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
    let wrong_key =
        common::jwt::issue_access_token(Uuid::new_v4(), "another-secret-another-secret-12345");
    s.get("/me")
        .authorization_bearer(wrong_key)
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn anonymous_requests_pass_without_identity() {
    let echo: serde_json::Value = server(devices()).await.post("/auth/login").await.json();
    assert!(echo["account_id"].is_null());
}

#[tokio::test]
async fn spoofed_identity_header_never_reaches_the_service() {
    let echo: serde_json::Value = server(devices())
        .await
        .get("/me")
        .add_header("x-lovisual-account-id", Uuid::new_v4().to_string())
        .await
        .json();
    assert!(echo["account_id"].is_null());
}

#[tokio::test]
async fn stale_credentials_on_auth_paths_are_treated_as_anonymous() {
    let s = server(devices()).await;
    let expired = common::expired_access_token(Uuid::new_v4());
    for token in [expired.as_str(), "lvd_bad", "not.a.jwt"] {
        let res = s.post("/auth/logout").authorization_bearer(token).await;
        res.assert_status_ok();
        let echo: serde_json::Value = res.json();
        assert_eq!(echo["path"], "/auth/logout", "reached upstream");
        assert!(echo["account_id"].is_null());
        assert!(echo["authorization"].is_null());
    }
    // Outside /auth/ the same token is still rejected.
    s.get("/me")
        .authorization_bearer(expired)
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn unreachable_device_backend_is_503() {
    let echo = common::spawn_echo().await;
    let app = gateway::build_app(&common::config(&echo, &echo), Arc::new(common::DownDevices));
    axum_test::TestServer::new(app)
        .get("/configs")
        .authorization_bearer("lvd_whatever")
        .await
        .assert_status(StatusCode::SERVICE_UNAVAILABLE);
}
