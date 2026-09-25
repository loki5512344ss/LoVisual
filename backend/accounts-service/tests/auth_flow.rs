use serde_json::json;
use uuid::Uuid;

mod common;
// A test root's submodules resolve against `tests/`, not the file's own
// directory — pin the path so `tests/` itself stays at 4 files.
#[path = "auth_flow/refresh.rs"]
mod refresh;

#[tokio::test]
async fn register_then_login_succeeds() {
    let pool = common::test_pool().await;
    let app = accounts_service::build_app(pool.clone(), &common::test_config());
    let server = common::test_server(app);
    let email = format!("flow-{}@example.com", Uuid::new_v4());

    let register_response = server
        .post("/auth/register")
        .json(
            &json!({ "email": email, "password": "correct-horse-battery-staple", "nick": "Rider" }),
        )
        .await;
    register_response.assert_status(axum::http::StatusCode::CREATED);

    let login_response = server
        .post("/auth/login")
        .json(&json!({ "email": email, "password": "correct-horse-battery-staple" }))
        .await;
    login_response.assert_status_ok();
    let body: serde_json::Value = login_response.json();
    assert!(body["access_token"].is_string());
    assert!(
        body["refresh_token"].is_null(),
        "refresh token must be in the httpOnly cookie, not the body"
    );

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn login_with_wrong_password_returns_401() {
    let pool = common::test_pool().await;
    let app = accounts_service::build_app(pool.clone(), &common::test_config());
    let server = common::test_server(app);
    let email = format!("wrongpw-{}@example.com", Uuid::new_v4());

    server
        .post("/auth/register")
        .json(&json!({ "email": email, "password": "right-password", "nick": "Rider" }))
        .await
        .assert_status(axum::http::StatusCode::CREATED);

    let login_response = server
        .post("/auth/login")
        .json(&json!({ "email": email, "password": "wrong-password" }))
        .await;
    login_response.assert_status(axum::http::StatusCode::UNAUTHORIZED);

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn duplicate_email_registration_returns_409() {
    let pool = common::test_pool().await;
    let app = accounts_service::build_app(pool.clone(), &common::test_config());
    let server = common::test_server(app);
    let email = format!("dup-{}@example.com", Uuid::new_v4());

    server
        .post("/auth/register")
        .json(&json!({ "email": email, "password": "password123", "nick": "First" }))
        .await
        .assert_status(axum::http::StatusCode::CREATED);

    let second = server
        .post("/auth/register")
        .json(&json!({ "email": email, "password": "password456", "nick": "Second" }))
        .await;
    second.assert_status(axum::http::StatusCode::CONFLICT);

    sqlx::query("DELETE FROM accounts WHERE email = $1")
        .bind(&email)
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn login_with_unknown_email_returns_401() {
    let pool = common::test_pool().await;
    let app = accounts_service::build_app(pool.clone(), &common::test_config());
    let server = common::test_server(app);

    let response = server
        .post("/auth/login")
        .json(&json!({ "email": format!("nobody-{}@example.com", Uuid::new_v4()), "password": "whatever-123" }))
        .await;
    response.assert_status(axum::http::StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn login_is_case_insensitive_on_email() {
    let pool = common::test_pool().await;
    let app = accounts_service::build_app(pool.clone(), &common::test_config());
    let server = common::test_server(app);
    let tag = Uuid::new_v4();
    let registered = format!("CaseUser-{tag}@Example.com");

    server
        .post("/auth/register")
        .json(&json!({ "email": registered, "password": "password123", "nick": "Rider" }))
        .await
        .assert_status(axum::http::StatusCode::CREATED);

    server
        .post("/auth/login")
        .json(&json!({ "email": registered.to_lowercase(), "password": "password123" }))
        .await
        .assert_status_ok();

    sqlx::query("DELETE FROM accounts WHERE lower(email) = $1")
        .bind(registered.to_lowercase())
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn malformed_json_body_returns_400_with_json_error_shape() {
    let pool = common::test_pool().await;
    let app = accounts_service::build_app(pool.clone(), &common::test_config());
    let server = common::test_server(app);

    let response = server
        .post("/auth/register")
        .content_type("application/json")
        .bytes(axum::body::Bytes::from_static(b"{ this is not valid json"))
        .await;
    response.assert_status(axum::http::StatusCode::BAD_REQUEST);
    let body: serde_json::Value = response.json();
    assert!(
        body["error"].is_string(),
        "expected {{\"error\": ...}}, got {body}"
    );
}

#[tokio::test]
async fn register_rejects_oversized_password_with_400() {
    let pool = common::test_pool().await;
    let app = accounts_service::build_app(pool.clone(), &common::test_config());
    let server = common::test_server(app);

    let response = server
        .post("/auth/register")
        .json(&json!({
            "email": format!("big-{}@example.com", Uuid::new_v4()),
            "password": "a".repeat(10_000),
            "nick": "Rider"
        }))
        .await;
    response.assert_status(axum::http::StatusCode::BAD_REQUEST);
}

#[tokio::test]
async fn me_returns_profile_without_password_hash() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (id, email) = common::register_account(&server).await;

    let res = server
        .get("/me")
        .add_header(common::ACCOUNT_ID_HEADER, id.to_string())
        .await;
    res.assert_status_ok();
    let body: serde_json::Value = res.json();
    assert_eq!(body["email"], email);
    assert_eq!(body["display_nick"], "Tester");
    assert_eq!(body["role"], "user");
    assert!(body["avatar_url"].is_null());
    assert!(body.get("password_hash").is_none());

    server.get("/me").await.assert_status_unauthorized();
}
