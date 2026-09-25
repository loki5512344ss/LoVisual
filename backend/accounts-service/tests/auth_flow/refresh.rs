use super::common;
use axum::http::StatusCode;
use axum_extra::extract::cookie::Cookie;
use serde_json::json;

async fn login(server: &axum_test::TestServer, email: &str) -> (String, String) {
    let res = server
        .post("/auth/login")
        .json(&json!({ "email": email, "password": "correct-horse-battery-staple" }))
        .await;
    res.assert_status_ok();
    let cookie = res.cookie("lv_refresh");
    assert!(cookie.http_only().unwrap_or(false));
    assert_eq!(cookie.path(), Some("/auth"));
    let body: serde_json::Value = res.json();
    assert!(
        body.get("refresh_token").is_none(),
        "refresh token must not be in the JSON body"
    );
    (
        body["access_token"].as_str().unwrap().to_owned(),
        cookie.value().to_owned(),
    )
}

#[tokio::test]
async fn refresh_rotates_the_cookie_and_issues_a_new_access_token() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (_, email) = common::register_account(&server).await;
    let (_, refresh) = login(&server, &email).await;

    let res = server
        .post("/auth/refresh")
        .add_cookie(Cookie::new("lv_refresh", refresh.clone()))
        .await;
    res.assert_status_ok();
    assert!(res.json::<serde_json::Value>()["access_token"].is_string());
    assert_ne!(res.cookie("lv_refresh").value(), refresh);
}

#[tokio::test]
async fn reusing_a_rotated_refresh_token_revokes_all_sessions() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (_, email) = common::register_account(&server).await;
    let (_, first) = login(&server, &email).await;

    let second = server
        .post("/auth/refresh")
        .add_cookie(Cookie::new("lv_refresh", first.clone()))
        .await
        .cookie("lv_refresh")
        .value()
        .to_owned();
    // Wait out the rotation grace window: a replay this long after rotation
    // is genuine reuse, not a benign concurrent-refresh race.
    tokio::time::sleep(std::time::Duration::from_secs(11)).await;
    // Attacker replays the old token -> rejected, and the legit new one dies too.
    server
        .post("/auth/refresh")
        .add_cookie(Cookie::new("lv_refresh", first))
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
    server
        .post("/auth/refresh")
        .add_cookie(Cookie::new("lv_refresh", second))
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn concurrent_refresh_within_the_grace_window_does_not_kill_the_session() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (_, email) = common::register_account(&server).await;
    let (_, first) = login(&server, &email).await;

    // Two tabs racing to refresh the same cookie: the first wins and rotates.
    let second = server
        .post("/auth/refresh")
        .add_cookie(Cookie::new("lv_refresh", first.clone()))
        .await
        .cookie("lv_refresh")
        .value()
        .to_owned();
    // The second tab's request lands moments later with the now-stale cookie:
    // it must be rejected...
    server
        .post("/auth/refresh")
        .add_cookie(Cookie::new("lv_refresh", first))
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
    // ...but the first tab's freshly-rotated token must keep working.
    server
        .post("/auth/refresh")
        .add_cookie(Cookie::new("lv_refresh", second))
        .await
        .assert_status_ok();
}

#[tokio::test]
async fn logout_revokes_the_refresh_token() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (_, email) = common::register_account(&server).await;
    let (_, refresh) = login(&server, &email).await;

    let res = server
        .post("/auth/logout")
        .add_cookie(Cookie::new("lv_refresh", refresh.clone()))
        .await;
    res.assert_status(StatusCode::NO_CONTENT);
    let removal = res.cookie("lv_refresh");
    assert_eq!(removal.value(), "");
    assert_eq!(removal.max_age(), Some(time::Duration::ZERO));
    server
        .post("/auth/refresh")
        .add_cookie(Cookie::new("lv_refresh", refresh))
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn refresh_without_cookie_or_with_garbage_is_401() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    server
        .post("/auth/refresh")
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
    server
        .post("/auth/refresh")
        .add_cookie(Cookie::new("lv_refresh", "lvr_garbage"))
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
}
