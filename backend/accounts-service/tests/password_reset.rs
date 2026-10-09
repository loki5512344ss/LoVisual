mod common;

use accounts_service::auth::reset::{Mail, MailBox};
use axum::http::StatusCode;
use axum_test::TestServer;
use sqlx::PgPool;
use tokio::sync::mpsc::{UnboundedReceiver, unbounded_channel};

type App = (TestServer, PgPool, UnboundedReceiver<Mail>);

async fn app() -> App {
    let (tx, rx) = unbounded_channel();
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app_with_mail(
        pool.clone(),
        &common::test_config(),
        MailBox::Queue(tx),
    ));
    (server, pool, rx)
}

async fn request_reset(server: &TestServer, email: &str) {
    server
        .post("/auth/forgot-password")
        .json(&serde_json::json!({ "email": email }))
        .await
        .assert_status(StatusCode::ACCEPTED);
}

async fn reset_with(server: &TestServer, token: &str, password: &str) -> axum_test::TestResponse {
    server
        .post("/auth/reset-password")
        .json(&serde_json::json!({ "token": token, "new_password": password }))
        .await
}

#[tokio::test]
async fn forgot_password_never_reveals_account_existence() {
    let (server, _pool, _mail) = app().await;

    // Unknown email and known email must be indistinguishable: same status,
    // same body. Enumeration is the first step of account takeover.
    let unknown = server
        .post("/auth/forgot-password")
        .json(&serde_json::json!({ "email": "nobody-here@example.com" }))
        .await;
    unknown.assert_status(StatusCode::ACCEPTED);
    let unknown_body: serde_json::Value = unknown.json();
    assert_eq!(
        unknown_body["status"],
        "reset email sent if the account exists"
    );

    let (_, email) = common::register_account(&server).await;
    let known = server
        .post("/auth/forgot-password")
        .json(&serde_json::json!({ "email": email }))
        .await;
    known.assert_status(StatusCode::ACCEPTED);
    let known_body: serde_json::Value = known.json();
    assert_eq!(unknown_body, known_body);
}

#[tokio::test]
async fn full_reset_flow_changes_password_and_kills_sessions() {
    let (server, _pool, mut mail) = app().await;
    let (_, email) = common::register_account(&server).await;
    let old_password = "correct-horse-battery-staple";

    // Log in to create a live refresh session the reset must kill.
    let login = server
        .post("/auth/login")
        .json(&serde_json::json!({ "email": email, "password": old_password }))
        .await;
    login.assert_status_ok();
    let old_refresh = refresh_cookie(&login).expect("login must set the refresh cookie");

    request_reset(&server, &email).await;
    let token = mail.recv().await.expect("queue mailer must deliver").token;

    reset_with(&server, &token, "brand-new-password-1")
        .await
        .assert_status_ok();

    // Old password is dead, new password works.
    server
        .post("/auth/login")
        .json(&serde_json::json!({ "email": email, "password": old_password }))
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
    server
        .post("/auth/login")
        .json(&serde_json::json!({ "email": email, "password": "brand-new-password-1" }))
        .await
        .assert_status_ok();

    // Every refresh session issued before the reset is revoked: replaying
    // the pre-reset cookie must not survive (a stolen session cannot
    // outlive a password change).
    let replay = server
        .post("/auth/refresh")
        .add_cookie(old_refresh.as_str().into())
        .await;
    replay.assert_status(StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn reset_token_is_single_use() {
    let (server, _pool, mut mail) = app().await;
    let (_, email) = common::register_account(&server).await;

    request_reset(&server, &email).await;
    let token = mail.recv().await.expect("mail").token;

    reset_with(&server, &token, "brand-new-password-1")
        .await
        .assert_status_ok();
    reset_with(&server, &token, "another-password-2")
        .await
        .assert_status(StatusCode::BAD_REQUEST);
}

#[tokio::test]
async fn new_request_supersedes_pending_token() {
    let (server, _pool, mut mail) = app().await;
    let (_, email) = common::register_account(&server).await;

    request_reset(&server, &email).await;
    let first = mail.recv().await.expect("mail").token;
    request_reset(&server, &email).await;
    let second = mail.recv().await.expect("mail").token;
    assert_ne!(first, second);

    // The superseded token no longer works, the fresh one does.
    reset_with(&server, &first, "brand-new-password-1")
        .await
        .assert_status(StatusCode::BAD_REQUEST);
    reset_with(&server, &second, "brand-new-password-1")
        .await
        .assert_status_ok();
}

#[tokio::test]
async fn bad_tokens_are_rejected_without_oracle() {
    let (server, _pool, _mail) = app().await;

    server
        .post("/auth/reset-password")
        .json(&serde_json::json!({ "token": "short", "new_password": "brand-new-password-1" }))
        .await
        .assert_status(StatusCode::BAD_REQUEST);

    // Unknown but well-formed token: same 400 family, and unlike the
    // shape-rejection above it must not leak which of unknown/expired/used
    // it is.
    let unknown = server
        .post("/auth/reset-password")
        .json(&serde_json::json!({
            "token": "lvpr_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "new_password": "brand-new-password-1"
        }))
        .await;
    unknown.assert_status(StatusCode::BAD_REQUEST);
    let body: serde_json::Value = unknown.json();
    assert_eq!(body["error"], "reset token is invalid or expired");
}

#[tokio::test]
async fn weak_password_is_rejected_before_token_consumption() {
    let (server, _pool, mut mail) = app().await;
    let (_, email) = common::register_account(&server).await;

    request_reset(&server, &email).await;
    let token = mail.recv().await.expect("mail").token;

    reset_with(&server, &token, "short12")
        .await
        .assert_status(StatusCode::BAD_REQUEST);

    // The token must still be usable afterwards: rejecting a weak password
    // must not burn the user's one link.
    reset_with(&server, &token, "brand-new-password-1")
        .await
        .assert_status_ok();
}

#[tokio::test]
async fn reset_mail_only_goes_to_known_accounts() {
    let (server, _pool, mut mail) = app().await;

    request_reset(&server, "ghost@example.com").await;
    assert!(
        mail.try_recv().is_err(),
        "unknown account must not enqueue a reset email"
    );

    let (_, email) = common::register_account(&server).await;
    request_reset(&server, &email).await;
    let sent = mail.recv().await.expect("mail");
    assert_eq!(sent.to, email);
    assert!(sent.token.starts_with("lvpr_"));
}

/// The refresh cookie is httpOnly and scoped to /auth; axum-test exposes
/// response headers, so parse Set-Cookie directly.
fn refresh_cookie(res: &axum_test::TestResponse) -> Option<String> {
    res.headers()
        .get_all(axum::http::header::SET_COOKIE)
        .iter()
        .find_map(|v| {
            let s = v.to_str().ok()?;
            s.strip_prefix("lv_refresh=")
                .map(|rest| format!("lv_refresh={}", rest.split(';').next().unwrap_or("")))
        })
}
