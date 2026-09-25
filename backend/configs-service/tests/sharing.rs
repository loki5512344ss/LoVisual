mod common;

use axum::http::StatusCode;
use serde_json::json;

async fn server() -> axum_test::TestServer {
    common::test_server(configs_service::build_app(
        common::test_pool().await,
        &common::test_config(),
        common::no_profiles(),
    ))
}

async fn save(s: &axum_test::TestServer, owner: &str) -> String {
    let r: serde_json::Value = s
        .put("/configs/1")
        .add_header(common::ACCOUNT_ID_HEADER, owner)
        .json(&json!({ "name": "legit", "data": { "k": 1 } }))
        .await
        .json();
    r["share_code"].as_str().unwrap().to_owned()
}

#[tokio::test]
async fn iddqd_is_a_god_mode_easter_egg_without_touching_the_db() {
    let s = server().await;
    for code in ["IDDQD", "iddqd", "%20%20IdDqD%20%20"] {
        let r = s.get(&format!("/configs/shared/{code}")).await;
        r.assert_status_ok();
        let body: serde_json::Value = r.json();
        assert_eq!(body["name"], "God mode");
        assert_eq!(body["data"]["modules"]["ChinaHat"]["enabled"], true);
        assert_eq!(body["updated_at"], "1993-12-10T00:00:00Z");
    }
}

#[tokio::test]
async fn anyone_can_load_by_code_case_insensitively_without_owner_leak() {
    let s = server().await;
    let code = save(&s, &common::new_account().to_string()).await;
    let res = s
        .get(&format!("/configs/shared/{}", code.to_lowercase()))
        .await;
    res.assert_status_ok();
    let body: serde_json::Value = res.json();
    assert_eq!(body["name"], "legit");
    assert_eq!(body["data"], json!({ "k": 1 }));
    assert!(body.get("account_id").is_none());
}

#[tokio::test]
async fn regenerate_invalidates_the_old_code() {
    let s = server().await;
    let owner = common::new_account().to_string();
    let old = save(&s, &owner).await;
    let res = s
        .post("/configs/1/regenerate-code")
        .add_header(common::ACCOUNT_ID_HEADER, &owner)
        .await;
    res.assert_status_ok();
    let new = res.json::<serde_json::Value>()["share_code"]
        .as_str()
        .unwrap()
        .to_owned();
    assert_ne!(old, new);
    s.get(&format!("/configs/shared/{old}"))
        .await
        .assert_status(StatusCode::NOT_FOUND);
    s.get(&format!("/configs/shared/{new}"))
        .await
        .assert_status_ok();
}

#[tokio::test]
async fn regenerate_on_empty_slot_is_404_and_needs_identity() {
    let s = server().await;
    s.post("/configs/4/regenerate-code")
        .add_header(common::ACCOUNT_ID_HEADER, common::new_account().to_string())
        .await
        .assert_status(StatusCode::NOT_FOUND);
    s.post("/configs/4/regenerate-code")
        .await
        .assert_status_unauthorized();
}

#[tokio::test]
async fn unknown_code_is_404() {
    server()
        .await
        .get("/configs/shared/ZZZZZZZZ")
        .await
        .assert_status(StatusCode::NOT_FOUND);
}
