/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.scene;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.animation.AnimatedColorModeOptions;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Кастомизация рендера энд-кристаллов: цвет стекла/куба (см.
 * {@code EndCrystalRendererMixin}) и осколки-вспышка на месте разрушения
 * (кристалл исчезает из мира мгновенно — ваниль не даёт доиграть анимацию).
 * Референс: Soup other/EndCrystal.java — идея взята (тонировка модели,
 * персистентный эффект поломки), код не копировался. Гранулярное разбиение
 * на "Outer Glass"/"Inner Glass" референса условно: ванильная модель зовёт
 * один и тот же {@code ModelPart glass} дважды подряд (внешний и вложенный
 * повёрнутый куб) — мы различаем эти два вызова по порядку в миксине.
 */
@ModuleInfo(
        id = "endcrystal",
        displayName = "EndCrystal",
        category = ModuleCategory.VISUALS,
        description = "Цвет модели энд-кристалла и эффект осколков при разрушении"
)
public final class EndCrystal extends Module {

    private static final int SHARD_COUNT = 14;
    private static final float SHARD_SPEED = 0.16f;

    private final ModeValue colorMode = modeSetting("colorMode", "color_mode", "Sync", "Sync", "Custom");
    private final NumberValue<Integer> colorSpeed = num("color_speed", 18, 2, 54);
    private final RGBAColorValue colorValue = visibleWhen(color("color", "#FF7FE0D8"), this::isCustomColor);
    private final RGBAColorValue colorValue2 = visibleWhen(color("color2", "#FFB57FE0"), () -> isCustomColor() && usesSecondaryColor());
    private final ModeValue colorAnimation = visibleWhen(
            modeSetting("colorAnimation", "color_animation", "Static", AnimatedColorModeOptions.OPTIONS), this::isCustomColor);

    public final BooleanValue outerGlassVisible = bool("outer_glass", true);
    public final BooleanValue innerGlassVisible = bool("inner_glass", true);
    public final BooleanValue cubeVisible = bool("cube", true);

    private final BooleanValue shatterEnabled = bool("shatter_enabled", true);
    private final NumberValue<Integer> shatterDurationMs = visibleWhen(num("shatter_duration_ms", 600, 200, 2000), shatterEnabled::get);

    private final List<Shard> shards = new ArrayList<>();
    private final Random random = new Random();

    private boolean isCustomColor() {
        return "Custom".equals(colorMode.get());
    }

    private boolean usesSecondaryColor() {
        return AnimatedRenderColors.usesSecondary(AnimatedColorModeOptions.parse(colorAnimation.get()));
    }

    /** @return тонировка модели (ARGB), либо -1 (без тонировки — ванильный белый), если Sync. */
    public int resolveTintArgb() {
        if (!isCustomColor()) return -1;
        return AnimatedRenderColors.resolve(
                AnimatedColorModeOptions.parse(colorAnimation.get()), colorSpeed.get(), 0,
                colorValue.getArgb(), colorValue2.getArgb());
    }

    /** Вызывается миксином при разрушении кристалла (RemovalReason#shouldDestroy). */
    public void onCrystalDestroyed(Vec3 pos) {
        if (!isEnabled() || !shatterEnabled.get()) return;

        int color = resolveTintArgb();
        if (color == -1) color = 0xFF9CF0E8;

        long now = GameClock.millis();
        for (int i = 0; i < SHARD_COUNT; i++) {
            double yaw = random.nextDouble() * Math.PI * 2.0;
            double pitch = random.nextDouble() * Math.PI - Math.PI / 2.0;
            double speed = SHARD_SPEED * (0.5 + random.nextDouble());
            Vec3 velocity = new Vec3(
                    Math.cos(yaw) * Math.cos(pitch),
                    Math.sin(pitch),
                    Math.sin(yaw) * Math.cos(pitch)
            ).scale(speed);
            shards.add(new Shard(pos, velocity, now, color));
        }
    }

    @Override
    public void onDisable() {
        shards.clear();
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        if (!isEnabled() || shards.isEmpty()) return;

        long durationMs = Math.max(1, shatterDurationMs.get());
        long now = GameClock.millis();
        shards.removeIf(s -> now - s.startMs > durationMs);
        if (shards.isEmpty()) return;

        MeshBuilder mesh = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE, TextureStorage.PARTICLE_STARS, Renderer3D.DepthMode.MAIN);
        if (mesh == null) return;

        Quaternionf camRot = RenderState.cameraRotation;
        for (Shard s : shards) {
            float progress = Mth.clamp((now - s.startMs) / (float) durationMs, 0f, 1f);
            float fade = 1.0f - progress;
            if (fade <= 0.02f) continue;

            double dt = (now - s.startMs) / 1000.0;
            double x = s.origin.x + s.velocity.x * dt;
            double y = s.origin.y + s.velocity.y * dt - 0.35 * dt * dt;
            double z = s.origin.z + s.velocity.z * dt;

            float size = 0.22f * fade;
            int argb = applyOpacity(s.colorArgb, fade);
            addBillboard(mesh, x, y, z, size, camRot, argb);
        }
    }

    private static void addBillboard(MeshBuilder mesh, double cx, double cy, double cz,
                                     float size, Quaternionf camRot, int argb) {
        Vector3f right = new Vector3f(1, 0, 0).rotate(camRot).mul(size);
        Vector3f up = new Vector3f(0, 1, 0).rotate(camRot).mul(size);

        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(cx - right.x() - up.x(), cy - right.y() - up.y(), cz - right.z() - up.z()).vec2(0, 1).color(new RenderColor(argb)).next();
        int i2 = mesh.vec3(cx + right.x() - up.x(), cy + right.y() - up.y(), cz + right.z() - up.z()).vec2(1, 1).color(new RenderColor(argb)).next();
        int i3 = mesh.vec3(cx + right.x() + up.x(), cy + right.y() + up.y(), cz + right.z() + up.z()).vec2(1, 0).color(new RenderColor(argb)).next();
        int i4 = mesh.vec3(cx - right.x() + up.x(), cy - right.y() + up.y(), cz - right.z() + up.z()).vec2(0, 0).color(new RenderColor(argb)).next();
        mesh.quad(i1, i2, i3, i4);
    }

    private static int applyOpacity(int argb, float opacity) {
        int a = (argb >>> 24) & 0xFF;
        int na = Mth.clamp((int) (a * Mth.clamp(opacity, 0f, 1f)), 0, 255);
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    private record Shard(Vec3 origin, Vec3 velocity, long startMs, int colorArgb) {
    }
}
