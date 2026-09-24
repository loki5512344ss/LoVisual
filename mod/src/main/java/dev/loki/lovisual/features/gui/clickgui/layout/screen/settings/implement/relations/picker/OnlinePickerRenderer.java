
package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiRichTextRenderer;
import dev.loki.lovisual.features.module.modules.misc.DefineTarget;
import dev.loki.lovisual.features.relations.PlayerRelations;
import dev.loki.lovisual.features.relations.category.CategoryType;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.render.helpers.player.PlayerHeadRenderer;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.List;

final class OnlinePickerRenderer {
    private static final String I18N = "clickgui.settings.relations.online_picker.";
    void render(OnlineRelationPlayerPickerComponent host, float x, float y, float w, float h, float mx, float my, float scale, SettingsGuiPalette palette) {
        float dt = AnimationUtility.deltaTime();
        host.openAnim = AnimationUtility.approach(host.openAnim, host.openTarget ? 1f : 0f, dt, host.openTarget ? 13f : 18f);
        host.openAnim = AnimationUtility.snap(host.openAnim, host.openTarget ? 1f : 0f, 0.003f);
        host.searchAnim = AnimationUtility.approach(host.searchAnim, host.searchFocused ? 1f : 0f, dt, 12f);
        host.searchAnim = AnimationUtility.snap(host.searchAnim, host.searchFocused ? 1f : 0f, 0.004f);
        if (!host.isVisible()) return;
        host.bounds = new OnlineRelationPlayerPickerComponent.Rect(x, y, w, h);
        host.rowHits.clear();
        host.data.refreshIfNeeded();
        float enter = AnimationUtility.easeOutBack(host.openAnim, 0.42f);
        float exit = AnimationUtility.easeInOutCubic(host.openAnim);
        float visual = host.openTarget ? enter : exit;
        float opacity = host.openTarget ? AnimationUtility.clamp01(host.openAnim * 1.16f) : AnimationUtility.smoothstep(host.openAnim);
        float radius = 7.5f * scale;
        float lift = host.openTarget ? (1f - visual) * 4f * scale : 0f;
        float panelY = y + lift;
        float panelH = host.openTarget ? h : Math.max(1f, h * (0.74f + 0.26f * visual));
        float panelBottom = panelY + panelH;
        ClickGuiRenderer.drawBlur(x, panelY, w, panelH, radius, palette.panelBlurTint(), (170f / 255f) * opacity);
        LayoutRender2D.roundedSoftShadow(x, panelY + 2f * scale, w, panelH, radius, 15f * scale, 0.18f * opacity, fade(SettingsGuiPalette.withAlpha(0xFF000000, 140), opacity));
        LayoutRender2D.roundedQuad(x, panelY, w, panelH, radius, fade(SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.panelBgLeft(), palette.menuCategorySelectedLeft(), 0.18f), 216), opacity), fade(SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.panelBgRight(), palette.menuCategorySelectedRight(), 0.16f), 208), opacity), fade(SettingsGuiPalette.withAlpha(SettingsGuiPalette.darken(palette.panelBgRight(), 0.12f), 214), opacity), fade(SettingsGuiPalette.withAlpha(SettingsGuiPalette.darken(palette.panelBgLeft(), 0.10f), 220), opacity));
        Renderer2D.COLOR.radialGlowMasked(x, panelY, w, panelH, radius, 0f, 92f * scale, x + 42f * scale, panelY + 30f * scale, LayoutRender2D.alpha(modeColor(host), 0.20f * opacity));
        LayoutRender2D.roundedStrokeQuad(x, panelY, w, panelH, radius, 0.65f * scale, LayoutRender2D.alpha(modeColor(host), 0.42f * opacity), LayoutRender2D.alpha(palette.menuWindowStroke(), 0.78f * opacity), LayoutRender2D.alpha(palette.menuWindowStroke(), 0.50f * opacity), LayoutRender2D.alpha(modeColor(host), 0.26f * opacity));
        float pad = 8f * scale;
        float headerH = 28f * scale;
        renderHeader(host, x + pad, panelY + pad, w - pad * 2f, headerH - pad, mx, my, scale, palette, opacity);
        float listX = x + pad;
        float listY = panelY + headerH + 4f * scale;
        float listW = w - pad * 2f;
        float listH = Math.max(1f, panelBottom - pad - listY);
        host.listRect = new OnlineRelationPlayerPickerComponent.Rect(listX, listY, listW, listH);
        renderRows(host, listX, listY, listW, listH, mx, my, scale, palette, opacity);
    }
    private void renderHeader(OnlineRelationPlayerPickerComponent host, float x, float y, float w, float h, float mx, float my, float scale, SettingsGuiPalette palette, float opacity) {
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), tr("title", "Online players"), x, y + 1.5f * scale, 8.2f * scale, fade(palette.panelText(), opacity), false);
        String subtitle = host.data.search == null || host.data.search.isBlank() ? tr("subtitle", "Pick from current tab list") : tr("subtitle_search", "%s / %s visible", host.data.filteredRows().size(), host.data.rows.size());
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), subtitle, x, y + 13f * scale, 6.15f * scale, fade(palette.panelMuted(), opacity), false);
        float btn = 17f * scale;
        host.closeButton = new OnlineRelationPlayerPickerComponent.Rect(x + w - btn, y, btn, btn);
        host.searchButton = new OnlineRelationPlayerPickerComponent.Rect(0f, 0f, 0f, 0f);
        host.searchField = new OnlineRelationPlayerPickerComponent.Rect(0f, 0f, 0f, 0f);
        drawSmallIconButton(host.closeButton, "x", false, host.closeButton.contains(mx, my), scale, palette, opacity);
    }
    private void renderRows(OnlineRelationPlayerPickerComponent host, float x, float y, float w, float h, float mx, float my, float scale, SettingsGuiPalette palette, float opacity) {
        List<OnlinePickerData.PlayerRow> filtered = host.data.filteredRows();
        if (filtered.isEmpty()) {
            String t = host.data.search.isBlank() ? tr("empty", "No online players.") : tr("empty_search", "No matching online players.");
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), t, x + 5f * scale, y + 5f * scale, 7.2f * scale, fade(palette.panelMuted(), opacity), false);
            host.scroll.updateMetrics(x, y, w, h, 0f, scale);
            return;
        }
        float reserved = 9f * scale;
        float gap = 5f * scale;
        float rowH = 31f * scale;
        float stepY = rowH + gap;
        float contentH = filtered.size() * stepY - gap;
        float maxScroll = Math.max(0f, contentH - h);
        host.scroll.updateMetrics(x, y, w, h, maxScroll, scale);
        float rowAreaW = host.scroll.scrollbarVisible ? Math.max(1f, w - reserved) : w;
        host.scroll.dragIfNeeded();
        host.scroll.clampAndSmooth(maxScroll);
        boolean clipped = ScissorFunction.pushRaw(x, y, w, h);
        for (int i = 0; i < filtered.size(); i++) {
            float rowY = y + i * stepY + host.scroll.smoothedScroll;
            if (rowY + rowH < y || rowY > y + h) continue;
            renderRow(host, filtered.get(i), x, rowY, rowAreaW, rowH, mx, my, scale, palette, i, opacity);
        }
        if (clipped) ScissorFunction.pop();
        host.scroll.renderScrollbar(x, y, w, h, maxScroll, scale, palette, opacity);
    }
    private void renderRow(OnlineRelationPlayerPickerComponent host, OnlinePickerData.PlayerRow player, float x, float y, float w, float h, float mx, float my, float scale, SettingsGuiPalette palette, int index, float opacity) {
        float base = host.openTarget ? AnimationUtility.easeOutCubic(host.openAnim) : 1f;
        float anim = AnimationUtility.clamp((base - index * 0.018f) / 0.72f, 0f, 1f);
        y += (1f - (host.openTarget ? AnimationUtility.easeOutCubic(anim) : 1f)) * 7f * scale;
        boolean hover = host.openTarget && ClickGuiMath.insideRect(mx, my, x, y, w, h);
        boolean selected = DefineTarget.isPlayerInRelationMode(host.mode, player.name());
        int relCol = player.relationColor() != 0 ? player.relationColor() : modeColor(host);
        int top = hover ? SettingsGuiPalette.mix(palette.moduleCardTop(), modeColor(host), 0.10f) : palette.moduleCardTop();
        int topStrong = hover || selected ? SettingsGuiPalette.mix(palette.moduleCardTopStrong(), relCol, selected ? 0.18f : 0.10f) : palette.moduleCardTopStrong();
        int bottom = hover ? SettingsGuiPalette.mix(palette.moduleCardBottom(), modeColor(host), 0.12f) : palette.moduleCardBottom();
        int bottomStrong = selected ? SettingsGuiPalette.mix(palette.moduleCardBottomStrong(), relCol, 0.20f) : palette.moduleCardBottomStrong();
        LayoutRender2D.roundedSoftShadow(x, y + 1f * scale, w, h, 5.5f * scale, 8f * scale, 0.08f, fade(SettingsGuiPalette.withAlpha(0xFF000000, hover ? 96 : 58), opacity));
        LayoutRender2D.roundedQuad(x, y, w, h, 5.5f * scale, fade(top, opacity), fade(topStrong, opacity), fade(bottomStrong, opacity), fade(bottom, opacity));
        Renderer2D.COLOR.radialGlowMasked(x, y, w, h, 5.5f * scale, 0f, 42f * scale, x + 15f * scale, y + h * 0.50f, LayoutRender2D.alpha(relCol, (selected ? 0.28f : 0.14f) * opacity));
        LayoutRender2D.roundedStrokeQuad(x, y, w, h, 5.5f * scale, 0.55f * scale, LayoutRender2D.alpha(relCol, (selected ? 0.70f : 0.34f) * opacity), LayoutRender2D.alpha(palette.menuWindowStroke(), 0.72f * opacity), LayoutRender2D.alpha(palette.menuWindowStroke(), 0.44f * opacity), LayoutRender2D.alpha(relCol, (selected ? 0.46f : 0.20f) * opacity));
        float head = 18.5f * scale;
        float headX = x + 6f * scale;
        float headY = y + (h - head) * 0.5f;
        renderHead(host.data.resolveSkin(player), headX, headY, head, scale, palette, opacity);
        float dot = 5.3f * scale;
        float dotX = headX + head - dot * 0.72f;
        float dotY = headY + head - dot * 0.72f;
        Renderer2D.COLOR.radialGlowMasked(dotX - 4f * scale, dotY - 4f * scale, dot + 8f * scale, dot + 8f * scale, (dot + 8f * scale) * 0.5f, 0f, 11f * scale, dotX + dot * 0.5f, dotY + dot * 0.5f, LayoutRender2D.alpha(relCol, 0.48f * opacity));
        Renderer2D.COLOR.roundedRect(dotX, dotY, dot, dot, dot * 0.5f, 0.7f, fade(relCol | 0xFF000000, opacity));
        Renderer2D.COLOR.roundedRectStroke(dotX, dotY, dot, dot, dot * 0.5f, 0.7f, 0.55f * scale, LayoutRender2D.alpha(0xFFFFFFFF, 0.42f * opacity));
        float textX = headX + head + 7f * scale;
        float rightPad = selected ? 56f * scale : 44f * scale;
        float pingW = Math.max(38f * scale, ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), pingText(player.latency()), 6.2f * scale) + 26f * scale);
        float nameW = Math.max(20f * scale, w - (textX - x) - rightPad - pingW);
        int nameColor = player.spectator() ? palette.panelMuted() : palette.panelText();
        ClickGuiRichTextRenderer.draw(player.displayName(), textX, y + 6f * scale, nameW, 8f * scale, nameColor, opacity, false);
        String relation = relationLabel(player.relation());
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), ClickGuiRenderer.fitText(ClickGuiRenderer.getInterRegular(), relation, 5.5f * scale, nameW), textX, y + 18.6f * scale, 5.5f * scale, fade(SettingsGuiPalette.mix(palette.panelMuted(), relCol | 0xFF000000, 0.45f), opacity), false);
        float pingH = 13f * scale;
        drawPingChip(player.latency(), x + w - 6f * scale - pingW, y + (h - pingH) * 0.5f, pingW, pingH, scale, palette, opacity);
        OnlineRelationPlayerPickerComponent.Rect hit = new OnlineRelationPlayerPickerComponent.Rect(x, y, w, h);
        host.rowHits.add(new OnlineRelationPlayerPickerComponent.RowHit(player.name(), hit));
        if (hover) SystemCursor.set(SystemCursor.CursorType.HAND);
    }
    private void renderHead(Identifier skin, float x, float y, float size, float scale, SettingsGuiPalette palette, float opacity) {
        float o = AnimationUtility.clamp01(opacity * ClickGuiRenderer.getRenderAlphaMultiplier());
        if (o <= 0.01f) return;
        GuiGraphicsExtractor ctx = ViewportContext.getCurrentContext();
        if (skin != null && ctx != null) {
            int a = Math.round(255f * o);
            PlayerHeadRenderer.drawRounded(ctx, x, y, size, 4.2f * scale, skin, new RenderColor(255, 255, 255, a), true, new RenderColor(255, 255, 255, Math.round(48f * o)), 0.95f * scale, false);
            return;
        }
        Renderer2D.COLOR.roundedRect(x, y, size, size, 4.2f * scale, 1f, LayoutRender2D.alpha(palette.panelMuted(), 0.22f * o));
        Renderer2D.COLOR.roundedRectStroke(x, y, size, size, 4.2f * scale, 1f, 0.55f * scale, LayoutRender2D.alpha(palette.panelMuted(), 0.62f * o));
    }
    private void drawPingChip(int latency, float x, float y, float w, float h, float scale, SettingsGuiPalette palette, float opacity) {
        String text = pingText(latency);
        int base = pingBaseColor(latency);
        int chipA = LayoutRender2D.alpha(palette.panelPillBase(), 0.84f * opacity);
        int chipB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillBase(), palette.moduleCardBottomStrong(), 0.38f), 0.52f * opacity);
        LayoutRender2D.roundedQuad(x, y, w, h, 5.4f * scale, chipA, chipA, chipB, chipB);
        LayoutRender2D.roundedStrokeQuad(x, y, w, h, 5.4f * scale, 0.55f * scale, LayoutRender2D.alpha(palette.panelStroke(), 0.40f * opacity), LayoutRender2D.alpha(palette.moduleDividerEnd(), 0.26f * opacity), LayoutRender2D.alpha(palette.moduleDividerEnd(), 0.18f * opacity), LayoutRender2D.alpha(palette.panelStroke(), 0.30f * opacity));
        float cx = x + 7.3f * scale;
        float cy = y + h * 0.5f;
        float r = 3.1f * scale;
        Renderer2D.COLOR.circleSoftShadow(cx, cy, r + 1.5f * scale, 5f * scale, 0.34f, LayoutRender2D.alpha(base, 0.48f * opacity));
        Renderer2D.COLOR.circle(cx, cy, r + 0.95f * scale, 0.8f * scale, LayoutRender2D.alpha(base, 0.30f * opacity));
        Renderer2D.COLOR.circle(cx, cy, r, 0.75f * scale, LayoutRender2D.alpha(base, opacity));
        Renderer2D.COLOR.circleStroke(cx, cy, r + 0.72f * scale, 0.55f * scale, 0.8f * scale, LayoutRender2D.alpha(0xFFFFFFFF, 0.36f * opacity));
        Renderer2D.COLOR.arcStrokeGradient(cx, cy, r + 1.55f * scale, 0.9f * scale, -140f, 70f, 0.45f * scale, LayoutRender2D.alpha(base, 0.68f * opacity), LayoutRender2D.alpha(0xFFFFFFFF, 0.16f * opacity), 35f, 0f);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), text, x + 14.5f * scale, y + 3.3f * scale, 6.2f * scale, fade(palette.panelText(), opacity), false);
    }
    private void drawSmallIconButton(OnlineRelationPlayerPickerComponent.Rect rect, String icon, boolean active, boolean hover, float scale, SettingsGuiPalette palette, float opacity) {
        int color = active ? modeColor(null) : palette.panelMuted();
        // host required for modeColor; if null fallback to panelMuted
        int bgA = active || hover ? SettingsGuiPalette.mix(palette.panelPillBase(), color, active ? 0.30f : 0.14f) : LayoutRender2D.alpha(palette.panelPillBase(), 0.82f);
        int bgB = active || hover ? SettingsGuiPalette.mix(palette.moduleCardTopStrong(), color, active ? 0.18f : 0.08f) : LayoutRender2D.alpha(palette.panelPillBase(), 0.70f);
        LayoutRender2D.roundedQuad(rect.x(), rect.y(), rect.w(), rect.h(), 4.3f * scale, fade(bgA, opacity), fade(bgB, opacity), fade(bgB, opacity), fade(bgA, opacity));
        LayoutRender2D.roundedStroke(rect.x(), rect.y(), rect.w(), rect.h(), 4.3f * scale, 0.45f * scale, LayoutRender2D.alpha(color, (active || hover ? 0.56f : 0.24f) * opacity));
        Renderer2D.COLOR.svg(icon, rect.x() + 4.1f * scale, rect.y() + 4.1f * scale, rect.w() - 8.2f * scale, rect.h() - 8.2f * scale, SvgRenderOptions.overrideColor(fade(active ? palette.panelText() : SettingsGuiPalette.mix(palette.panelMuted(), palette.panelText(), hover ? 0.25f : 0f), opacity)));
        if (hover) SystemCursor.set(SystemCursor.CursorType.HAND);
    }
    static int fade(int c, float o) { return LayoutRender2D.alpha(c, AnimationUtility.clamp01(o)); }
    static int modeColor(OnlineRelationPlayerPickerComponent host) {
        if (host == null) return 0xFF8A8A8A;
        return host.mode.color(PlayerRelations.get()) | 0xFF000000;
    }
    static int pingBaseColor(int l) { if (l < 0) return 0xFF8993A3; if (l < 90) return 0xFF52F2A1; if (l < 180) return 0xFFFFD166; return 0xFFFF5A6E; }
    static String pingText(int l) { return l < 0 ? "?" : String.valueOf(l); }
    static String relationLabel(CategoryType t) {
        if (t == null) return tr("relation.default", "Default");
        return switch (t) { case FRIEND -> tr("relation.friend", "Friend"); case ENEMY -> tr("relation.enemy", "Enemy"); case STAFF -> tr("relation.staff", "Staff"); case BEDWARS_SELF -> tr("relation.bedwars_self", "Bedwars Self"); case BEDWARS_ENEMY -> tr("relation.bedwars_enemy", "Bedwars Enemy"); default -> tr("relation.default", "Default"); };
    }
    static String tr(String k, String f, Object... a) { return ClickGuiI18n.tr(I18N + k, f, a); }
}
