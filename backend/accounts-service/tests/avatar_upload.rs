mod common;

use axum::http::StatusCode;
use axum_test::multipart::{MultipartForm, Part};
use common::ACCOUNT_ID_HEADER;

fn png_bytes() -> Vec<u8> {
    let img = image::RgbaImage::new(64, 32);
    let mut out = std::io::Cursor::new(Vec::new());
    img.write_to(&mut out, image::ImageFormat::Png).unwrap();
    out.into_inner()
}

fn form(bytes: Vec<u8>) -> MultipartForm {
    MultipartForm::new().add_part(
        "file",
        Part::bytes(bytes).file_name("a.png").mime_type("image/png"),
    )
}

#[tokio::test]
async fn valid_upload_stores_avatar_and_returns_url() {
    common::init_test_logging();
    common::ensure_avatar_bucket().await;
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(
        pool.clone(),
        &common::test_config(),
    ));
    let (account_id, email) = common::register_account(&server).await;

    let response = server
        .post("/avatars")
        .add_header(ACCOUNT_ID_HEADER, account_id.to_string())
        .multipart(form(png_bytes()))
        .await;
    response.assert_status_ok();
    let url = response.json::<serde_json::Value>()["avatar_url"]
        .as_str()
        .unwrap()
        .to_string();
    assert!(
        url.starts_with("http://localhost:9000/lovisual-avatars-test/avatars/"),
        "{url}"
    );
    assert!(url.ends_with(".png"), "{url}");

    let stored: (String,) = sqlx::query_as(
        "SELECT a.s3_key FROM avatars a JOIN accounts c ON c.id = a.account_id WHERE lower(c.email) = $1",
    )
    .bind(email.to_lowercase())
    .fetch_one(&pool)
    .await
    .expect("avatars row must exist");
    assert!(url.ends_with(&stored.0));

    sqlx::query("DELETE FROM accounts WHERE lower(email) = $1")
        .bind(email.to_lowercase())
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn non_image_upload_is_rejected_with_400() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(
        pool.clone(),
        &common::test_config(),
    ));
    let (account_id, email) = common::register_account(&server).await;

    server
        .post("/avatars")
        .add_header(ACCOUNT_ID_HEADER, account_id.to_string())
        .multipart(form(b"definitely not an image".to_vec()))
        .await
        .assert_status(StatusCode::BAD_REQUEST);

    sqlx::query("DELETE FROM accounts WHERE lower(email) = $1")
        .bind(email.to_lowercase())
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn oversized_upload_is_rejected_with_400() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(
        pool.clone(),
        &common::test_config(),
    ));
    let (account_id, email) = common::register_account(&server).await;

    server
        .post("/avatars")
        .add_header(ACCOUNT_ID_HEADER, account_id.to_string())
        .multipart(form(vec![0u8; 5 * 1024 * 1024 + 1]))
        .await
        .assert_status(StatusCode::BAD_REQUEST);

    sqlx::query("DELETE FROM accounts WHERE lower(email) = $1")
        .bind(email.to_lowercase())
        .execute(&pool)
        .await
        .unwrap();
}

#[tokio::test]
async fn upload_without_identity_is_401() {
    let pool = common::test_pool().await;
    let server = common::test_server(accounts_service::build_app(
        pool.clone(),
        &common::test_config(),
    ));

    server
        .post("/avatars")
        .multipart(form(png_bytes()))
        .await
        .assert_status(StatusCode::UNAUTHORIZED);
}
