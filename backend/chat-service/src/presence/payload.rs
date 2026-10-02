//! Validation of the opaque GUI payload and server key. The payload is a
//! base64 string produced by the mod (header + up to 8 cursor samples); the
//! service never decodes it, it only bounds what it stores and relays.

/// Header (4 bytes) + 8 samples x 4 bytes = 36 bytes = 48 base64 characters.
pub const MAX_PAYLOAD_CHARS: usize = 48;
pub const MAX_SERVER_CHARS: usize = 64;
pub const MAX_WANT: usize = 40;

pub fn valid_payload(s: &str) -> bool {
    !s.is_empty()
        && s.len() <= MAX_PAYLOAD_CHARS
        && s.bytes().all(|b| b.is_ascii_alphanumeric() || matches!(b, b'+' | b'/' | b'='))
}

/// Normalised server key: trimmed, lower-case, no control characters, bounded.
pub fn normalize_server(s: &str) -> Option<String> {
    let s = s.trim().to_ascii_lowercase();
    if s.is_empty() || s.chars().count() > MAX_SERVER_CHARS || s.chars().any(|c| c.is_control()) {
        return None;
    }
    Some(s)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn payload_charset_and_length() {
        assert!(valid_payload("AQEAAAA="));
        assert!(!valid_payload(""));
        assert!(!valid_payload("has space"));
        assert!(!valid_payload("<script>"));
        assert!(!valid_payload(&"A".repeat(MAX_PAYLOAD_CHARS + 1)));
        assert!(valid_payload(&"A".repeat(MAX_PAYLOAD_CHARS)));
    }

    #[test]
    fn server_is_normalised() {
        assert_eq!(normalize_server("  Play.Example.COM ").as_deref(), Some("play.example.com"));
        assert_eq!(normalize_server(""), None);
        assert_eq!(normalize_server("a\nb"), None);
        assert_eq!(normalize_server(&"x".repeat(65)), None);
    }
}
