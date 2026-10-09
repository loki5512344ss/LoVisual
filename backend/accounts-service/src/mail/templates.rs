use super::{Lang, layout};

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

fn site_url(base_url: &str) -> String {
    base_url.trim().trim_end_matches('/').to_owned()
}

fn render(subject: &str, card: &layout::Card) -> EmailContent {
    EmailContent {
        subject: subject.to_owned(),
        text: layout::text(card),
        html: layout::html(card),
    }
}

/// Password reset: one button, 30-minute token, generic wording (the mail
/// itself must not reveal whether the address even has an account).
pub fn reset_email(lang: Lang, base_url: &str, token: &str) -> EmailContent {
    let url = link(base_url, "/reset-password", token);
    let site = site_url(base_url);
    match lang {
        Lang::Ru => render(
            "Сброс пароля LoVisual",
            &layout::Card {
                lang: "ru",
                preheader: "Ссылка действует 30 минут. Если это были не вы, ничего делать не нужно.",
                badge: "Безопасность",
                title: "Сброс пароля",
                paragraphs: &[
                    "Мы получили запрос на сброс пароля для вашего аккаунта LoVisual.",
                    "Нажмите кнопку ниже, чтобы придумать новый пароль.",
                ],
                button: "Сбросить пароль",
                url: &url,
                expiry: "Ссылка действует 30 минут и срабатывает один раз.",
                copy_hint: "Кнопка не работает? Скопируйте ссылку и вставьте её в адресную строку браузера:",
                ignore_note: "Если вы не запрашивали сброс, просто проигнорируйте письмо: пароль останется прежним.",
                footer: "Это автоматическое письмо от LoVisual, отвечать на него не нужно.",
                site_url: &site,
            },
        ),
        Lang::En => render(
            "LoVisual password reset",
            &layout::Card {
                lang: "en",
                preheader: "The link is valid for 30 minutes. If it was not you, no action is needed.",
                badge: "Security",
                title: "Reset your password",
                paragraphs: &[
                    "We received a request to reset the password of your LoVisual account.",
                    "Press the button below to choose a new password.",
                ],
                button: "Reset password",
                url: &url,
                expiry: "The link is valid for 30 minutes and works only once.",
                copy_hint: "Button not working? Copy the link and paste it into your browser's address bar:",
                ignore_note: "If you did not request a reset, just ignore this email: your password stays the same.",
                footer: "This is an automated email from LoVisual, no need to reply.",
                site_url: &site,
            },
        ),
    }
}

/// Address verification: one button, 24-hour token.
pub fn verify_email(lang: Lang, base_url: &str, token: &str) -> EmailContent {
    let url = link(base_url, "/verify", token);
    let site = site_url(base_url);
    match lang {
        Lang::Ru => render(
            "Подтверждение почты LoVisual",
            &layout::Card {
                lang: "ru",
                preheader: "Подтвердите почту, чтобы привязать мод к аккаунту. Ссылка действует 24 часа.",
                badge: "Добро пожаловать",
                title: "Подтвердите почту",
                paragraphs: &[
                    "Спасибо за регистрацию в LoVisual!",
                    "Подтвердите адрес почты, чтобы привязать мод к аккаунту и публиковать свои конфиги.",
                ],
                button: "Подтвердить почту",
                url: &url,
                expiry: "Ссылка действует 24 часа и срабатывает один раз.",
                copy_hint: "Кнопка не работает? Скопируйте ссылку и вставьте её в адресную строку браузера:",
                ignore_note: "Если вы не регистрировались в LoVisual, просто проигнорируйте письмо.",
                footer: "Это автоматическое письмо от LoVisual, отвечать на него не нужно.",
                site_url: &site,
            },
        ),
        Lang::En => render(
            "LoVisual email verification",
            &layout::Card {
                lang: "en",
                preheader: "Verify your email to link the mod to your account. The link is valid for 24 hours.",
                badge: "Welcome",
                title: "Verify your email",
                paragraphs: &[
                    "Thanks for signing up for LoVisual!",
                    "Verify your email address to link the mod to your account and publish your configs.",
                ],
                button: "Verify email",
                url: &url,
                expiry: "The link is valid for 24 hours and works only once.",
                copy_hint: "Button not working? Copy the link and paste it into your browser's address bar:",
                ignore_note: "If you did not sign up for LoVisual, just ignore this email.",
                footer: "This is an automated email from LoVisual, no need to reply.",
                site_url: &site,
            },
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

    #[test]
    fn link_is_shown_as_button_and_as_separate_copyable_text() {
        let mail = reset_email(Lang::Ru, BASE, TOKEN);
        let url = format!("https://visual.loki-code.dev/reset-password?token={TOKEN}");
        assert_eq!(
            mail.html.matches(url.as_str()).count(),
            3,
            "2x href + 1x visible text"
        );
        assert!(
            mail.text.lines().any(|l| l == url),
            "plain text keeps the URL alone on a line"
        );
    }

    #[test]
    fn copy_has_no_em_dash_and_ships_a_preheader() {
        for lang in [Lang::Ru, Lang::En] {
            for mail in [
                reset_email(lang, BASE, TOKEN),
                verify_email(lang, BASE, TOKEN),
            ] {
                assert!(!mail.html.contains('\u{2014}') && !mail.text.contains('\u{2014}'));
                assert!(mail.html.contains("display:none;max-height:0"));
            }
        }
    }

    /// `MAIL_PREVIEW_DIR=/tmp/mail cargo test mail_previews` writes the four
    /// emails as .html files for eyeballing; a no-op otherwise.
    #[test]
    fn mail_previews() {
        let Some(dir) = std::env::var_os("MAIL_PREVIEW_DIR") else {
            return;
        };
        std::fs::create_dir_all(&dir).unwrap();
        for (name, mail) in [
            ("reset-ru", reset_email(Lang::Ru, BASE, TOKEN)),
            ("reset-en", reset_email(Lang::En, BASE, TOKEN)),
            ("verify-ru", verify_email(Lang::Ru, BASE, TOKEN)),
            ("verify-en", verify_email(Lang::En, BASE, TOKEN)),
        ] {
            std::fs::write(
                std::path::Path::new(&dir).join(format!("{name}.html")),
                mail.html,
            )
            .unwrap();
        }
    }
}
