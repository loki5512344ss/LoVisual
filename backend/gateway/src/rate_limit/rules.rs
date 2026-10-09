use axum::http::Method;
use governor::Quota;
use std::num::NonZeroU32;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum KeyBy {
    Ip,
    Account,
}

pub struct Rule {
    pub method: Method,
    /// Exact path, or a prefix when it ends with '*'.
    pub pattern: &'static str,
    pub quota: Quota,
    pub key_by: KeyBy,
}

impl Rule {
    pub fn matches(&self, method: &Method, path: &str) -> bool {
        if self.method != *method {
            return false;
        }
        match self.pattern.strip_suffix('*') {
            Some(prefix) => path.starts_with(prefix),
            None => path == self.pattern,
        }
    }
}

fn n(v: u32) -> NonZeroU32 {
    NonZeroU32::new(v).expect("rate-limit constants are non-zero")
}

fn rule(method: Method, pattern: &'static str, quota: Quota, key_by: KeyBy) -> Rule {
    Rule {
        method,
        pattern,
        quota,
        key_by,
    }
}

pub fn rules() -> Vec<Rule> {
    use KeyBy::*;
    vec![
        rule(Method::POST, "/auth/login", Quota::per_minute(n(5)), Ip),
        rule(Method::POST, "/auth/register", Quota::per_hour(n(3)), Ip),
        // Same threat as register: each accepted request sends an email, so
        // the mailer must not be usable as a spam cannon.
        rule(
            Method::POST,
            "/auth/forgot-password",
            Quota::per_hour(n(3)),
            Ip,
        ),
        rule(
            Method::POST,
            "/auth/reset-password",
            Quota::per_minute(n(10)),
            Ip,
        ),
        // Verification tokens are 256-bit random, so this limit exists to
        // keep the route cheap, not to stop brute force.
        rule(
            Method::POST,
            "/auth/verify-email",
            Quota::per_minute(n(10)),
            Ip,
        ),
        // Requires login: per account (IP until the credential resolves),
        // tight enough that one session cannot spam the mailbox.
        rule(
            Method::POST,
            "/auth/resend-verification",
            Quota::per_hour(n(3)),
            Account,
        ),
        rule(Method::POST, "/auth/refresh", Quota::per_minute(n(30)), Ip),
        rule(Method::POST, "/device/code", Quota::per_minute(n(10)), Ip),
        // The mod polls every 2–3 s for up to 10 min: 10/min would break linking.
        rule(Method::POST, "/device/token", Quota::per_minute(n(30)), Ip),
        rule(Method::POST, "/device/revoke", Quota::per_minute(n(10)), Ip),
        rule(Method::PUT, "/configs/*", Quota::per_minute(n(20)), Account),
        rule(
            Method::GET,
            "/configs/shared/*",
            Quota::per_minute(n(30)),
            Ip,
        ),
        rule(Method::POST, "/avatars", Quota::per_hour(n(5)), Account),
        rule(Method::GET, "/showcase*", Quota::per_minute(n(60)), Ip),
        // The mod syncs menu presence about 3 times a second while a menu is open.
        rule(
            Method::POST,
            "/presence/*",
            Quota::per_second(n(6)),
            Account,
        ),
    ]
}

pub fn global_quota() -> Quota {
    Quota::per_minute(n(300))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn exact_and_prefix_matching() {
        let all = rules();
        let find = |m: Method, p: &str| all.iter().position(|r| r.matches(&m, p));
        assert_eq!(find(Method::POST, "/auth/login"), Some(0));
        assert_eq!(find(Method::GET, "/auth/login"), None);
        assert_eq!(find(Method::POST, "/auth/login/x"), None);
        assert!(find(Method::PUT, "/configs/3").is_some());
        assert!(find(Method::GET, "/configs/shared/ABCD").is_some());
        assert!(find(Method::GET, "/showcase").is_some());
        assert!(
            find(
                Method::GET,
                "/showcase/11111111-1111-1111-1111-111111111111"
            )
            .is_some()
        );
    }
}
