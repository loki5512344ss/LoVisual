package dev.loki.lovisual.features.module.modules.visuals.esp.render;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.helpers.clip.ScreenProjection;
import dev.loki.lovisual.util.player.health.PlayerHealthResolver;
import dev.loki.lovisual.features.module.modules.visuals.esp.ESP;
import dev.loki.lovisual.features.module.modules.visuals.esp.EspColorUtil;
import dev.loki.lovisual.features.module.modules.visuals.esp.EspModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import dev.loki.lovisual.features.module.modules.visuals.esp.render.EspOutlineRenderer;

public final class Esp2DRenderer {

    private Esp2DRenderer() {
    }

    public static void renderPlayerEntries(ESP esp, Renderer2D renderer, float tickDelta, List<EspModel.RenderEntry> entries) {
        entries.sort(Comparator.comparingDouble(EspModel.RenderEntry::distSq).reversed());
        for (EspModel.RenderEntry entry : entries) {
            draw2DESP(esp, renderer, entry.entity(), tickDelta, entry.color(), entry.distSq());
        }
    }

    static void draw2DESP(ESP esp, Renderer2D renderer, Entity entity, float tickDelta, int baseColor, double distSq) {
        AABB box = resolveWorldBox(entity, tickDelta);
        EspModel.ScreenBox screen = project(box, tickDelta);
        if (screen == null) return;

        drawBoxESP(esp, renderer, screen.minX(), screen.minY(), screen.maxX(), screen.maxY(), baseColor);
        if (esp.healthBar.get()) {
            double maxDist = esp.healthBarDistance.get();
            if (distSq <= maxDist * maxDist) {
                drawHealthBar(esp, renderer, screen.minX(), screen.minY(), screen.maxX(), screen.maxY(), entity);
            }
        }
    }

