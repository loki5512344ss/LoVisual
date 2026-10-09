//! Email verification flow: registration queues the mail, the token marks
//! the address verified once, resend re-issues, and unverified accounts are
//! locked out of device linking.

mod common;

use accounts_service::auth::reset::{Mail, MailBox};
use axum::http::StatusCode;
use axum_test::TestServer;
use serde_json::json;
use sqlx::PgPool;
use std::time::Duration;
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

/// Waits for the next verification email (the only mail these tests queue)
/// with a timeout, so a broken background task fails loudly instead of
/// hanging the suite.
async fn wait_verify_mail(mail: &mut UnboundedReceiver<Mail>) -> Mail {
    loop {
        let m = tokio::time::timeout(Duration::from_secs(5), mail.recv())
            .await
            .expect("background mail task must finish within 5s")
            .expect("queue mailer must deliver");
        if m.token.starts_with("lvev_") {
            return m;
        }
    }
}

async fn login(server: &TestServer, email: &str) -> String {
    let res = server
        .post("/auth/login")
        .json(&json!({ "email": email, "password": "correct-horse-battery-staple" }))
        .await;
    res.assert_status_ok();
    let body: serde_json::Value = res.json();
    body["access_token"].as_str().unwrap().to_owned()
}

async fn me(server: &TestServer, account_id: &str, bearer: &str) -> serde_json::Value {
    server
        .get("/me")
        .add_header(common::ACCOUNT_ID_HEADER, account_id)
        .add_header("authorization", format!("Bearer {bearer}"))
        .await
        .json::<serde_json::Value>()
}

#[tokio::test]
async fn registration_sends_mail_and_verification_round_trip_works() {
    let (server, _pool, mut mail) = app().await;
    let (account_id, email) = common::register_account(&server).await;

    // Registration answered before the mail went out; the token arrives
    // from the background task.
    let sent = wait_verify_mail(&mut mail).await;
    assert_eq!(sent.to, email);
    assert!(sent.token.starts_with("lvev_"));

    let bearer = login(&server, &email).await;
    let before = me(&server, &account_id.to_string(), &bearer).await;
    assert_eq!(before["email_verified"], false);

    server
        .post("/auth/verify-email")
        .json(&json!({ "token": sent.token }))
        .await
        .assert_status_ok();

    let after = me(&server, &account_id.to_string(), &bearer).await;
    assert_eq!(after["email_verified"], true);
}

#[tokio::test]
async fn verification_token_is_single_use_and_shape_checked() {
    let (server, _pool, mut mail) = app().await;
    common::register_account(&server).await;
    let sent = wait_verify_mail(&mut mail).await;

    server
        .post("/auth/verify-email")
        .json(&json!({ "token": sent.token }))
        .await
        .assert_status_ok();
    // Second click: burned.
    server
        .post("/auth/verify-email")
        .json(&json!({ "token": sent.token }))
        .await
        .assert_status(StatusCode::BAD_REQUEST);

    // Wrong prefix is a shape rejection, same 400 family, no oracle.
    server
        .post("/auth/verify-email")
        .json(&json!({ "token": "lvpr_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" }))
        .await
        .assert_status(StatusCode::BAD_REQUEST);
}

#[tokio::test]
async fn resend_requires_login_and_reissues_a_working_token() {
    let (server, _pool, mut mail) = app().await;
    let (account_id, email) = common::register_account(&server).await;
    let first = wait_verify_mail(&mut mail).await.token;

    // Anonymous (no identity header): 401, and no second mail.
    server
        .post("/auth/resend-verification")
        .await
        .assert_status(StatusCode::UNAUTHORIZED);

    let bearer = login(&server, &email).await;
    // The gateway resolves the bearer into the identity header; the test
    // server plays its part.
    server
        .post("/auth/resend-verification")
        .add_header(common::ACCOUNT_ID_HEADER, account_id.to_string())
        .add_header("authorization", format!("Bearer {bearer}"))
        .await
        .assert_status(StatusCode::ACCEPTED);
    let second = wait_verify_mail(&mut mail).await.token;
    assert_ne!(first, second);

    // The superseded token is dead, the fresh one works.
    server
        .post("/auth/verify-email")
        .json(&json!({ "token": first }))
        .await
        .assert_status(StatusCode::BAD_REQUEST);
    server
        .post("/auth/verify-email")
        .json(&json!({ "token": second }))
        .await
        .assert_status_ok();
}

#[tokio::test]
async fn unverified_account_cannot_link_a_device_until_verified() {
    let (server, pool, mut mail) = app().await;
    let (account_id, email) = common::register_account(&server).await;

    let code: serde_json::Value = server.post("/device/code").await.json();
    server
        .post("/device/confirm")
        .add_header(common::ACCOUNT_ID_HEADER, account_id.to_string())
        .json(&json!({ "user_code": code["user_code"] }))
        .await
        .assert_status(StatusCode::FORBIDDEN);

    // The same code is still pending: the 403 must not have consumed it.
    let sent = wait_verify_mail(&mut mail).await;
    server
        .post("/auth/verify-email")
        .json(&json!({ "token": sent.token }))
        .await
        .assert_status_ok();

    server
        .post("/device/confirm")
        .add_header(common::ACCOUNT_ID_HEADER, account_id.to_string())
        .json(&json!({ "user_code": code["user_code"] }))
        .await
        .assert_status_ok();

    sqlx::query("DELETE FROM accounts WHERE id = $1")
        .bind(account_id)
        .execute(&pool)
        .await
        .unwrap();
    let _ = email;
}

#[tokio::test]
async fn reset_and_verify_tokens_cannot_stand_in_for_each_other() {
    let (server, _pool, mut mail) = app().await;
    let (_, email) = common::register_account(&server).await;
    let verify_token = wait_verify_mail(&mut mail).await.token;

    // A reset token can never verify (shape), and this verify token can
    // never reset: the endpoint's prefix check rejects it before the DB.
    let res = server
        .post("/auth/reset-password")
        .json(&json!({ "token": verify_token, "new_password": "brand-new-password-1" }))
        .await;
    res.assert_status(StatusCode::BAD_REQUEST);

    // The login password is untouched, and the verification token still
    // works — the failed cross-use did not consume it.
    server
        .post("/auth/login")
        .json(&json!({ "email": email, "password": "correct-horse-battery-staple" }))
        .await
        .assert_status_ok();
    server
        .post("/auth/verify-email")
        .json(&json!({ "token": verify_token }))
        .await
        .assert_status_ok();
}
