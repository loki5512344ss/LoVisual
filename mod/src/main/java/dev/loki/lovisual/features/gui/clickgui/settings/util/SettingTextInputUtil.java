package dev.loki.lovisual.features.gui.clickgui.settings.util;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;

public final class SettingTextInputUtil {
    private SettingTextInputUtil() {
    }

    public static int caretFromText(TextRenderer font, String text, float size, float textX, float mx) {
        if (text == null || text.isEmpty()) return 0;
        float rel = mx - textX;
        if (rel <= 0f) return 0;
        for (int i = 1; i <= text.length(); i++) {
            float prev = UnifiedSettingsSkin.textWidth(font, text.substring(0, i - 1), size);
            float cur = UnifiedSettingsSkin.textWidth(font, text.substring(0, i), size);
            if (rel < (prev + cur) * 0.5f) return i - 1;
        }
        return text.length();
    }

    public static void renderInlineSelection(TextRenderer font, String text, int start, int end, float textX, float textY, float size, float fieldH, float fieldY) {
        if (text == null || start < 0 || end < 0 || start == end) return;
        int a = Math.max(0, Math.min(start, text.length()));
        int b = Math.max(0, Math.min(end, text.length()));
        if (a == b) return;
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
        float x0 = textX + UnifiedSettingsSkin.textWidth(font, text.substring(0, a), size);
        float x1 = textX + UnifiedSettingsSkin.textWidth(font, text.substring(0, b), size);
        float h = Math.max(SettingSkinUtil.m(8f, 5f), UnifiedSettingsSkin.textHeight(font, size) + SettingSkinUtil.m(3f, 2f));
        float y = fieldY > 0f ? fieldY + (fieldH - h) * 0.5f : textY - SettingSkinUtil.m(1.5f, 0.7f);
        ClickGuiRenderer.drawRoundedRect(x0, y, Math.max(SettingSkinUtil.m(1f, 0.7f), x1 - x0), h, SettingSkinUtil.m(2f, 1.5f), UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.ACCENT, 92));
    }
}
