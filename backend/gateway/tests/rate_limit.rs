mod common;

use axum::http::StatusCode;

async fn server() -> axum_test::TestServer {
    let echo = common::spawn_echo().await;
    axum_test::TestServer::new(gateway::build_app(
        &common::config(&echo, &echo),
        common::no_devices(),
    ))
}

#[tokio::test]
async fn sixth_login_in_a_minute_from_one_ip_is_429_with_retry_after() {
    let s = server().await;
    for _ in 0..5 {
        s.post("/auth/login")
            .add_header("x-forwarded-for", "203.0.113.7")
            .await
            .assert_status_ok();
    }
    let res = s
        .post("/auth/login")
        .add_header("x-forwarded-for", "203.0.113.7")
        .await;
    res.assert_status(StatusCode::TOO_MANY_REQUESTS);
    let retry: u64 = res.header("retry-after").to_str().unwrap().parse().unwrap();
    assert!((1..=60).contains(&retry));
}

#[tokio::test]
async fn limits_are_per_ip() {
    let s = server().await;
    for _ in 0..5 {
        s.post("/auth/login")
            .add_header("x-forwarded-for", "203.0.113.8")
            .await;
    }
    s.post("/auth/login")
        .add_header("x-forwarded-for", "203.0.113.9")
        .await
        .assert_status_ok();
}

#[tokio::test]
async fn rightmost_forwarded_for_entry_is_used() {
    // A client can prepend fake entries; only the one our proxy appended counts.
    let s = server().await;
    for i in 0..5 {
        s.post("/auth/login")
            .add_header("x-forwarded-for", format!("10.0.0.{i}, 203.0.113.10"))
            .await
            .assert_status_ok();
    }
    s.post("/auth/login")
        .add_header("x-forwarded-for", "1.1.1.1, 203.0.113.10")
        .await
        .assert_status(StatusCode::TOO_MANY_REQUESTS);
}

#[tokio::test]
async fn failed_credentials_count_against_the_ip_limit() {
    // Every bad device token costs accounts-service a gRPC call + DB query,
    // so the per-IP global limit must apply before identity rejects it.
    let s = server().await;
    for _ in 0..300 {
        s.get("/configs")
            .authorization_bearer("lvd_bad")
            .add_header("x-forwarded-for", "203.0.113.20")
            .await
            .assert_status(StatusCode::UNAUTHORIZED);
    }
    s.get("/configs")
        .authorization_bearer("lvd_bad")
        .add_header("x-forwarded-for", "203.0.113.20")
        .await
        .assert_status(StatusCode::TOO_MANY_REQUESTS);
}
