#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Upstream {
    Accounts,
    Configs,
    Chat,
}

pub fn upstream_for(path: &str) -> Option<Upstream> {
    match path.trim_start_matches('/').split('/').next()? {
        "auth" | "device" | "avatars" | "media" | "me" | "users" => Some(Upstream::Accounts),
        "configs" | "showcase" => Some(Upstream::Configs),
        "presence" => Some(Upstream::Chat),
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn routes_by_first_segment_only() {
        assert_eq!(upstream_for("/auth/login"), Some(Upstream::Accounts));
        assert_eq!(upstream_for("/me"), Some(Upstream::Accounts));
        assert_eq!(
            upstream_for("/media/avatars/abc.png"),
            Some(Upstream::Accounts)
        );
        assert_eq!(upstream_for("/configs/shared/ABC"), Some(Upstream::Configs));
        assert_eq!(upstream_for("/showcase"), Some(Upstream::Configs));
        assert_eq!(upstream_for("/presence/gui"), Some(Upstream::Chat));
        assert_eq!(upstream_for("/authx"), None);
        assert_eq!(upstream_for("/"), None);
        assert_eq!(upstream_for("/health"), None);
    }
}
