mod common;

use axum::http::StatusCode;

async fn app() -> axum::Router {
    let echo = common::spawn_echo().await;
    gateway::build_app(&common::config(&echo, &echo), common::no_devices())
}

#[tokio::test]
async fn dot_segments_encoded_slashes_and_empty_segments_are_400() {
    let app = app().await;
    for uri in [
        "/auth/x/../login",
        "/auth/./login",
        "/auth/%2e%2e/device/token",
        "/auth/%2E%2E/login",
        "/auth/x/.%2e/login",
        "/auth/x/%2e./login",
        "/auth/%2e/login",
        "/configs%2f1",
        "/configs/1%5Cx",
        "/configs/1\\..\\2",
        "//evil.com/x",
        "/auth//login",
    ] {
        let (status, _, body) = common::raw(&app, "POST", uri, "203.0.113.50").await;
        assert_eq!(status, StatusCode::BAD_REQUEST, "{uri}");
        assert!(body.contains("bad path"), "{uri}: {body}");
    }
}

#[tokio::test]
async fn normal_paths_are_unaffected() {
    let app = app().await;
    for uri in ["/auth/login", "/configs/2?x=../y", "/showcase", "/health"] {
        let (status, _, _) = common::raw(&app, "POST", uri, "203.0.113.51").await;
        assert_ne!(status, StatusCode::BAD_REQUEST, "{uri}");
    }
    let (status, _, _) = common::raw(&app, "GET", "/", "203.0.113.51").await;
    assert_eq!(status, StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn traversal_cannot_dodge_the_login_limit() {
    let app = app().await;
    for _ in 0..8 {
        let (status, _, _) = common::raw(&app, "POST", "/auth/x/../login", "203.0.113.52").await;
        assert_eq!(status, StatusCode::BAD_REQUEST);
    }
    for _ in 0..5 {
        let (status, _, _) = common::raw(&app, "POST", "/auth/login", "203.0.113.52").await;
        assert_eq!(status, StatusCode::OK);
    }
    let (status, _, _) = common::raw(&app, "POST", "/auth/login", "203.0.113.52").await;
    assert_eq!(status, StatusCode::TOO_MANY_REQUESTS);
}

#[tokio::test]
async fn env_probes_get_the_honeypot_instead_of_400() {
    let app = app().await;
    for uri in [
        "/../../.env",
        "/%2e%2e/.env",
        "/.env",
        "/api/.env",
        "/.env.local",
        "/.env.production",
    ] {
        let (status, headers, body) = common::raw(&app, "GET", uri, "203.0.113.53").await;
        assert_eq!(status, StatusCode::OK, "{uri}");
        assert_eq!(headers["content-type"], "text/plain; charset=utf-8");
        assert!(body.contains("nice_try_skiddie"), "{uri}: {body}");
        assert!(!body.contains("\"path\""), "never forwarded upstream");
    }
}

#[tokio::test]
async fn coffee_is_a_teapot_for_every_method() {
    let app = app().await;
    for method in ["GET", "POST", "PUT"] {
        let (status, headers, body) = common::raw(&app, method, "/coffee", "203.0.113.54").await;
        assert_eq!(status, StatusCode::IM_A_TEAPOT, "{method}");
        assert_eq!(headers["content-type"], "text/plain; charset=utf-8");
        assert!(body.contains("Я чайник"), "{method}: {body}");
        assert!(
            body.contains("https://github.com/loki5512344ss/LoVisual/releases"),
            "{method}: {body}"
        );
        assert!(!body.contains("\"path\""), "never forwarded upstream");
    }
}
