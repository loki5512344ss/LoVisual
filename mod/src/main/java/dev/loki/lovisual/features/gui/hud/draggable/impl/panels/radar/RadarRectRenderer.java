package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.relations.category.CategoryService;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import static dev.loki.lovisual.features.theme.Theme.theme;

final class RadarRectRenderer {

    private RadarRectRenderer() {}

    static void renderRect(Radar host, Renderer2D renderer, float tickDelta, int screenW, int screenH) {
        float scale = HudScale.scale(screenW, screenH)
                * (host.hud.getFontSize() / 18f);
        float size = host.size.get().floatValue() * scale;
        float baseX = host.getX();
        float baseY = host.getY();
        float radius = Math.min(Radar.BASE_RADIUS * scale, size * 0.25f);
        float lineW = Math.max(0.5f, Radar.BASE_LINE_W * scale);
        float pad = Radar.BASE_PAD * scale;

        int bgAlpha = host.bgAlpha.get();
        boolean syncTheme = host.colorMode.get() == Radar.ColorMode.SYNC;
        int bgRgb = syncTheme
                ? (theme().windowBg() & 0x00FFFFFF)
                : (host.bgColor.getArgb() & 0x00FFFFFF);
        int bg = (bgAlpha << 24) | bgRgb;

        boolean blurEnabled = host.blur.get();
        if (blurEnabled) {
            drawBlur(host, baseX, baseY, size, size, radius, bg);
        }
        boolean useGradient = syncTheme && host.gradient.get();
        HudRenderUtil.drawHudBackground(renderer, baseX, baseY, size, size, radius, 1.0f, bg, useGradient);

        int stroke = HudRenderUtil.setAlpha(theme().accent(), 0x55);
        renderer.roundedRectStroke(baseX, baseY, size, size, radius, 1.0f,
                Math.max(1.0f, 1.25f * scale), stroke);

        int cross = HudRenderUtil.setAlpha(theme().textMuted(), 0x55);
        float cx = baseX + size * 0.5f;
        float cy = baseY + size * 0.5f;
        renderer.quad(cx - lineW * 0.5f, baseY + pad, lineW, size - pad * 2f, cross);
        renderer.quad(baseX + pad, cy - lineW * 0.5f, size - pad * 2f, lineW, cross);

        renderBlips(renderer, tickDelta, cx, cy, size * 0.5f - Radar.BLIP_MARGIN * scale, scale);

        host.setSize(size, size);
    }

    static void renderBlips(Renderer2D renderer, float tickDelta, float cx, float cy, float half, float scale) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;

        float blip = Math.max(2.5f, Radar.BLIP_SIZE * scale);
        float blipRadius = Math.max(1.0f, blip * 0.5f);
        float yaw = mc.player.getViewYRot(tickDelta);
        float yawRad = (float) Math.toRadians(yaw);
        float cos = Mth.cos(yawRad);
        float sin = Mth.sin(yawRad);

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;

            double px = player.getX();
            double pz = player.getZ();
            double dx = px - mc.player.getX();
            double dz = pz - mc.player.getZ();

            float rotY = (float) (-(dz * cos - dx * sin)) * 2.0f;
            float rotX = (float) (-(dx * cos + dz * sin)) * 2.0f;

            rotX = Mth.clamp(rotX, -half, half);
            rotY = Mth.clamp(rotY, -half, half);

            int color = CategoryService.getColor(player);
            renderer.roundedRect(cx + rotX - blip * 0.5f, cy + rotY - blip * 0.5f,
                    blip, blip, blipRadius, 1.0f, color);
        }
    }

    static void drawBlur(Radar host, float x, float y, float w, float h, float radius, int tintRgb) {
        float quality = host.hud.getBlurRadius();
        float brightness = 1.0f;
        float alpha = host.blurAlpha.get() / 255f;
        Renderer2D.COLOR.blurRect(x, y, w, h, radius, quality, brightness, alpha, 0xFFFFFF);
    }
}
