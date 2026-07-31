//! LoVisual launcher entry point.

mod api;

slint::include_modules!();

use slint::ComponentHandle;

fn main() -> Result<(), slint::PlatformError> {
    let ui = MainWindow::new()?;

    spawn_backend_status_check(ui.as_weak());

    let weak = ui.as_weak();
    ui.on_login(move || {
        let ui = weak.unwrap();
        let login = ui.get_login_input().trim().to_string();
        let password = ui.get_password_input().trim().to_string();

        if login.is_empty() || password.is_empty() {
            ui.set_status("Заполните логин и пароль".into());
            return;
        }

        // TODO: auth against backend, save session, transition to dashboard.
        ui.set_status(format!("Вход выполнен как {login}").into());
    });

    ui.run()
}

/// Ping backend health in background and surface the result in the UI.
fn spawn_backend_status_check(weak: slint::Weak<MainWindow>) {
    std::thread::spawn(move || {
        let runtime = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("failed to build tokio runtime");

        let status = runtime.block_on(api::health(api::DEFAULT_API_URL));
        let message = match status {
            Ok(health) => format!("Backend v{}: {}", health.version, health.status),
            Err(error) => format!("Backend недоступен: {error}"),
        };

        let _ = slint::invoke_from_event_loop(move || {
            if let Some(ui) = weak.upgrade() {
                ui.set_status(message.clone().into());
            }
        });
    });
}
