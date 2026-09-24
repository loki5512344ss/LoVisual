mod common;

use axum::http::StatusCode;
use serde_json::json;

#[tokio::test]
async fn full_device_link_flow() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool.clone(), &common::test_config()));
    let (account_id, email) = common::register_account(&server).await;

    let code_response: serde_json::Value = server.post("/device/code").await.json();
    let device_code = code_response["device_code"].as_str().unwrap();
    let user_code = code_response["user_code"].as_str().unwrap();

    let poll_before = server
        .post("/device/token")
        .json(&json!({ "device_code": device_code }))
        .await;
    poll_before.assert_status(StatusCode::ACCEPTED);

    server
        .post("/device/confirm")
        .add_header(common::ACCOUNT_ID_HEADER, account_id.to_string())
        .json(&json!({ "user_code": user_code }))
        .await
        .assert_status_ok();

    let poll_after = server
        .post("/device/token")
        .json(&json!({ "device_code": device_code }))
        .await;
    poll_after.assert_status_ok();
    let token_body: serde_json::Value = poll_after.json();
    assert!(token_body["device_token"].is_string());

    // Single use: the same device_code cannot be redeemed twice.
    server
        .post("/device/token")
        .json(&json!({ "device_code": device_code }))
        .await
        .assert_status(StatusCode::NOT_FOUND);

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn confirm_without_identity_is_401() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool.clone(), &common::test_config()));
    let code: serde_json::Value = server.post("/device/code").await.json();
    server
        .post("/device/confirm")
        .json(&json!({ "user_code": code["user_code"] }))
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn unknown_device_code_and_user_code_return_404() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool.clone(), &common::test_config()));
    let (account_id, email) = common::register_account(&server).await;

    server
        .post("/device/token")
        .json(&json!({ "device_code": "no-such-code" }))
        .await
        .assert_status(StatusCode::NOT_FOUND);

    server
        .post("/device/confirm")
        .add_header(common::ACCOUNT_ID_HEADER, account_id.to_string())
        .json(&json!({ "user_code": "ZZZZ-ZZZZ" }))
        .await
        .assert_status(StatusCode::NOT_FOUND);

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}
