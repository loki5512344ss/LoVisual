package dev.loki.lovisual.features.gui.mainmenu.proxy;

import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.util.proxy.ProxyBackend;
import dev.loki.lovisual.util.proxy.model.ProxyEntry;
import dev.loki.lovisual.util.proxy.model.ProxyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;

/**
 * Business state, input routing and proxy CRUD for the proxy manager screen.
 */
public final class ProxyManagerController {
    public static final float SCALE = 2.28f;
    public static final float TEXT_SCALE = 1.30f;
    public static final float PANEL_W = 184f * SCALE;
    public static final float PANEL_H = 184f * SCALE;
    public static final float PANEL_R = 6f * SCALE;
    public static final float FIELD_H = 14f * SCALE;
    public static final float BUTTON_H = 16f * SCALE;
    public static final float GAP = 5f * SCALE;
    public static final float TITLE_FONT = 0.84f * TEXT_SCALE;
    public static final float LABEL_FONT = 0.55f * TEXT_SCALE;
    public static final float FIELD_FONT = 0.60f * TEXT_SCALE;
    public static final float BUTTON_FONT = 0.58f * TEXT_SCALE;
    public static final int FIELD_COUNT = 3;

    private final String[] values = {"", "", ""};
    private Field focusedField;
    private ProxyType type = ProxyType.SOCKS5;
    private boolean enabled;
    private String status = "";
    private float fixedWidth;
    private float fixedHeight;

    public String[] values() { return values; }
    public Field focusedField() { return focusedField; }
    public void setFocusedField(Field f) { this.focusedField = f; }
    public ProxyType type() { return type; }
    public boolean enabled() { return enabled; }
    public String status() { return status; }
    public void setStatus(String s) { this.status = s; }
    public float fixedWidth() { return fixedWidth; }
    public float fixedHeight() { return fixedHeight; }

    public static String tr(String key, Object... args) {
        return I18n.get(key, args);
    }

    public static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public float[] panelOrigin() {
        return new float[]{fixedWidth * 0.5f - PANEL_W * 0.5f, fixedHeight * 0.5f - PANEL_H * 0.5f};
    }

    public void updateUiMetrics(Minecraft mc, int width, int height) {
        if (mc == null) { fixedWidth = width; fixedHeight = height; return; }
        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        fixedWidth = Math.max(1f, HudScale.virtualWidth(fbw, fbh));
        fixedHeight = Math.max(1f, HudScale.virtualHeight(fbw, fbh));
    }

    public void loadProxy() {
        ProxyEntry proxy = ProxyBackend.getDefaultProxy();
        type = proxy.type();
        values[Field.ADDRESS.index] = proxy.ipPort();
        values[Field.USERNAME.index] = proxy.username();
        values[Field.PASSWORD.index] = proxy.password();
        enabled = ProxyBackend.isEnabled();
        status = "";
    }

    /** @return true if "back" button was clicked; caller should close. */
    public boolean handleClick(float mouseX, float mouseY) {
        float[] o = panelOrigin();
        float innerX = o[0] + 8f * SCALE;
        float innerW = PANEL_W - 16f * SCALE;
        float rowY = o[1] + 28f * SCALE;
        focusedField = null;

        if (inside(mouseX, mouseY, innerX, rowY, 66f * SCALE, 13f * SCALE)) {
            enabled = !enabled;
            status = enabled ? tr("screen.lovisual.proxy_manager.status.enabled")
                    : tr("screen.lovisual.proxy_manager.status.disabled");
            return false;
        }
        float typeX = innerX + 72f * SCALE;
        float typeW = innerW - 72f * SCALE;
        float typeItemW = (typeW - 2f * SCALE) * 0.5f;
        if (inside(mouseX, mouseY, typeX, rowY, typeItemW, 13f * SCALE)) {
            type = ProxyType.SOCKS5;
            return false;
        }
        if (inside(mouseX, mouseY, typeX + typeItemW + 2f * SCALE, rowY, typeItemW, 13f * SCALE)) {
            type = ProxyType.SOCKS4;
            return false;
        }
        float fieldY = o[1] + 53f * SCALE;
        for (Field field : Field.values()) {
            float y = fieldY + field.index * 27f * SCALE + 9f * SCALE;
            if (inside(mouseX, mouseY, innerX, y, innerW, FIELD_H)) {
                focusedField = field;
                return false;
            }
        }
        float buttonY = o[1] + PANEL_H - 26f * SCALE;
        float third = (innerW - GAP * 2f) / 3f;
        if (inside(mouseX, mouseY, innerX, buttonY, third, BUTTON_H)) { saveProxy(); return false; }
        if (inside(mouseX, mouseY, innerX + third + GAP, buttonY, third, BUTTON_H)) { clearProxy(); return false; }
        if (inside(mouseX, mouseY, innerX + (third + GAP) * 2f, buttonY, third, BUTTON_H)) { return true; }
        return false;
    }

    public boolean handleKey(KeyEvent input) {
        if (focusedField != null) {
            int key = input.key();
            if (key == 259) {
                int i = focusedField.index;
                if (!values[i].isEmpty()) values[i] = values[i].substring(0, values[i].length() - 1);
                return true;
            }
            if (key == 258) { focusedField = focusedField.next(); return true; }
            if (key == 257 || key == 335) { saveProxy(); return true; }
            if (key == 256) { focusedField = null; return true; }
        }
        return false;
    }

    public boolean handleChar(CharacterEvent input) {
        if (focusedField == null) return false;
        int cp = input.codepoint();
        if (cp < 32 || cp > 126) return true;
        int i = focusedField.index;
        int max = focusedField == Field.ADDRESS ? 96 : 64;
        if (values[i].length() >= max) return true;
        values[i] += Character.toString(cp);
        return true;
    }

    public String currentSummary() {
        if (!enabled) return tr("screen.lovisual.proxy_manager.status.disabled");
        String addr = values[Field.ADDRESS.index].trim();
        if (addr.isEmpty()) return tr("screen.lovisual.proxy_manager.status.enabled_no_address");
        return type.name() + " " + addr;
    }

    private void saveProxy() {
        String address = values[Field.ADDRESS.index].trim();
        if (!address.isEmpty() && !ProxyEntry.isValidIpPort(address)) {
            status = tr("screen.lovisual.proxy_manager.status.invalid_address");
            return;
        }
        if (address.isEmpty()) enabled = false;
        ProxyEntry entry = address.isEmpty() ? ProxyEntry.empty()
                : new ProxyEntry(type, address, values[Field.USERNAME.index], values[Field.PASSWORD.index]);
        ProxyBackend.setDefaultProxy(entry);
        ProxyBackend.setEnabled(enabled);
        status = address.isEmpty() ? tr("screen.lovisual.proxy_manager.status.cleared")
                : tr("screen.lovisual.proxy_manager.status.saved", type.name());
    }

    private void clearProxy() {
        values[0] = ""; values[1] = ""; values[2] = "";
        enabled = false; type = ProxyType.SOCKS5; focusedField = null;
        ProxyBackend.setDefaultProxy(ProxyEntry.empty());
        ProxyBackend.setEnabled(false);
        status = tr("screen.lovisual.proxy_manager.status.cleared");
    }

    public enum Field {
        ADDRESS(0), USERNAME(1), PASSWORD(2);
        public final int index;
        Field(int index) { this.index = index; }
        public Field next() { return values()[(ordinal() + 1) % values().length]; }
    }
}
