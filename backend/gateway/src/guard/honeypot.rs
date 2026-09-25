//! Easter egg for `.env` scanners: instead of a 400 they get a fake file.
//! Never forwarded upstream; answered before identity and rate limiting.

use axum::{
    http::{StatusCode, header::CONTENT_TYPE},
    response::{IntoResponse, Response},
};

const FAKE_ENV: &str = "\
# LoVisual production secrets — не благодари
DATABASE_URL=postgres://idi_naxui:daun_ebani@localhost:5432/tvoya_mamka
JWT_SECRET=nice_try_skiddie_tvoy_ip_uzhe_v_bane
INTERNAL_KEY=0000-0000-0000-0000-otvali
ADMIN_PASSWORD=hunter2
S3_SECRET_KEY=lovisual_luchshe_chem_tvoy_chit
# P.S. лучше скачай мод: https://github.com/loki5512344/LoVisual-/releases
";

/// Any segment named `.env` or `.env.<suffix>` (dots may be `%2e`-encoded).
pub fn is_env_probe(path: &str) -> bool {
    super::segments(path).any(|raw| {
        let name = super::decode_dots(raw);
        name == ".env" || name.starts_with(".env.")
    })
}

pub fn fake_env() -> Response {
    (
        StatusCode::OK,
        [(CONTENT_TYPE, "text/plain; charset=utf-8")],
        FAKE_ENV,
    )
        .into_response()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn detects_env_segments_only() {
        for hit in [
            "/.env",
            "/../../.env",
            "/api/.env",
            "/.env.local",
            "/%2e%2e/%2Eenv",
        ] {
            assert!(is_env_probe(hit), "{hit}");
        }
        for miss in [
            "/auth/login",
            "/configs/.environment",
            "/x.env",
            "/showcase/env",
        ] {
            assert!(!is_env_probe(miss), "{miss}");
        }
    }
}
