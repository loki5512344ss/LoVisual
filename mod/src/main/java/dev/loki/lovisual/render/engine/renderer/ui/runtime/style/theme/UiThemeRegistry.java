package dev.loki.lovisual.render.engine.renderer.ui.runtime.style.theme;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.theme.UiTheme;


public enum UiThemeRegistry {
    ;
    private static UiTheme current = new UiTheme("default", 1);

    public static UiTheme current() {
        return current;
    }

    public static void setCurrent(UiTheme theme) {
        current = theme != null ? theme : new UiTheme("default", 1);
    }
}
