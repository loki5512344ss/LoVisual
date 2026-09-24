package dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.math.RenderMath;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Draw pipeline of the custom health bar: glass background, scripted fill,
 * smoothed health/absorption animation and HP text layout. Package-private
 * helper of {@link CustomHealthBar} (impl subfolder, {@code bars} package).
 */
final class HealthBarPainter {

    private static final float HEALTH_BAR_RADIUS = 5.0f;
    private static final int HEALTH_TEXT_OFFSET_X = 0;
    private static final int HEALTH_TEXT_OFFSET_Y = 0;
    private static final int HEALTH_BAR_X_OFFSET = 0;
    private static final int HEALTH_BAR_Y_OFFSET = 0;
    private static final float HEALTH_ANIM_SPEED = 10.0f;

    private static final HealthScriptRenderer SCRIPT = new HealthScriptRenderer();

    private static float animatedHealth = -1.0f;
    private static float animatedAbsorb = -1.0f;
    private static long lastHealthAnimMs = -1L;
    private static int lastHealthPlayerId = Integer.MIN_VALUE;

    private HealthBarPainter() {
    }

    static CustomHealthBar.HealthTextInfo renderBackground(Renderer2D r2d, Minecraft mc, LocalPlayer player,
                                                           int x, int y, float maxHealth) {
        if (!CustomHealthBar.shouldRenderHealthBar(mc, player, maxHealth)) return null;

        CustomHealthBar bm = CustomHealthBar.get();
        if (bm == null || !bm.isHudHealthEnabled()) return null;

        float resolvedMaxHealth = Math.max(1.0f, player.getMaxHealth());
        float resolvedHealth = Mth.clamp(player.getHealth(), 0.0f, resolvedMaxHealth);
        float resolvedAbsorb = Math.max(0.0f, player.getAbsorptionAmount());

        updateHealthAnim(player, resolvedHealth, resolvedAbsorb);

        float baseRatio = Mth.clamp(animatedHealth / resolvedMaxHealth, 0.0f, 1.0f);
        float absorbRatio = Mth.clamp(animatedAbsorb / resolvedMaxHealth, 0.0f, 1.0f);

        float scale = (float) mc.getWindow().getGuiScale();

        float barX = x + HEALTH_BAR_X_OFFSET;
        float barY = y + HEALTH_BAR_Y_OFFSET;

        drawBar(r2d, barX, barY, scale, baseRatio, absorbRatio, bm);

        String text = formatHealthText(resolvedHealth + resolvedAbsorb);
        if (text.isEmpty()) return new CustomHealthBar.HealthTextInfo("", 0, 0, 0, bm.getTextScale());

        return layoutText(bm, text, barX, barY, Math.max(1.0f, scale));
    }

    /**
     * Lays out the HP digits in the currently active projection/batch. The bar
     * itself is submitted in framebuffer-space, so the text must stay in the
     * same unscaled ordered batch; otherwise the text can end up in a different
     * GUI stratum/projection and disappear behind later HUD work.
     */
    static void renderText(CustomHealthBar.HealthTextInfo info) {
        if (info == null || info.text.isEmpty()) return;
        TextRenderer tr = getHealthTextRenderer();
        tr.begin(info.scale, false, false);
        tr.render(info.text, info.x, info.y, new RenderColor(info.color), true);
        tr.end();
    }

    private static CustomHealthBar.HealthTextInfo layoutText(CustomHealthBar bm, String text,
                                                             float barX, float barY, float framebufferScale) {
        float textScale = bm.getTextScale() * framebufferScale;
        TextRenderer tr = getHealthTextRenderer();
        tr.begin(textScale, false, false);
        int textW = (int) Math.ceil(tr.getWidth(text, true));
        int textH = (int) Math.ceil(tr.getHeight(true));
        tr.end();

        float barW = bm.getBarWidth() * framebufferScale;
        float barH = bm.getBarHeight() * framebufferScale;
        float pad = bm.getTextPadding() * framebufferScale;
        int tx = Math.round(barX * framebufferScale + barW - textW - pad)
                + Math.round(HEALTH_TEXT_OFFSET_X * framebufferScale);
        int ty = Math.round(barY * framebufferScale + (barH - textH) * 0.5f)
                + Math.round(HEALTH_TEXT_OFFSET_Y * framebufferScale);

        int color = applyHealthAlpha(bm.getTextArgb(), bm);
        return new CustomHealthBar.HealthTextInfo(text, tx, ty, color, textScale);
    }

    private static void drawBar(Renderer2D r2d, float barX, float barY, float scale,
                                float baseRatio, float absorbRatio, CustomHealthBar bm) {
        if (r2d == null || bm == null) return;

        float barW = bm.getBarWidth();
        float barH = bm.getBarHeight();
        float radius = bm.getBarRadius();

        float xFb = barX * scale;
        float yFb = barY * scale;
        float wFb = barW * scale;
        float hFb = barH * scale;
        float roundFb = radius * scale;

        float glassScale = HEALTH_BAR_RADIUS <= 0.0f ? 1.0f : roundFb / HEALTH_BAR_RADIUS;
        Renderer2D.COLOR.liquidGlassRect(
                xFb,
                yFb,
                wFb,
                hFb,
                roundFb,
                0xFFFFFFFF,
                bm.getHudHealthAlphaFactor(),
                Renderer2D.LiquidGlassPreset.HEALTH_BAR,
                glassScale,
                0.0f
        );
        SCRIPT.render(bm, r2d, xFb, yFb, wFb, hFb, roundFb, baseRatio, absorbRatio);
    }

    private static void updateHealthAnim(LocalPlayer player, float health, float absorb) {
        int id = player.getId();
        long now = Util.getMillis();

        if (id != lastHealthPlayerId || lastHealthAnimMs < 0L) {
            animatedHealth = health;
            animatedAbsorb = absorb;
            lastHealthAnimMs = now;
            lastHealthPlayerId = id;
            return;
        }

        float dt = (now - lastHealthAnimMs) / 1000.0f;
        lastHealthAnimMs = now;

        animatedHealth = RenderMath.smoothLerp(animatedHealth, health, dt, HEALTH_ANIM_SPEED);
        animatedAbsorb = RenderMath.smoothLerp(animatedAbsorb, absorb, dt, HEALTH_ANIM_SPEED);
    }

    static String formatHealthText(float value) {
        if (value <= 0.0f) return "0";
        float rounded = Math.round(value * 10.0f) / 10.0f;
        if (Math.abs(rounded - Math.round(rounded)) < 0.001f) {
            return String.valueOf(Math.round(rounded));
        }
        return String.valueOf(rounded);
    }

    private static TextRenderer getHealthTextRenderer() {
        return Fonts.renderer("Iosevka");
    }

    private static int applyHealthAlpha(int argb, CustomHealthBar bm) {
        if (bm == null) return argb;
        int a = (argb >>> 24) & 0xFF;
        int na = (int) (a * bm.getHudHealthAlphaFactor());
        return (argb & 0x00FFFFFF) | ((na & 0xFF) << 24);
    }
}
