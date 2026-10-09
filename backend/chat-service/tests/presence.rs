use axum_test::TestServer;
use chat_service::config::Config;
use common::internal::{ACCOUNT_ID_HEADER, INTERNAL_KEY_HEADER};
use serde_json::{Value, json};
use uuid::Uuid;

const KEY: &str = "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk";

fn server() -> TestServer {
    let cfg = Config {
        port: 0,
        internal_key: KEY.into(),
    };
    let (app, _) = chat_service::build_app(&cfg);
    TestServer::new(app)
}

async fn sync(s: &TestServer, account: u128, body: Value) -> axum_test::TestResponse {
    s.post("/presence/gui")
        .add_header(INTERNAL_KEY_HEADER, KEY)
        .add_header(ACCOUNT_ID_HEADER, Uuid::from_u128(account).to_string())
        .json(&body)
        .await
}

#[tokio::test]
async fn requires_internal_key_and_identity() {
    let s = server();
    let r = s.post("/presence/gui").json(&json!({})).await;
    assert_eq!(r.status_code(), 403);
    let r = s
        .post("/presence/gui")
        .add_header(INTERNAL_KEY_HEADER, KEY)
        .json(&json!({"server": "a", "mc": Uuid::nil()}))
        .await;
    assert_eq!(r.status_code(), 401);
}

#[tokio::test]
async fn two_players_see_each_other() {
    let s = server();
    let (a, b) = (Uuid::from_u128(10), Uuid::from_u128(20));
    let r = sync(
        &s,
        1,
        json!({"server": "Play.X", "mc": a, "gui": "AQEAAAA=", "want": [b]}),
    )
    .await;
    assert_eq!(r.status_code(), 200);
    assert_eq!(r.json::<Value>()["players"].as_array().unwrap().len(), 0);
    let r = sync(
        &s,
        2,
        json!({"server": "play.x", "mc": b, "gui": null, "want": [a]}),
    )
    .await;
    let body = r.json::<Value>();
    let players = body["players"].as_array().unwrap();
    assert_eq!(players.len(), 1);
    assert_eq!(players[0]["mc"], a.to_string());
    assert_eq!(players[0]["gui"], "AQEAAAA=");
}

#[tokio::test]
async fn rejects_bad_input_and_uuid_theft() {
    let s = server();
    let mc = Uuid::from_u128(10);
    let r = sync(&s, 1, json!({"server": "x", "mc": mc, "gui": "<bad>"})).await;
    assert_eq!(r.status_code(), 400);
    let r = sync(&s, 1, json!({"server": "", "mc": mc})).await;
    assert_eq!(r.status_code(), 400);
    assert_eq!(
        sync(&s, 1, json!({"server": "x", "mc": mc}))
            .await
            .status_code(),
        200
    );
    let r = sync(&s, 2, json!({"server": "x", "mc": mc})).await;
    assert_eq!(r.status_code(), 409);
}

#[tokio::test]
async fn read_only_poll_does_not_publish() {
    let s = server();
    let (a, b) = (Uuid::from_u128(10), Uuid::from_u128(20));
    sync(
        &s,
        1,
        json!({"server": "x", "mc": a, "publish": false, "want": [b]}),
    )
    .await;
    let r = sync(&s, 2, json!({"server": "x", "mc": b, "want": [a]})).await;
    assert_eq!(r.json::<Value>()["players"].as_array().unwrap().len(), 0);
}
