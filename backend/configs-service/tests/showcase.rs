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

async fn publish(s: &axum_test::TestServer, owner: &str, title: &str) -> String {
    s.put("/configs/1")
        .add_header(common::ACCOUNT_ID_HEADER, owner)
        .json(&json!({ "name": "cfg", "data": { "t": title } }))
        .await
        .assert_status_ok();
    let r = s
        .post("/configs/1/publish")
        .add_header(common::ACCOUNT_ID_HEADER, owner)
        .json(&json!({ "title": title, "description": "desc" }))
        .await;
    r.assert_status_ok();
    r.json::<serde_json::Value>()["listing_id"]
        .as_str()
        .unwrap()
        .to_owned()
}

#[tokio::test]
async fn published_listing_shows_up_publicly_with_author() {
    let s = server().await;
    let owner = common::new_account();
    let id = publish(&s, &owner.to_string(), "Best PvP").await;

    let page: serde_json::Value = s.get("/showcase?sort=new&page=0").await.json();
    let item = page["items"]
        .as_array()
        .unwrap()
        .iter()
        .find(|i| i["id"] == id)
        .expect("listed");
    assert_eq!(item["title"], "Best PvP");
    assert_eq!(item["config_name"], "cfg");
    assert_eq!(
        item["author"]["nick"],
        format!("Author-{}", &owner.to_string()[..4])
    );
    assert!(item.get("account_id").is_none());

    let detail: serde_json::Value = s.get(&format!("/showcase/{id}")).await.json();
    assert_eq!(detail["data"], json!({ "t": "Best PvP" }));
}

#[tokio::test]
async fn unpublish_removes_listing() {
    let s = server().await;
    let owner = common::new_account().to_string();
    let id = publish(&s, &owner, "gone").await;
    s.delete("/configs/1/publish")
        .add_header(common::ACCOUNT_ID_HEADER, &owner)
        .await
        .assert_status(StatusCode::NO_CONTENT);
    s.get(&format!("/showcase/{id}"))
        .await
        .assert_status(StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn copy_goes_into_a_free_slot_and_counts() {
    let s = server().await;
    let id = publish(&s, &common::new_account().to_string(), "copy me").await;
    let me = common::new_account().to_string();

    let r = s
        .post(&format!("/showcase/{id}/copy"))
        .add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "slot": 3 }))
        .await;
    r.assert_status(StatusCode::CREATED);
    assert_eq!(
        r.json::<serde_json::Value>()["data"],
        json!({ "t": "copy me" })
    );

    s.post(&format!("/showcase/{id}/copy"))
        .add_header(common::ACCOUNT_ID_HEADER, &me)
        .json(&json!({ "slot": 3 }))
        .await
        .assert_status(StatusCode::CONFLICT);

    let detail: serde_json::Value = s.get(&format!("/showcase/{id}")).await.json();
    assert_eq!(detail["copies_count"], 1);
}

#[tokio::test]
async fn publish_validation_and_auth() {
    let s = server().await;
    let owner = common::new_account().to_string();
    s.post("/configs/2/publish")
        .add_header(common::ACCOUNT_ID_HEADER, &owner)
        .json(&json!({ "title": "x" }))
        .await
        .assert_status(StatusCode::NOT_FOUND);
    s.put("/configs/2")
        .add_header(common::ACCOUNT_ID_HEADER, &owner)
        .json(&json!({ "name": "n", "data": {} }))
        .await;
    s.post("/configs/2/publish")
        .add_header(common::ACCOUNT_ID_HEADER, &owner)
        .json(&json!({ "title": "  " }))
        .await
        .assert_status(StatusCode::BAD_REQUEST);
    s.post("/configs/2/publish")
        .json(&json!({ "title": "x" }))
        .await
        .assert_status_unauthorized();
    s.get("/showcase?sort=bogus")
        .await
        .assert_status(StatusCode::BAD_REQUEST);
}

