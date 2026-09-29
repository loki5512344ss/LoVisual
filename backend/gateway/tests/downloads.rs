mod common;

use axum::http::StatusCode;

/// Fresh per-test downloads directory so parallel tests never share a file.
async fn server_with(downloads_dir: &std::path::Path) -> axum_test::TestServer {
    let accounts = common::spawn_echo().await;
    let configs = common::spawn_echo().await;
    let mut cfg = common::config(&accounts, &configs);
    cfg.downloads_dir = downloads_dir.display().to_string();
    let app = gateway::build_app(&cfg, common::no_devices());
    axum_test::TestServer::new(app)
}

/// A clean per-test directory inside the gateway's downloads root.
fn dir(name: &str) -> std::path::PathBuf {
    let dir = std::env::temp_dir().join(format!("lv-downloads-{name}-{}", std::process::id()));
    let _ = std::fs::remove_dir_all(&dir);
    std::fs::create_dir_all(&dir).expect("create downloads dir");
    dir
}

#[tokio::test]
async fn serves_the_mod_jar_as_a_download() {
    let dir = dir("serve");
    std::fs::write(dir.join("lovisual.jar"), b"fake-jar-bytes").expect("write jar");
    let server = server_with(&dir).await;

    let res = server.get("/downloads/lovisual.jar").await;
    res.assert_status(StatusCode::OK);
    assert_eq!(
        res.headers()["content-type"],
        "application/java-archive",
        "the jar keeps its own media type"
    );
    assert_eq!(res.headers()["content-length"], "14");
    assert_eq!(res.text(), "fake-jar-bytes");
    std::fs::remove_dir_all(&dir).ok();
}

#[tokio::test]
async fn missing_file_and_directory_index_are_404() {
    let dir = dir("missing");
    let server = server_with(&dir).await;

    server
        .get("/downloads/other.jar")
        .await
        .assert_status(StatusCode::NOT_FOUND);
    // No index/SPA fallback under /downloads: a directory is never a download.
    let res = server.get("/downloads/").await;
    assert_ne!(
        res.status_code(),
        StatusCode::OK,
        "directory must not be served"
    );
    std::fs::remove_dir_all(&dir).ok();
}

#[tokio::test]
async fn dot_segment_traversal_is_rejected_before_the_file_is_touched() {
    let dir = dir("traversal");
    std::fs::write(dir.join("lovisual.jar"), b"fake-jar-bytes").expect("write jar");
    let accounts = common::spawn_echo().await;
    let configs = common::spawn_echo().await;
    let mut cfg = common::config(&accounts, &configs);
    cfg.downloads_dir = dir.display().to_string();
    let app = gateway::build_app(&cfg, common::no_devices());

    // Raw request: an HTTP client would normalize `..` away before sending.
    let (status, _headers, body) =
        common::raw(&app, "GET", "/downloads/../../etc/passwd", "1.2.3.4").await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    assert!(!body.contains("fake-jar-bytes"));
    std::fs::remove_dir_all(&dir).ok();
}
