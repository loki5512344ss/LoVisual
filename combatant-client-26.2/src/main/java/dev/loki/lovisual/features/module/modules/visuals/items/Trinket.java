/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.items;

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
import dev.loki.lovisual.util.physics.TrinketPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Компаньон-шарик, подвешенный у ног игрока: падает под гравитацией,
 * отскакивает, тянется обратно пружиной (см. {@link TrinketPhysics} — чистая
 * симуляция, покрыта тестами), оставляет затухающий шлейф и звенит при
 * отскоке. Референс: Soup other/Trinket.java — модуль-настройки без
 * реализации (физика/рендер были в недоступной нам базе), физика и рендер
 * написаны с нуля под наш движок. Библиотека из 10 иконок-спутников
 * (Atom/Ball/Cat/...) и полный набор из 8 звуковых тем референса не
 * портированы — нужны новые ассеты; вместо этого светящийся шарик тонируется
 * через уже существующий {@link AnimatedRenderColors}, а звук — из ванильных
 * {@link SoundEvents} (без новых .ogg).
 */
@ModuleInfo(
        id = "trinket",
        displayName = "Trinket",
        category = ModuleCategory.VISUALS,
        description = "Компаньон-шарик у ног игрока: физика прыжков, шлейф, звук отскока"
)
public final class Trinket extends Module {

    private static final int TRAIL_CAPACITY = 24;
    private static final double TICK_DT = 0.05;

    private final NumberValue<Float> gravity = num("gravity", 20.0f, 5.0f, 40.0f);
    private final NumberValue<Float> weight = num("weight", 1.0f, 0.2f, 2.0f);
    private final NumberValue<Float> friction = num("friction", 0.98f, 0.8f, 1.0f);
    private final NumberValue<Float> bounce = num("bounce", 0.7f, 0.0f, 1.0f);
    private final NumberValue<Float> groundFriction = num("ground_friction", 0.9f, 0.7f, 1.0f);
    private final NumberValue<Float> tetherRadius = num("tether_radius", 1.2f, 0.3f, 3.0f);
    private final NumberValue<Float> iconSize = num("icon_size", 0.28f, 0.1f, 0.6f);

    private final ModeValue colorMode = modeSetting("colorMode", "color_mode", "Static", AnimatedColorModeOptions.OPTIONS);
    private final NumberValue<Integer> colorSpeed = num("color_speed", 18, 2, 54);
    private final RGBAColorValue colorValue = color("color", "#FF7FE0FF");
    private final RGBAColorValue colorValue2 = visibleWhen(color("color2", "#FFFF9F40"), this::usesSecondaryColor);

    private final BooleanValue trailEnabled = bool("trail_enabled", true);
    private final NumberValue<Integer> trailLength = visibleWhen(num("trail_length", 15, 5, TRAIL_CAPACITY), trailEnabled::get);

    private final BooleanValue soundEnabled = bool("sound_enabled", true);
    private final NumberValue<Float> soundVolume = visibleWhen(num("sound_volume", 0.5f, 0.0f, 1.5f), soundEnabled::get);

    private final TrinketPhysics physics = new TrinketPhysics();
    private final double[] trailX = new double[TRAIL_CAPACITY];
    private final double[] trailY = new double[TRAIL_CAPACITY];
    private final double[] trailZ = new double[TRAIL_CAPACITY];
    private int trailHead;
    private int trailCount;

    @Override
    public void onEnable() {
        physics.reset();
        trailHead = 0;
        trailCount = 0;
    }

    private boolean usesSecondaryColor() {
        return AnimatedRenderColors.usesSecondary(AnimatedColorModeOptions.parse(colorMode.get()));
    }

    private int resolveColorArgb() {
        return AnimatedRenderColors.resolve(
                AnimatedColorModeOptions.parse(colorMode.get()), colorSpeed.get(), 0,
                colorValue.getArgb(), colorValue2.getArgb());
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!isEnabled() || player == null || mc.level == null) return;

        boolean bounced = physics.tick(TICK_DT, gravity.get(), weight.get(), friction.get(),
                bounce.get(), groundFriction.get(), tetherRadius.get());

        if (trailEnabled.get()) {
            trailX[trailHead] = physics.x();
            trailY[trailHead] = physics.y();
            trailZ[trailHead] = physics.z();
            trailHead = (trailHead + 1) % trailX.length;
            if (trailCount < trailX.length) trailCount++;
        }

        if (bounced && soundEnabled.get() && soundVolume.get() > 0.001f) {
            Vec3 pos = player.position();
            mc.level.playSound(player, pos.x, pos.y, pos.z,
                    SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, soundVolume.get(), 1.4f);
        }
    }

    @Override
    public void onDisable() {
        physics.reset();
        trailCount = 0;
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null) return;

        Vec3 anchor = mc.player.getPosition(tickDelta);
        MeshBuilder mesh = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE, TextureStorage.FIRE_FLY, Renderer3D.DepthMode.MAIN);
        if (mesh == null) return;

        Quaternionf camRot = RenderState.cameraRotation;
        int color = resolveColorArgb();

        if (trailEnabled.get() && trailCount > 1) {
            int visible = Math.min(trailCount, Math.max(2, trailLength.get()));
            for (int i = 0; i < visible; i++) {
                int idx = Math.floorMod(trailHead - 1 - i, trailX.length);
                float fade = 1.0f - (i / (float) visible);
                if (fade <= 0.02f) continue;
                int argb = applyOpacity(color, fade * 0.6f);
                double x = anchor.x + trailX[idx];
                double y = anchor.y + trailY[idx];
                double z = anchor.z + trailZ[idx];
                addBillboard(mesh, x, y, z, iconSize.get() * (0.4f + 0.4f * fade), camRot, argb);
            }
        }

        double x = anchor.x + physics.x();
        double y = anchor.y + physics.y();
        double z = anchor.z + physics.z();
        addBillboard(mesh, x, y, z, iconSize.get(), camRot, applyOpacity(color, 1.0f));
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
}