#[tokio::test]
async fn popular_sort_orders_by_copies() {
    let s = server().await;
    let low = publish(&s, &common::new_account().to_string(), "low").await;
    let high = publish(&s, &common::new_account().to_string(), "high").await;
    for _ in 0..2 {
        s.post(&format!("/showcase/{high}/copy"))
            .add_header(
                common::ACCOUNT_ID_HEADER,
                &common::new_account().to_string(),
            )
            .json(&json!({ "slot": 1 }))
            .await
            .assert_status(StatusCode::CREATED);
    }
    let page: serde_json::Value = s.get("/showcase?sort=popular").await.json();
    let ids: Vec<&str> = page["items"]
        .as_array()
        .unwrap()
        .iter()
        .map(|i| i["id"].as_str().unwrap())
        .collect();
    let hi = ids.iter().position(|i| *i == high);
    let lo = ids.iter().position(|i| *i == low);
    assert!(
        hi.is_some() && lo.is_none_or(|lo| hi.unwrap() < lo),
        "high-copy listing must come first"
    );
}

async fn hide(pool: &sqlx::PgPool, id: &str) {
    sqlx::query("UPDATE showcase_listings SET hidden = true WHERE id = $1")
        .bind(uuid::Uuid::parse_str(id).unwrap())
        .execute(pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn hidden_listing_is_invisible_everywhere_and_moderation_wins() {
    let pool = common::test_pool().await;
    let s = common::test_server(configs_service::build_app(
        pool.clone(),
        &common::test_config(),
        common::no_profiles(),
    ));
    let owner = common::new_account().to_string();
    let id = publish(&s, &owner, "moderate me").await;
    hide(&pool, &id).await;

    // Invisible in browse.
    let page: serde_json::Value = s.get("/showcase?sort=new&page=0").await.json();
    assert!(
        page["items"]
            .as_array()
            .unwrap()
            .iter()
            .all(|i| i["id"] != id),
        "hidden listing must not appear in browse"
    );

    // Invisible in the author filter.
    let page: serde_json::Value = s.get(&format!("/showcase?author={owner}")).await.json();
    assert!(
        page["items"]
            .as_array()
            .unwrap()
            .iter()
            .all(|i| i["id"] != id),
        "hidden listing must not appear in the author filter"
    );

    // 404 on detail.
    s.get(&format!("/showcase/{id}"))
        .await
        .assert_status(StatusCode::NOT_FOUND);

    // 404 on copy.
    s.post(&format!("/showcase/{id}/copy"))
        .add_header(
            common::ACCOUNT_ID_HEADER,
            &common::new_account().to_string(),
        )
        .json(&json!({ "slot": 3 }))
        .await
        .assert_status(StatusCode::NOT_FOUND);

    // Unpublishing a hidden listing is a 404, and it stays in the DB.
    s.delete("/configs/1/publish")
        .add_header(common::ACCOUNT_ID_HEADER, &owner)
        .await
        .assert_status(StatusCode::NOT_FOUND);
    let still_there: i64 =
        sqlx::query_scalar("SELECT count(*) FROM showcase_listings WHERE id = $1")
            .bind(uuid::Uuid::parse_str(&id).unwrap())
            .fetch_one(&pool)
            .await
            .unwrap();
    assert_eq!(still_there, 1, "unpublish must not delete a hidden listing");

    // Republishing a hidden slot never unhides it: 403, and it's still hidden.
    s.post("/configs/1/publish")
        .add_header(common::ACCOUNT_ID_HEADER, &owner)
        .json(&json!({ "title": "sneaky", "description": "" }))
        .await
        .assert_status(StatusCode::FORBIDDEN);
    let hidden: bool = sqlx::query_scalar("SELECT hidden FROM showcase_listings WHERE id = $1")
        .bind(uuid::Uuid::parse_str(&id).unwrap())
        .fetch_one(&pool)
        .await
        .unwrap();
    assert!(hidden, "publish must never clear the hidden flag");
}

#[tokio::test]
async fn showcase_can_be_filtered_by_author() {
    let s = server().await;
    let owner = common::new_account().to_string();
    let mine = publish(&s, &owner, "mine one").await;
    let theirs = publish(&s, &common::new_account().to_string(), "someone else").await;

    let page: serde_json::Value = s.get(&format!("/showcase?author={owner}")).await.json();
    let ids: Vec<&str> = page["items"]
        .as_array()
        .unwrap()
        .iter()
        .map(|i| i["id"].as_str().unwrap())
        .collect();
    assert!(ids.contains(&mine.as_str()), "own listing must be listed");
    assert!(
        !ids.contains(&theirs.as_str()),
        "other authors filtered out"
    );

    s.get("/showcase?author=not-a-uuid")
        .await
        .assert_status(StatusCode::BAD_REQUEST);
}
