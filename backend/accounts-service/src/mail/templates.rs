use super::Lang;

/// Rendered email content: subject plus plain-text and HTML bodies.
pub struct EmailContent {
    pub subject: String,
    pub text: String,
    pub html: String,
}

/// Escapes the five characters that matter in HTML text and attribute
/// values. Today the only dynamic input is the link (a trusted-config base
/// URL plus our own base64url token), but escaping stays mandatory so a
/// future template edit cannot silently re-open an HTML-injection hole.
pub fn escape_html(s: &str) -> String {
    let mut out = String::with_capacity(s.len());
    for ch in s.chars() {
        match ch {
            '&' => out.push_str("&amp;"),
            '<' => out.push_str("&lt;"),
            '>' => out.push_str("&gt;"),
            '"' => out.push_str("&quot;"),
            '\'' => out.push_str("&#39;"),
            _ => out.push(ch),
        }
    }
    out
}

/// The only link origin is `PUBLIC_BASE_URL` from config; request headers
/// are never consulted (host header injection would forge phishing links).
fn link(base_url: &str, path: &str, token: &str) -> String {
    format!(
        "{}{path}?token={}",
        base_url.trim().trim_end_matches('/'),
        token
    )
}

fn build(subject: &str, text: String, html: String) -> EmailContent {
    EmailContent {
        subject: subject.to_owned(),
        text,
        html,
    }
}

/// Renders the HTML body with every dynamic value escaped: the link origin
/// comes from config, but the config is still data, not markup.
fn html_body(body_template: &str, url: &str) -> String {
    body_template.replace("{URL}", &escape_html(url))
}

/// Password reset: one button, 30-minute token, generic wording (the mail
/// itself must not reveal whether the address even has an account).
pub fn reset_email(lang: Lang, base_url: &str, token: &str) -> EmailContent {
    let url = link(base_url, "/reset-password", token);
    match lang {
        Lang::Ru => build(
            "Сброс пароля LoVisual",
            format!(
                "Кто-то запросил сброс пароля LoVisual.\n\
                 Если это были вы, открой ссылку (действует 30 минут):\n{url}\n\n\
                 Если нет — просто проигнорируйте письмо, пароль не менялся."
            ),
            html_body(
                "<p>Кто-то запросил сброс пароля LoVisual.</p>\
                 <p>Если это были вы, нажмите кнопку (действует 30 минут):</p>\
                 <p><a href=\"{URL}\">Сбросить пароль</a></p>\
                 <p>Если нет — просто проигнорируйте письмо, пароль не менялся.</p>",
                &url,
            ),
        ),
        Lang::En => build(
            "LoVisual password reset",
            format!(
                "Someone requested a password reset for LoVisual.\n\
                 If it was you, open the link (valid for 30 minutes):\n{url}\n\n\
                 If not, just ignore this email - the password was not changed."
            ),
            html_body(
                "<p>Someone requested a password reset for LoVisual.</p>\
                 <p>If it was you, press the button (valid for 30 minutes):</p>\
                 <p><a href=\"{URL}\">Reset password</a></p>\
                 <p>If not, just ignore this email - the password was not changed.</p>",
                &url,
            ),
        ),
    }
}

/// Address verification: one button, 24-hour token.
pub fn verify_email(lang: Lang, base_url: &str, token: &str) -> EmailContent {
    let url = link(base_url, "/verify", token);
    match lang {
        Lang::Ru => build(
            "Подтверждение почты LoVisual",
            format!(
                "Добро пожаловать в LoVisual!\n\
                 Подтвердите почту по ссылке (действует 24 часа):\n{url}\n\n\
                 Без подтверждения нельзя привязать мод к аккаунту."
            ),
            html_body(
                "<p>Добро пожаловать в LoVisual!</p>\
                 <p>Подтвердите почту по ссылке (действует 24 часа):</p>\
                 <p><a href=\"{URL}\">Подтвердить почту</a></p>\
                 <p>Без подтверждения нельзя привязать мод к аккаунту.</p>",
                &url,
            ),
        ),
        Lang::En => build(
            "LoVisual email verification",
            format!(
                "Welcome to LoVisual!\n\
                 Verify your email via the link (valid for 24 hours):\n{url}\n\n\
                 Device linking stays locked until the address is verified."
            ),
            html_body(
                "<p>Welcome to LoVisual!</p>\
                 <p>Verify your email via the link (valid for 24 hours):</p>\
                 <p><a href=\"{URL}\">Verify email</a></p>\
                 <p>Device linking stays locked until the address is verified.</p>",
                &url,
            ),
        ),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const BASE: &str = "https://visual.loki-code.dev/";
    const TOKEN: &str = "lvev_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    #[test]
    fn links_are_built_from_config_base_without_double_slash() {
        let mail = verify_email(Lang::Ru, BASE, TOKEN);
        assert!(
            mail.html
                .contains("https://visual.loki-code.dev/verify?token=lvev_")
        );
        assert!(!mail.html.contains("dev//verify"));
    }

    #[test]
    fn dynamic_content_is_html_escaped() {
        // A hostile base URL must not be able to break out of the attribute.
        let evil = "https://x.example\"><script>alert(1)</script>";
        let mail = verify_email(Lang::En, evil, TOKEN);
        assert!(!mail.html.contains("<script>"));
        assert!(mail.html.contains("&lt;script&gt;"));
    }

    #[test]
    fn both_locales_render_distinct_subjects() {
        let ru = reset_email(Lang::Ru, BASE, TOKEN);
        let en = reset_email(Lang::En, BASE, TOKEN);
        assert_ne!(ru.subject, en.subject);
        assert!(en.text.contains("/reset-password?token="));
        assert!(ru.text.contains("/reset-password?token="));
    }

    #[test]
    fn verify_and_reset_point_to_their_own_pages() {
        let reset = reset_email(Lang::En, BASE, TOKEN);
        let verify = verify_email(Lang::En, BASE, TOKEN);
        assert!(reset.html.contains("/reset-password?token="));
        assert!(verify.html.contains("/verify?token="));
    }
}
