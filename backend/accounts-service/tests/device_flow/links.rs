use super::common;
use super::common::ACCOUNT_ID_HEADER;
use axum::http::StatusCode;

async fn link_device(server: &axum_test::TestServer, account: uuid::Uuid) -> String {
    let code: serde_json::Value = server.post("/device/code").await.json();
    server
        .post("/device/confirm")
        .add_header(ACCOUNT_ID_HEADER, account.to_string())
        .json(&serde_json::json!({ "user_code": code["user_code"] }))
        .await
        .assert_status_ok();
    let res = server
        .post("/device/token")
        .json(&serde_json::json!({ "device_code": code["device_code"] }))
        .await;
    res.assert_status_ok();
    res.json::<serde_json::Value>()["device_token"]
        .as_str()
        .unwrap()
        .to_owned()
}

#[tokio::test]
async fn confirmed_device_gets_an_opaque_token_backed_by_a_link_row() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(
        pool.clone(),
        &common::test_config(),
    ));
    let (account, _) = common::register_account(&server).await;
    let token = link_device(&server, account).await;
    assert!(token.starts_with("lvd_"));

    let resolved = accounts_service::device::links::authenticate(&pool, &token)
        .await
        .unwrap();
    assert_eq!(resolved, Some(account));
    let links: Vec<serde_json::Value> = server
        .get("/device/links")
        .add_header(ACCOUNT_ID_HEADER, account.to_string())
        .await
        .json();
    assert_eq!(links.len(), 1);
    assert!(
        links[0]["last_seen"].is_string(),
        "authenticate must bump last_seen"
    );
}

#[tokio::test]
async fn revoking_a_link_kills_its_token_only() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(
        pool.clone(),
        &common::test_config(),
    ));
    let (account, _) = common::register_account(&server).await;
    let t1 = link_device(&server, account).await;
    let t2 = link_device(&server, account).await;

    let links: Vec<serde_json::Value> = server
        .get("/device/links")
        .add_header(ACCOUNT_ID_HEADER, account.to_string())
        .await
        .json();
    let first_id = links.iter().find(|l| l["id"].is_string()).unwrap()["id"]
        .as_str()
        .unwrap()
        .to_owned();
    server
        .delete(&format!("/device/links/{first_id}"))
        .add_header(ACCOUNT_ID_HEADER, account.to_string())
        .await
        .assert_status(StatusCode::NO_CONTENT);

    let alive = [
        accounts_service::device::links::authenticate(&pool, &t1)
            .await
            .unwrap(),
        accounts_service::device::links::authenticate(&pool, &t2)
            .await
            .unwrap(),
    ];
    assert_eq!(alive.iter().filter(|a| a.is_some()).count(), 1);
}

#[tokio::test]
async fn cannot_revoke_someone_elses_link() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(pool, &common::test_config()));
    let (owner, _) = common::register_account(&server).await;
    let (stranger, _) = common::register_account(&server).await;
    link_device(&server, owner).await;
    let links: Vec<serde_json::Value> = server
        .get("/device/links")
        .add_header(ACCOUNT_ID_HEADER, owner.to_string())
        .await
        .json();
    let id = links[0]["id"].as_str().unwrap();

    server
        .delete(&format!("/device/links/{id}"))
        .add_header(ACCOUNT_ID_HEADER, stranger.to_string())
        .await
        .assert_status(StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn unknown_device_token_does_not_authenticate() {
    let pool = common::test_pool().await;
    let r = accounts_service::device::links::authenticate(&pool, "lvd_nope")
        .await
        .unwrap();
    assert_eq!(r, None);
}

#[tokio::test]
async fn self_revoke_by_token_deletes_only_that_link() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(
        pool.clone(),
        &common::test_config(),
    ));
    let (account, _) = common::register_account(&server).await;
    let t1 = link_device(&server, account).await;
    let t2 = link_device(&server, account).await;

    server
        .post("/device/revoke")
        .json(&serde_json::json!({ "device_token": t1 }))
        .await
        .assert_status(StatusCode::NO_CONTENT);
    // Unknown tokens answer the same way: no oracle.
    server
        .post("/device/revoke")
        .json(&serde_json::json!({ "device_token": "lvd_nope" }))
        .await
        .assert_status(StatusCode::NO_CONTENT);

    let gone = accounts_service::device::links::authenticate(&pool, &t1)
        .await
        .unwrap();
    let kept = accounts_service::device::links::authenticate(&pool, &t2)
        .await
        .unwrap();
    assert_eq!(gone, None);
    assert_eq!(kept, Some(account));
}
