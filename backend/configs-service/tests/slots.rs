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

#[tokio::test]
async fn health_ok_and_api_requires_internal_key() {
    let pool = common::test_pool().await;
    let raw = axum_test::TestServer::new(configs_service::build_app(
        pool,
        &common::test_config(),
        common::no_profiles(),
    ));
    raw.get("/health").await.assert_status_ok();
    raw.get("/configs")
        .await
        .assert_status(StatusCode::FORBIDDEN);
}

#[tokio::test]
async fn save_then_get_then_list() {
    let s = server().await;
    let me = common::new_account();
    let data = json!({ "modules": { "Hud": { "enabled": true } } });

    let saved = s
        .put("/configs/2")
        .add_header(common::ACCOUNT_ID_HEADER, me.to_string())
        .json(&json!({ "name": "  pvp  ", "data": data }))
        .await;
    saved.assert_status_ok();
    let saved: serde_json::Value = saved.json();
    assert_eq!(saved["name"], "pvp");
    assert_eq!(saved["share_code"].as_str().unwrap().len(), 8);

    let got: serde_json::Value = s
        .get("/configs/2")
        .add_header(common::ACCOUNT_ID_HEADER, me.to_string())
        .await
        .json();
    assert_eq!(got["data"], data);

    let list: Vec<serde_json::Value> = s
        .get("/configs")
        .add_header(common::ACCOUNT_ID_HEADER, me.to_string())
        .await
        .json();
    assert_eq!(list.len(), 1);
    assert_eq!(list[0]["slot"], 2);
    assert_eq!(list[0]["published"], false);
    assert!(
        list[0].get("data").is_none(),
        "list must not ship full configs"
    );
}

#[tokio::test]
async fn resave_keeps_share_code_and_overwrites_data() {
    let s = server().await;
    let me = common::new_account().to_string();
    let first: serde_json::Value = s
        .put("/configs/1")
        .add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "name": "a", "data": 1 }))
        .await
        .json();
    let second: serde_json::Value = s
        .put("/configs/1")
        .add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "name": "b", "data": 2 }))
        .await
        .json();
    assert_eq!(first["share_code"], second["share_code"]);
    assert_eq!(second["data"], 2);
}

#[tokio::test]
async fn validation_errors() {
    let s = server().await;
    let me = common::new_account().to_string();
    for slot in ["0", "5"] {
        s.put(&format!("/configs/{slot}"))
            .add_header(common::ACCOUNT_ID_HEADER, &me)
            .json(&json!({ "name": "x", "data": {} }))
            .await
            .assert_status(StatusCode::BAD_REQUEST);
    }
    s.put("/configs/1")
        .add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "name": "   ", "data": {} }))
        .await
        .assert_status(StatusCode::BAD_REQUEST);
    s.put("/configs/1")
        .add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "name": "x".repeat(33), "data": {} }))
        .await
        .assert_status(StatusCode::BAD_REQUEST);
    let huge = "x".repeat(256 * 1024 + 1);
    s.put("/configs/1")
        .add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "name": "x", "data": huge }))
        .await
        .assert_status(StatusCode::BAD_REQUEST);
}

#[tokio::test]
async fn slots_are_private_and_need_identity() {
    let s = server().await;
    let owner = common::new_account().to_string();
    s.put("/configs/3")
        .add_header(common::ACCOUNT_ID_HEADER, &owner)
        .json(&json!({ "name": "x", "data": {} }))
        .await
        .assert_status_ok();
    s.get("/configs/3")
        .add_header(common::ACCOUNT_ID_HEADER, common::new_account().to_string())
        .await
        .assert_status(StatusCode::NOT_FOUND);
    s.get("/configs").await.assert_status_unauthorized();
}
