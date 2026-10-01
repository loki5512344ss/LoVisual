package dev.loki.lovisual.features.gui.hud.draggable.impl.voice;

import dev.loki.lovisual.compat.voicechat.VoiceChatBridge;
import dev.loki.lovisual.compat.voicechat.VoiceState;
import dev.loki.lovisual.features.gui.hud.config.HudBgStyles;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel.Palette;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.player.health.PlayerSkinResolver;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static dev.loki.lovisual.features.theme.Theme.theme;

/** Builds the rows of {@link VoiceChatHud} and feeds them to the shared scripted list panel. */
final class VoiceChatHudRenderer {
    private static final int TALKING = 0xFF55FF77;
    private static final int WHISPER = 0xFFFFD255;
    private static final int SILENT = 0xFFFF5555;
    private static final float WIDTH_PAD = 30.0f;

    private VoiceChatHudRenderer() {
    }

    private record Row(String key, Identifier skin, String name, String status, int statusColor, float alpha) { }

    static void render(VoiceChatHud host, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx,
                       float tickDelta, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        boolean preview = DraggableHudElementRegistry.isForceVisible() || ClientScreen.current() instanceof ChatScreen;
        if (mc == null || (mc.player == null && !preview)) { host.setSize(0f, 0f); return; }
        List<Row> rows = collectRows(host, mc, preview);
        host.visibilityAnim = HudRenderUtil.animateVisibility(host.visibilityAnim, !rows.isEmpty());
        if (rows.isEmpty() && host.visibilityAnim <= 0.0f) { host.setSize(0f, 0f); return; }

        TextRenderer header = font("Inter", FontInfo.Type.Bold, textRenderer);
        TextRenderer body = font("OnestMedium", FontInfo.Type.Regular, textRenderer);
        TextRenderer icons = font("IconsNur", FontInfo.Type.Regular, textRenderer);
        float base = HudScale.scale(screenW, screenH) * 1.1f * HudPanelLayoutModes.effectiveScale(host.scale);
        float fontScale = 0.98f * (host.hud.getFontSize() / 18.0f);
        float iconH = height(icons, fontScale);
        float titleH = height(header, fontScale);
        float rowH = height(body, fontScale);
        float titleW = width(header, I18n.get("hud.voicechat.title"), fontScale);
        float labelW = width(body, "Active:", fontScale * 0.92f);
        float countW = width(body, Integer.toString(rows.size()), fontScale * 0.92f);
        float maxW = Math.max(ScriptedListHudPanel.VOICECHAT.minWidth() * base,
                (ScriptedListHudPanel.VOICECHAT.titleTextX() * base) + titleW + labelW + countW + 34.0f * base);
        for (Row r : rows) {
            maxW = Math.max(maxW, width(body, r.name(), fontScale) + width(body, r.status(), fontScale) + WIDTH_PAD * base);
        }
        float targetH = ScriptedListHudPanel.CONTENT_START_Y * base + rows.size() * ScriptedListHudPanel.ROW_STEP * base;
        host.displayWidth = HudRenderUtil.animateDimension(host.displayWidth, maxW);
        host.displayHeight = HudRenderUtil.animateDimension(host.displayHeight, rows.isEmpty() ? 0f : targetH);
        host.setSize(host.displayWidth, host.displayHeight);

        float s = Math.max(0.0f, HudRenderUtil.visibilityScale(host.visibilityAnim));
        float w = host.displayWidth * s;
        float h = host.displayHeight * s;
        if (w <= 0.0f || h <= 0.0f) return;
        Palette palette = palette(host.bgAlpha.get());
        boolean blur = HudBgStyles.hasEffect(host.bgEffect.get());
        host.panel.render(renderer, textRenderer, ctx, tickDelta, new ScriptedListHudPanel.Panel(
                ScriptedListHudPanel.VOICECHAT, palette,
                host.getX() + (host.displayWidth - w) * 0.5f, host.getY() + (host.displayHeight - h) * 0.5f, w, h,
                s, base * s, fontScale * s, iconH * s, titleH * s, rowH * s, labelW * s, countW * s, rows.size(),
                blur, Math.min(1.0f, host.blurAlpha.get() / 255.0f * 1.15f), palette.counter(),
                HudPanelLayoutModes.current(host.layoutMode), toPanelRows(rows, palette)));
    }

