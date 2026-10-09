use super::templates::escape_html;

/// Everything one transactional email needs. All strings are static copy
/// except `url`, but every field still goes through `escape_html` so a future
/// edit cannot open an HTML-injection hole.
pub struct Card<'a> {
    pub lang: &'a str,
    pub preheader: &'a str,
    /// Small pill above the title ("Security", "Welcome").
    pub badge: &'a str,
    pub title: &'a str,
    pub paragraphs: &'a [&'a str],
    pub button: &'a str,
    pub url: &'a str,
    pub expiry: &'a str,
    pub copy_hint: &'a str,
    pub ignore_note: &'a str,
    pub footer: &'a str,
    pub site_url: &'a str,
}

const BG: &str = "#070b0c";
const CARD: &str = "#0d1416";
const TEXT: &str = "#e7eef0";
const MUTED: &str = "#8fa3a8";
const ACCENT: &str = "#be185d";
const ACCENT_SOFT: &str = "#f472b6";
const BORDER: &str = "#1f2d31";
const CODE_BG: &str = "#111a1d";
const FONT: &str = "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";
const MONO: &str = "SFMono-Regular,Consolas,'Liberation Mono',Menlo,monospace";

/// Table-based, inline-styled HTML (the only layout every mail client
/// renders): 600 px card, gradient banner, bulletproof button, a separate
/// copyable link block, hidden preheader. The palette is dark on purpose, so
/// Gmail's and Outlook's automatic dark mode has nothing left to invert.
pub fn html(c: &Card) -> String {
    let url = escape_html(c.url);
    let paragraphs: String = c
        .paragraphs
        .iter()
        .map(|p| {
            format!(
                "<p style=\"margin:0 0 16px;font-size:16px;line-height:26px;color:{TEXT};\">{}</p>",
                escape_html(p)
            )
        })
        .collect();
    format!(
        "<!DOCTYPE html><html lang=\"{lang}\"><head><meta charset=\"utf-8\">\
<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\
<meta name=\"color-scheme\" content=\"dark light\"><meta name=\"supported-color-schemes\" content=\"dark light\">\
<title>{title}</title></head>\
<body style=\"margin:0;padding:0;background:{BG};\" bgcolor=\"{BG}\">\
<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;color:{BG};\">{pre}&#8199;&#847;&#8199;&#847;&#8199;&#847;</div>\
<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"{BG}\" style=\"background:{BG};\"><tr><td align=\"center\" style=\"padding:32px 12px;\">\
<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;max-width:600px;background:{CARD};border:1px solid {BORDER};border-radius:18px;overflow:hidden;font-family:{FONT};\" bgcolor=\"{CARD}\">\
<tr><td bgcolor=\"{ACCENT}\" style=\"background:{ACCENT};background-image:linear-gradient(135deg,{ACCENT} 0%,#7c1d6f 55%,#312e81 100%);padding:28px 32px 30px;\">\
<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>\
<td style=\"padding-right:12px;\"><img src=\"{site}/favicon.png\" width=\"40\" height=\"40\" alt=\"LoVisual\" style=\"display:block;border:0;border-radius:10px;\"></td>\
<td style=\"font-size:22px;font-weight:800;letter-spacing:0.5px;color:#ffffff;\">LoVisual</td></tr></table>\
<div style=\"margin:26px 0 12px;\"><span style=\"display:inline-block;padding:4px 12px;border-radius:999px;background:rgba(255,255,255,0.18);font-size:12px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#ffffff;\">{badge}</span></div>\
<h1 style=\"margin:0;font-size:28px;line-height:34px;font-weight:800;color:#ffffff;\">{title}</h1>\
</td></tr>\
<tr><td style=\"padding:32px 32px 8px;\">{paragraphs}\
<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:8px 0 18px;\"><tr>\
<td bgcolor=\"{ACCENT}\" style=\"border-radius:12px;background:{ACCENT};\"><a href=\"{url}\" target=\"_blank\" style=\"display:inline-block;padding:15px 36px;font-size:16px;font-weight:700;color:#ffffff;text-decoration:none;border-radius:12px;\">{button}</a></td></tr></table>\
<p style=\"margin:0 0 24px;font-size:14px;line-height:22px;color:{MUTED};\">{expiry}</p>\
</td></tr>\
<tr><td style=\"padding:0 32px 8px;\">\
<p style=\"margin:0 0 8px;font-size:13px;line-height:20px;color:{MUTED};\">{hint}</p>\
<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td bgcolor=\"{CODE_BG}\" style=\"background:{CODE_BG};border:1px solid {BORDER};border-radius:10px;padding:12px 14px;font-family:{MONO};font-size:13px;line-height:20px;word-break:break-all;\">\
<a href=\"{url}\" target=\"_blank\" style=\"color:{ACCENT_SOFT};text-decoration:none;\">{url}</a></td></tr></table>\
</td></tr>\
<tr><td style=\"padding:24px 32px 32px;\"><p style=\"margin:0;font-size:13px;line-height:21px;color:{MUTED};\">{ignore}</p></td></tr>\
<tr><td bgcolor=\"#0a1011\" style=\"background:#0a1011;border-top:1px solid {BORDER};padding:20px 32px;\">\
<p style=\"margin:0;font-size:12px;line-height:19px;color:{MUTED};\">{footer}<br><a href=\"{site}\" target=\"_blank\" style=\"color:{ACCENT_SOFT};text-decoration:none;\">{site_host}</a></p>\
</td></tr></table></td></tr></table></body></html>",
        lang = escape_html(c.lang),
        title = escape_html(c.title),
        pre = escape_html(c.preheader),
        badge = escape_html(c.badge),
        button = escape_html(c.button),
        expiry = escape_html(c.expiry),
        hint = escape_html(c.copy_hint),
        ignore = escape_html(c.ignore_note),
        footer = escape_html(c.footer),
        site = escape_html(c.site_url),
        site_host = escape_html(
            c.site_url
                .trim_start_matches("https://")
                .trim_start_matches("http://")
        ),
    )
}

/// Plain-text twin: same order as the HTML, with the link on its own line so
/// it stays one clean, copyable URL in clients that strip HTML.
pub fn text(c: &Card) -> String {
    let mut out = format!("{}\n\n", c.title);
    for p in c.paragraphs {
        out.push_str(p);
        out.push_str("\n\n");
    }
    out.push_str(&format!(
        "{}:\n{}\n\n{}\n\n{}\n\n--\n{}\n{}\n",
        c.button, c.url, c.expiry, c.ignore_note, c.footer, c.site_url
    ));
    out
}
