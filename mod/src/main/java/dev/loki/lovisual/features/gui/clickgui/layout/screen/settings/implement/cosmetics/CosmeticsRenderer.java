package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.preview.PreviewCrop;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.preview.PreviewStore;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.other.StatusRender;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Draws the cosmetics pills and card grid, and remembers where things are for hit testing. */
final class CosmeticsRenderer {
    private static final int COLUMNS = 3;

    private record Rect(float x, float y, float w, float h) {
        boolean contains(float mx, float my) {
            return ClickGuiMath.insideRect(mx, my, x, y, w, h);
        }
    }

    private final Map<CosmeticKind, Rect> pills = new HashMap<>();
    private final List<Map.Entry<String, Rect>> cards = new ArrayList<>();
    private final Map<String, Float> enabledAnim = new HashMap<>();
    private final Map<String, Float> hoverAnim = new HashMap<>();
    private final Map<String, StatusRender> statuses = new HashMap<>();
    private final Map<CosmeticKind, Float> pillAnim = new HashMap<>();
    private final PreviewStore previews;
    private final Map<String, Rect> pictureButtons = new HashMap<>();
    private final Map<String, Rect> cropButtons = new HashMap<>();
    private float maxScroll;

    CosmeticsRenderer(PreviewStore previews) {
        this.previews = previews;
    }

    String pictureButtonAt(float mx, float my) {
        return buttonAt(pictureButtons, mx, my);
    }

    String cropButtonAt(float mx, float my) {
        return buttonAt(cropButtons, mx, my);
    }

    private static String buttonAt(Map<String, Rect> buttons, float mx, float my) {
        for (Map.Entry<String, Rect> b : buttons.entrySet()) {
            if (b.getValue().contains(mx, my)) return b.getKey();
        }
        return null;
    }

    float maxScroll() {
        return maxScroll;
    }

    CosmeticKind pillAt(float mx, float my) {
        for (Map.Entry<CosmeticKind, Rect> pill : pills.entrySet()) {
            if (pill.getValue().contains(mx, my)) return pill.getKey();
        }
        return null;
    }

    String cardAt(float mx, float my) {
        for (Map.Entry<String, Rect> card : cards) {
            if (card.getValue().contains(mx, my)) return card.getKey();
        }
        return null;
    }

    void render(CosmeticKind kind, List<Module> modules, float menuX, float menuY, float menuW, float menuH,
                float scroll, float s) {
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        float left = menuX + 32f * s;
        renderPills(kind, left, menuY + 33f * s, s, palette);

        cards.clear();
        pictureButtons.clear();
        cropButtons.clear();
        float gap = 8f * s;
        float areaW = 340f * s;
        float cardW = (areaW - gap * (COLUMNS - 1)) / COLUMNS;
        float cardH = 92f * s;
        float top = menuY + 56f * s;
        float bottom = menuY + menuH - 3.5f * s;
        boolean clipped = ScissorFunction.pushRaw(left - 4f * s, top, areaW + 8f * s, Math.max(1f, bottom - top));
        for (int i = 0; i < modules.size(); i++) {
            float x = left + (i % COLUMNS) * (cardW + gap);
            float y = top + (i / COLUMNS) * (cardH + gap) + scroll;
            if (y + cardH > top && y < bottom) drawCard(modules.get(i), x, y, cardW, cardH, s, palette);
        }
        if (clipped) ScissorFunction.pop();
        int rows = (modules.size() + COLUMNS - 1) / COLUMNS;
        maxScroll = Math.max(0f, rows * (cardH + gap) - gap - (bottom - top));
    }