    private static List<Row> collectRows(VoiceChatHud host, Minecraft mc, boolean preview) {
        if (!VoiceChatBridge.isInstalled()) {
            return List.of(new Row("missing", null, I18n.get("hud.voicechat.not_found"), "", SILENT, 1.0f));
        }
        VoiceState state = VoiceChatBridge.state();
        long hold = host.holdMs.get();
        List<Row> rows = new java.util.ArrayList<>();
        if (host.showSelf.get() && mc.player != null) {
            VoiceState.MicStatus mic = state.micStatus(hold);
            rows.add(new Row("self", PlayerSkinResolver.resolveProfileSkin(mc.player.getGameProfile()),
                    mc.player.getName().getString(), I18n.get("hud.voicechat.status." + mic.name().toLowerCase()),
                    statusColor(mic), 1.0f));
        }
        for (VoiceState.Speaker sp : state.speakers(hold, host.maxRows.get())) {
            PlayerInfo info = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(sp.id()) : null;
            String name = info != null && info.getProfile() != null ? info.getProfile().name() : shortId(sp.id());
            Identifier skin = info != null ? PlayerSkinResolver.resolveProfileSkin(info.getProfile()) : null;
            rows.add(new Row(sp.id().toString(), skin, name, sp.whisper() ? I18n.get("hud.voicechat.whisper") : "",
                    sp.whisper() ? WHISPER : TALKING, state.level(sp.id(), hold) * 0.6f + 0.4f));
        }
        if (rows.isEmpty() && preview && mc.player != null) {
            rows.add(new Row("example", PlayerSkinResolver.resolveProfileSkin(mc.player.getGameProfile()),
                    "Example Player", "", TALKING, 1.0f));
        }
        return rows;
    }

    private static List<LinkedHashMap<String, Object>> toPanelRows(List<Row> rows, Palette p) {
        List<LinkedHashMap<String, Object>> out = ScriptedListHudPanel.rows();
        for (Row r : rows) {
            int alpha = Math.round(255.0f * r.alpha());
            out.add(ScriptedListHudPanel.row(r.key(), r.skin() != null ? "head" : "", ScriptedListHudPanel.idString(r.skin()),
                    List.of(ScriptedListHudPanel.textPart(r.name(), withAlpha(p.text(), alpha), 0.0f)),
                    r.status(), withAlpha(r.statusColor(), alpha), withAlpha(p.muted(), Math.max(26, alpha - 120)), r.alpha()));
        }
        return out;
    }

    private static int statusColor(VoiceState.MicStatus mic) {
        return switch (mic) {
            case TALKING -> TALKING;
            case WHISPERING -> WHISPER;
            case MUTED, DISABLED, DISCONNECTED -> SILENT;
            default -> theme().textMuted();
        };
    }

    /** Theme-driven palette, the same recipe the other list panels use in theme mode. */
    private static Palette palette(int alpha) {
        int window = HudRenderUtil.setAlpha(theme().windowBg(), alpha);
        int header = HudRenderUtil.setAlpha(HudRenderUtil.mixColor(theme().windowHeader(), theme().surface(), 0.18f), Math.min(255, alpha + 14));
        int surface = HudRenderUtil.setAlpha(HudRenderUtil.mixColor(theme().surface(), theme().windowBg(), 0.22f), Math.min(255, alpha + 6));
        int deep = HudRenderUtil.setAlpha(HudRenderUtil.mixColor(theme().surface(), theme().windowHeader(), 0.42f), Math.min(255, alpha + 18));
        int headerRight = HudRenderUtil.mixColor(surface, header, 0.52f);
        int bodyRight = HudRenderUtil.mixColor(deep, surface, 0.18f);
        int muted = HudRenderUtil.mixColor(theme().textMuted(), theme().textPrimary(), 0.16f);
        return new Palette(HudRenderUtil.mixColor(header, window, 0.22f), headerRight,
                HudRenderUtil.mixColor(window, surface, 0.24f), bodyRight,
                HudRenderUtil.setAlpha(HudRenderUtil.mixColor(theme().windowStroke(), theme().strokeSoft(), 0.18f), Math.min(255, Math.max(182, alpha + 36))),
                theme().textPrimary(), muted, HudRenderUtil.mixColor(theme().accent(), theme().textPrimary(), 0.38f),
                HudRenderUtil.mixColor(theme().textPrimary(), theme().accent(), 0.14f), HudRenderUtil.scaleAlpha(muted, 0.68f),
                HudRenderUtil.mixColor(HudRenderUtil.mixColor(headerRight, bodyRight, 0.5f), theme().accent(), 0.14f));
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static TextRenderer font(String family, FontInfo.Type type, TextRenderer fallback) {
        TextRenderer r = Fonts.renderer(family, type, TextRenderer.get());
        return r != null ? r : fallback;
    }

    private static float height(TextRenderer r, float size) {
        r.begin(size, true, false);
        float v = (float) r.getHeight(false);
        r.end();
        return v;
    }

    private static float width(TextRenderer r, String text, float size) {
        r.begin(size, true, false);
        float v = (float) r.getWidth(text, false);
        r.end();
        return v;
    }
}
