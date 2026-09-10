/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.visibility;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.features.relations.category.CategoryService;
import dev.loki.lovisual.features.relations.category.CategoryType;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.features.module.modules.visuals.esp.shader.EspMeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Colored wireframe/fill rendering of entity hitboxes.
 * Render-only: it never changes collision boxes (that would be a cheat).
 */
@ModuleInfo(
        id = "hitboxes",
        displayName = "Hitboxes",
        category = ModuleCategory.VISUALS
)
public class Hitboxes extends Module {

    private static final String MODE_OUTLINE = "Outline";
    private static final String MODE_FILL = "Fill";
    private static final String MODE_BOTH = "Both";
    private static final String COLOR_SYNC = "Sync";
    private static final String COLOR_CUSTOM = "Custom";

    private final ModeValue renderMode =
            mode("hitboxesRenderMode", "render_mode", MODE_BOTH, MODE_OUTLINE, MODE_FILL, MODE_BOTH);
    private final ModeValue colorMode =
            mode("hitboxesColorMode", "color_mode", COLOR_SYNC, COLOR_SYNC, COLOR_CUSTOM);
    private final BooleanValue eyesLine = bool("hitboxesEyesLine", "eyes_line", false);
    private final BooleanValue playersOnly = bool("hitboxesPlayersOnly", "players_only", true);
    private final NumberValue<Integer> lineWidth = num("hitboxesLineWidth", "line_width", 2, 1, 10);
    private final NumberValue<Integer> fillAlpha = num("hitboxesFillAlpha", "fill_alpha", 40, 0, 255);
    private final RGBColorValue hitboxColor =
            visibleWhen(colorNoAlpha("hitboxesColor", "#FFFFFF"), () -> COLOR_CUSTOM.equals(colorMode.get()));
    private final RGBColorValue friendHitboxColor =
            visibleWhen(colorNoAlpha("hitboxesFriendColor", "#00FF00"), () -> COLOR_CUSTOM.equals(colorMode.get()));

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || renderer == null || mc.level == null) return;

        List<Entity> targets = collectTargets(mc, tickDelta);
        if (targets.isEmpty()) return;

        boolean outline = !MODE_FILL.equals(renderMode.get());
        boolean fill = !MODE_OUTLINE.equals(renderMode.get());

        float previousLineWidth = RenderState.lineWidth;
        RenderState.lineWidth = Math.max(0.01f, lineWidth.get());
        MeshBuilder lines = null;
        MeshBuilder tris = null;
        try {
            if (outline) {
                lines = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_LINES, Renderer3D.DepthMode.NONE);
            }
            if (fill) {
                tris = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED, Renderer3D.DepthMode.NONE);
            }
            for (Entity entity : targets) {
                AABB box = interpolatedBox(entity, tickDelta);
                int rgb = resolveColor(entity) & 0x00FFFFFF;
                drawBox(lines, tris, box, rgb);
                if (eyesLine.get() && entity instanceof LivingEntity living) {
                    drawEyesLine(lines, living, box, rgb);
                }
            }
        } finally {
            RenderState.lineWidth = previousLineWidth;
        }
    }

    private List<Entity> collectTargets(Minecraft mc, float tickDelta) {
        List<Entity> out = new ArrayList<>();
        Entity camera = mc.getCameraEntity();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == camera) continue;
            if (playersOnly.get() && !(entity instanceof Player)) continue;
            if (!Renderer3D.Culling.isInFrustum(interpolatedBox(entity, tickDelta))) continue;
            out.add(entity);
        }
        return out;
    }

    static AABB interpolatedBox(Entity entity, float tickDelta) {
        Vec3 pos = entity.getPosition(tickDelta);
        return entity.getBoundingBox().move(pos.x - entity.getX(), pos.y - entity.getY(), pos.z - entity.getZ());
    }

    private int resolveColor(Entity entity) {
        if (COLOR_SYNC.equals(colorMode.get()) && entity instanceof Player player) {
            return CategoryService.getColor(player) | 0xFF000000;
        }
        if (entity instanceof Player player
                && CategoryService.get(player) == CategoryType.FRIEND) {
            return friendHitboxColor.getArgb() | 0xFF000000;
        }
        return hitboxColor.getArgb() | 0xFF000000;
    }

    private void drawBox(MeshBuilder lines, MeshBuilder tris, AABB box, int rgb) {
        if (tris != null) {
            int fill = withAlpha(rgb, fillAlpha.get());
            EspMeshBuilder.addFilledBox(tris, box, fill, fill, fill, fill, fill, fill, fill, fill);
        }
        if (lines != null) {
            int outline = 0xFF000000 | rgb;
            EspMeshBuilder.addOutlineBox(lines, box,
                    outline, outline, outline, outline, outline, outline, outline, outline);
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private void drawEyesLine(MeshBuilder lines, LivingEntity living, AABB box, int rgb) {
        if (lines == null) return;
        double eyeY = box.minY + living.getEyeHeight();
        double cx = (box.minX + box.maxX) / 2.0;
        double cz = (box.minZ + box.maxZ) / 2.0;
        double len = 0.3;
        float yawDeg = living.getYRot();
        double rad = Math.toRadians(yawDeg);
        double dx = -Math.sin(rad) * len;
        double dz = Math.cos(rad) * len;
        int argb = 0xFF000000 | rgb;
        EspMeshBuilder.line(lines, cx, eyeY, cz, argb, cx + dx, eyeY + 0.05, cz + dz, argb);
    }
}