    static AABB resolveWorldBox(Entity entity, float tickDelta) {
        Vec3 pos = entity.getPosition(tickDelta);
        AABB box = entity.getBoundingBox().move(
                pos.x - entity.getX(),
                pos.y - entity.getY(),
                pos.z - entity.getZ()
        );

        return new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY + ESP.AABB_TOP_OFFSET, box.maxZ);
    }

    static EspModel.ScreenBox project(AABB box, float tickDelta) {
        Vec3[] pts = new Vec3[]{
                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ),
                new Vec3(box.minX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ)
        };

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Vec3 p : pts) {
            Vec3 screen = ScreenProjection.worldToScreen(p, tickDelta);
            if (screen == null) continue;
            minX = Math.min(minX, screen.x);
            minY = Math.min(minY, screen.y);
            maxX = Math.max(maxX, screen.x);
            maxY = Math.max(maxY, screen.y);
        }

        if (minX == Double.POSITIVE_INFINITY) return null;
        if (maxX <= minX || maxY <= minY) return null;

        if (ESP.PIXEL_SNAP) {
            minX = Math.floor(minX);
            minY = Math.floor(minY);
            maxX = Math.ceil(maxX);
            maxY = Math.ceil(maxY);
        }

        return new EspModel.ScreenBox(minX, minY, maxX, maxY);
    }

    static void drawBoxESP(ESP esp, Renderer2D renderer, double minX, double minY, double maxX, double maxY, int baseColor) {
        int baseRgb = baseColor & 0x00FFFFFF;
        int darkRgb = EspColorUtil.mixRgb(baseRgb, 0x000000, ESP.GRAD_DARKEN);
        int lightRgb = EspColorUtil.mixRgb(baseRgb, 0xFFFFFF, ESP.GRAD_LIGHTEN);

        EspModel.Gradient gradient = new EspModel.Gradient(
                EspColorUtil.withAlpha(lightRgb, ESP.COLOR_ALPHA),
                EspColorUtil.withAlpha(baseRgb, ESP.COLOR_ALPHA),
                EspColorUtil.withAlpha(lightRgb, ESP.COLOR_ALPHA),
                EspColorUtil.withAlpha(darkRgb, ESP.COLOR_ALPHA)
        );
        int outline = EspColorUtil.withAlpha(0x000000, ESP.OUTLINE_ALPHA);

        EspOutlineRenderer.drawOutline(esp, renderer,
                minX - ESP.OUTLINE_EXPAND, minY - ESP.OUTLINE_EXPAND,
                maxX + ESP.OUTLINE_EXPAND, maxY + ESP.OUTLINE_EXPAND,
                ESP.OUTLINE_THICKNESS, EspModel.SolidColor.of(outline));

        EspOutlineRenderer.drawOutline(esp, renderer, minX, minY, maxX, maxY, ESP.COLOR_THICKNESS, gradient);

        double innerInset = ESP.COLOR_THICKNESS;
        EspOutlineRenderer.drawOutline(esp, renderer,
                minX + innerInset, minY + innerInset,
                maxX - innerInset, maxY - innerInset,
                ESP.OUTLINE_THICKNESS, EspModel.SolidColor.of(outline));
    }

    static void drawHealthBar(ESP esp, Renderer2D renderer, double x1, double y1, double x2, double y2, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        double height = y2 - y1;
        if (height <= 0.0) return;

        PlayerHealthResolver.HealthSnapshot health = PlayerHealthResolver.resolve(living);
        float max = health.maxHealth();
        if (max <= 0f) return;
        float current = health.totalHealth();
        float ratio = EspColorUtil.clamp01(current / max);
        float absorption = Math.max(0f, health.absorption());
        float absorptionRatio = EspColorUtil.clamp01(absorption / max);

        double barX = x1 - ESP.HEALTH_BAR_GAP - ESP.HEALTH_BAR_WIDTH;
        double barY = y1;

        renderer.quad(barX, barY, ESP.HEALTH_BAR_WIDTH, height, 0xFF000000);

        double inset = Math.min(ESP.HEALTH_BAR_INSET, Math.min(ESP.HEALTH_BAR_WIDTH * 0.45, height * 0.1));
        double innerX = barX + inset;
        double innerY = barY + inset;
        double innerW = Math.max(0.0, ESP.HEALTH_BAR_WIDTH - inset * 2.0);
        double innerH = Math.max(0.0, height - inset * 2.0);

        double fillH = innerH * ratio;
        if (fillH <= 0.0) return;
        double fillY = innerY + (innerH - fillH);
        if (esp.healthBarGradient.get()) {
            float topT = EspColorUtil.clamp01((float) (fillH / Math.max(0.0001, innerH)));
            int topRgb = EspColorUtil.mixRgb(0xFF0000, 0x00FF00, topT);
            int bottomRgb = 0xFF0000;
            renderer.quad(
                    innerX, fillY, innerW, fillH,
                    EspColorUtil.withAlpha(topRgb, ESP.HEALTH_ALPHA),
                    EspColorUtil.withAlpha(topRgb, ESP.HEALTH_ALPHA),
                    EspColorUtil.withAlpha(bottomRgb, ESP.HEALTH_ALPHA),
                    EspColorUtil.withAlpha(bottomRgb, ESP.HEALTH_ALPHA)
            );
        } else {
            int rgb = EspColorUtil.healthRgb(ratio);
            renderer.quad(innerX, fillY, innerW, fillH, EspColorUtil.withAlpha(rgb, ESP.HEALTH_ALPHA));
        }

        if (absorptionRatio > 0f) {
            float totalRatio = EspColorUtil.clamp01(current / max);
            double absorbH = innerH * Math.min(absorptionRatio, totalRatio);
            if (absorbH > 0.0) {
                double absorbY = innerY + (innerH - absorbH);
                renderer.quad(innerX, absorbY, innerW, absorbH, EspColorUtil.withAlpha(ESP.ABSORB_RGB, ESP.HEALTH_ALPHA));
            }
        }
    }
}