    private void renderPills(CosmeticKind selected, float x, float y, float s, SettingsGuiPalette palette) {
        float size = 7.5f * s;
        float h = 14f * s;
        for (CosmeticKind kind : CosmeticKind.values()) {
            String label = kind.title();
            float w = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), label, size) + 14f * s;
            Rect rect = new Rect(x, y, w, h);
            pills.put(kind, rect);
            float target = kind == selected ? 1f : 0f;
            float anim = pillAnim.merge(kind, target, (old, t) -> AnimationUtility.approach(old, t, 0.2f));
            int base = SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelPillActive(), anim);
            LayoutRender2D.roundedQuad(x, y, w, h, h * 0.5f, base, base, base, base);
            int text = SettingsGuiPalette.mix(palette.panelMuted(), palette.panelText(), anim);
            float th = ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterMedium(), size);
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), label, x + 7f * s, y + (h - th) * 0.5f, size, text, false);
            x += w + 5f * s;
        }
    }

    private void drawCard(Module module, float x, float y, float w, float h, float s, SettingsGuiPalette palette) {
        String id = module.name();
        boolean hovered = ClickGuiMath.insideRect(ClickGuiRenderer.getMouseX(), ClickGuiRenderer.getMouseY(), x, y, w, h);
        float on = enabledAnim.merge(id, module.isEnabled() ? 1f : 0f, (old, t) -> AnimationUtility.approach(old, t, 0.16f));
        float hover = hoverAnim.merge(id, hovered ? 1f : 0f, (old, t) -> AnimationUtility.approach(old, t, 0.18f));
        cards.add(Map.entry(id, new Rect(x, y, w, h)));

        ClickGuiRenderer.drawRoundedRectShadow(x, y + 1.5f * s, w, h, 5f * s, 9f * s, s,
                LayoutRender2D.alpha(0xFF000000, 0.22f + 0.10f * hover));
        LayoutRender2D.roundedQuad(x, y, w, h, 5f * s, palette.moduleCardTop(), palette.moduleCardTopStrong(),
                palette.moduleCardBottom(), palette.moduleCardBottomStrong());

        float pad = 5f * s;
        float previewH = h - 30f * s;
        drawPreview(module, x + pad, y + pad, w - pad * 2f, previewH - pad, s, palette, on);

        float titleSize = 9f * s;
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), module.getDisplayName(), x + pad + 1f * s,
                y + h - 19f * s, titleSize, palette.moduleTitleText(), false);
        StatusRender status = statuses.computeIfAbsent(id, k -> new StatusRender());
        status.render(x + w - 24f * s, y + h - 14f * s, module.isEnabled() ? 1f : 0f, s);
    }

    /** The player's own picture when set, otherwise an accent gradient with the cosmetic's initial. */
    private void drawPreview(Module module, float x, float y, float w, float h, float s, SettingsGuiPalette palette, float on) {
        PreviewStore.Preview picture = previews.get(module.name());
        if (picture != null) {
            PreviewCrop c = picture.crop();
            ClickGuiRenderer.flushRenderer();
            Renderer2D.TEXTURE.roundedTexRect(x, y, w, h, 4f * s, 0f, c.u0(), c.v0(), c.u1(), c.v1(), 0xFFFFFFFF, picture.texture());
            ClickGuiRenderer.flushRenderer();
        } else {
            int a = SettingsGuiPalette.mix(palette.moduleCardBottom(), palette.moduleCardTopStrong(), 0.35f + 0.4f * on);
            int b = SettingsGuiPalette.mix(palette.moduleCardTop(), palette.panelPillActive(), 0.3f + 0.4f * on);
            LayoutRender2D.roundedQuad(x, y, w, h, 4f * s, a, b, a, b);
            String initial = module.getDisplayName().isEmpty() ? "?" : module.getDisplayName().substring(0, 1).toUpperCase();
            float size = 26f * s;
            float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), initial, size);
            float th = ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterRegular(), size);
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), initial, x + (w - tw) * 0.5f, y + (h - th) * 0.5f, size,
                    SettingsGuiPalette.withAlpha(palette.moduleTitleText(), 150 + Math.round(80f * on)), false);
        }
        float bx = x + w - 13f * s;
        drawSmallButton("+", bx, y + 2f * s, s, palette, pictureButtons, module.name());
        if (picture != null) {
            String label = ClickGuiI18n.tr("clickgui.settings.cosmetics.crop", "Crop");
            float lw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), label, 6.5f * s) + 8f * s;
            drawSmallButton(label, bx - lw - 3f * s, y + 2f * s, s, palette, cropButtons, module.name(), lw);
        }
    }

    private void drawSmallButton(String label, float x, float y, float s, SettingsGuiPalette palette,
                                 Map<String, Rect> sink, String id) {
        drawSmallButton(label, x, y, s, palette, sink, id, 11f * s);
    }

    private void drawSmallButton(String label, float x, float y, float s, SettingsGuiPalette palette,
                                 Map<String, Rect> sink, String id, float w) {
        float h = 11f * s;
        boolean hover = ClickGuiMath.insideRect(ClickGuiRenderer.getMouseX(), ClickGuiRenderer.getMouseY(), x, y, w, h);
        int fill = SettingsGuiPalette.withAlpha(hover ? palette.panelPillActive() : palette.panelPillBase(), hover ? 235 : 190);
        LayoutRender2D.roundedQuad(x, y, w, h, h * 0.5f, fill, fill, fill, fill);
        float size = 6.5f * s;
        float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), label, size);
        float th = ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterMedium(), size);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), label, x + (w - tw) * 0.5f, y + (h - th) * 0.5f, size, palette.panelText(), false);
        sink.put(id, new Rect(x, y, w, h));
    }
}
