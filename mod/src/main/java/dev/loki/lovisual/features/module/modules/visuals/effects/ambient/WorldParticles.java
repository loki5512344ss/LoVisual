package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.animation.AnimatedColorModeOptions;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.color.ColorBlendMode;
import dev.loki.lovisual.render.engine.profiler.frame.RenderCostProfiler;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.features.module.modules.visuals.effects.ambient.geometry.WorldParticlesGeometry;
import dev.loki.lovisual.features.module.modules.visuals.effects.ambient.geometry.WorldParticlesSphere;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

//todo Description
@ModuleInfo(
        id = "worldparticles",
        displayName = "WorldParticles",
        category = ModuleCategory.VISUALS
)
public class WorldParticles extends Module {
    private static final float SPRITE_SIZE_MULTIPLIER = 4.0f;
    private static final float SPRITE_ALPHA_MULTIPLIER = 0.40f;
    private static final float DIAGONAL_ALPHA_MULTIPLIER = 0.40f;
    private static final float OUTLINE_ALPHA_MULTIPLIER = 205.0f / 255.0f;

    private final EnumValue<Mode> mode =
            enumSetting("mode", "mode", Mode.QUADS);
    private final BooleanValue depthTest =
            boolCommon("depth_test", CommonSettingSchemas.RENDER_DEPTH_TEST, true);
    private final RGBAColorValue color =
            common(color("color", "#FF8ED4FF"), CommonSettingSchemas.RENDER_PRIMARY_COLOR.commonI18nKey());
    private final ModeValue colorMode =
            modeSetting("worldParticlesColorMode", "color_mode", "Static",
                    AnimatedColorModeOptions.OPTIONS);
    private final NumberValue<Integer> colorSpeed =
            num("worldParticlesColorSpeed", "color_speed", 18, 2, 54);
    private final NumberValue<Integer> colorPhase =
            num("worldParticlesColorPhase", "color_phase", 0, 0, 360);
    private final NumberValue<Integer> colorSpread =
            num("worldParticlesColorSpread", "color_spread", 90, -720, 720);
    private final RGBAColorValue color2 =
            visibleWhen(color("worldParticlesColor2", "color2", "#FF55FFFF"), this::usesSecondaryColor);
    private final NumberValue<Integer> maxParticles =
            num("max_particles", 100, 1, 400);
    private final NumberValue<Integer> spawnPerTick =
            num("spawn_per_tick", 1, 1, 20);
    private final NumberValue<Double> radius =
            num("radius", 20.0, 1.0, 64.0);
    private final NumberValue<Double> height =
            num("height", 5.0, 0.5, 16.0);
    private final NumberValue<Integer> lifeMinMs =
            num("life_min_ms", 1500, 100, 10000);
    private final NumberValue<Integer> lifeMaxMs =
            num("life_max_ms", 4500, 100, 15000);
    private final NumberValue<Double> sizeMin =
            num("size_min", 0.10, 0.02, 1.0);
    private final NumberValue<Double> sizeMax =
            num("size_max", 0.30, 0.02, 1.5);
    private final NumberValue<Double> lineWidth =
            visibleWhen(num("line_width", 1.0, 0.5, 4.0), () -> !isFunnelMode() && !isBubbleMode());
    private final EnumValue<ColorBlendMode> funnelTintMode =
            visibleWhen(enumSetting("funnel_tint_mode", "funnel_tint_mode", ColorBlendMode.ORIGINAL), this::isFunnelMode);
    private final BooleanValue funnelTintSyncTheme =
            visibleWhen(bool("funnel_tint_sync_theme", "funnel_tint_sync_theme", true), this::isFunnelTintControlsVisible);
    private final RGBColorValue funnelTintColor =
            visibleWhen(colorNoAlpha("funnel_tint_color", "funnel_tint_color", "#78D7FF"), this::isManualFunnelTintVisible);
    private final NumberValue<Float> funnelTintStrength =
            visibleWhen(num("funnel_tint_strength", "funnel_tint_strength", 0.45f, 0.0f, 1.0f), this::isFunnelTintControlsVisible);
    private final List<WorldParticlesParticle> particles = new ArrayList<>();

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onDisable() {
        particles.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null) {
            return;
        }

        particles.removeIf(WorldParticlesParticle::isExpired);
        for (WorldParticlesParticle particle : particles) {
            particle.tick();
        }

        int targetCount = Math.max(1, maxParticles.get());
        int available = targetCount - particles.size();
        if (available <= 0) {
            return;
        }

        int spawnCount = Mth.clamp(spawnPerTick.get(), 0, available);
        for (int i = 0; i < spawnCount; i++) {
            particles.add(WorldParticlesParticle.spawn(radius.get(), height.get(),
                    lifeMinMs.get(), lifeMaxMs.get(), sizeMin.get(), sizeMax.get(), isBubbleMode()));
        }
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || particles.isEmpty() || mc.player == null || mc.level == null) {
            return;
        }

        try (RenderCostProfiler.Scope ignored = RenderCostProfiler.worldEffect("world_particles:" + mode.get())) {
            boolean useDepth = depthTest.get();
            Renderer3D.DepthMode depthMode = useDepth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN;
            if (isFunnelMode()) {
                WorldParticlesFunnel.render(renderer, useDepth, depthMode, tickDelta, particles,
                        new WorldParticlesFunnel.Tint(funnelTintRgb(), funnelTintMode.get(), funnelTintStrength.get()),
                        this::colorForParticle);
                return;
            }

            boolean bubbleMode = isBubbleMode();
            MeshBuilder spriteMesh = WorldParticlesBatches.sprite(renderer, useDepth, depthMode);
            MeshBuilder lineMesh = bubbleMode
                    ? null
                    : WorldParticlesBatches.lines(renderer, useDepth, depthMode, lineWidth.get().floatValue());
            MeshBuilder bubbleShellMesh = bubbleMode
                    ? WorldParticlesBatches.bubbleShell(renderer, useDepth, depthMode)
                    : null;

            int particleCount = particles.size();
            if (bubbleShellMesh != null) {
                bubbleShellMesh.ensureCapacity(
                        particleCount * (WorldParticlesSphere.STACKS + 1) * (WorldParticlesSphere.SLICES + 1),
                        particleCount * WorldParticlesSphere.STACKS * WorldParticlesSphere.SLICES * 6
                );
            }
            if (spriteMesh != null) {
                spriteMesh.ensureCapacity(
                        bubbleMode ? particleCount * 20 : particleCount * 4,
                        bubbleMode ? particleCount * 30 : particleCount * 6
                );
            }
            if (lineMesh != null) {
                lineMesh.ensureCapacity(particleCount * 32, particleCount * 32);
            }

            Quaternionf camRot = RenderState.cameraRotation;
            for (int i = 0; i < particles.size(); i++) {
                WorldParticlesParticle particle = particles.get(i);
                float alpha = particle.alpha();
                if (alpha <= 0.003f) {
                    continue;
                }

                Vec3 pos = particle.interpolatePos(tickDelta);
                int baseColor = colorForParticle(particle, i);

                if (bubbleMode) {
                    WorldParticlesBubble.renderParticle(bubbleShellMesh, spriteMesh, particle, pos, camRot, alpha, baseColor);
                    continue;
                }

                if (spriteMesh != null) {
                    int spriteArgb = WorldParticlesGeometry.multiplyAlpha(baseColor, alpha * SPRITE_ALPHA_MULTIPLIER);
                    WorldParticlesGeometry.addBillboardQuad(spriteMesh, pos.x, pos.y, pos.z,
                            particle.size() * SPRITE_SIZE_MULTIPLIER, camRot, spriteArgb);
                }

                if (lineMesh != null) {
                    Vec3 rotation = particle.interpolateRotation(tickDelta);
                    int diagonalArgb = WorldParticlesGeometry.multiplyAlpha(baseColor, alpha * DIAGONAL_ALPHA_MULTIPLIER);
                    int outlineArgb = WorldParticlesGeometry.multiplyAlpha(baseColor, alpha * OUTLINE_ALPHA_MULTIPLIER);
                    WorldParticlesGeometry.addCube(lineMesh, pos, rotation, particle.size(), diagonalArgb, outlineArgb);
                }
            }
        }
    }

    private int colorForParticle(WorldParticlesParticle particle, int index) {
        int phase = colorPhase.get()
                + Math.round(index * colorSpread.get() / 90.0f)
                + Math.round(particle.visualPhase() * 57.29578f);
        return AnimatedRenderColors.resolve(
                AnimatedColorModeOptions.parse(colorMode.get()),
                colorSpeed.get(),
                phase,
                color.getArgb(),
                color2.getArgb(),
                true
        );
    }

    private boolean usesSecondaryColor() {
        return AnimatedRenderColors.usesSecondary(AnimatedColorModeOptions.parse(colorMode.get()));
    }

    private int funnelTintRgb() {
        if (funnelTintSyncTheme.get()) {
            return Theme.theme().accent() & 0x00FFFFFF;
        }
        return funnelTintColor.getArgb() & 0x00FFFFFF;
    }

    private boolean isFunnelTintControlsVisible() {
        return isFunnelMode() && funnelTintMode.get() != ColorBlendMode.ORIGINAL;
    }

    private boolean isManualFunnelTintVisible() {
        return isFunnelTintControlsVisible() && !funnelTintSyncTheme.get();
    }

    private boolean isFunnelMode() {
        return mode.get() == Mode.FUNNEL;
    }

    private boolean isBubbleMode() {
        return mode.get() == Mode.AIR_BUBBLES;
    }

    private enum Mode implements EnumValue.AliasProvider {
        QUADS,
        AIR_BUBBLES,
        FUNNEL;

        @Override
        public List<String> aliases() {
            return switch (this) {
                case AIR_BUBBLES -> List.of("AirBubbles");
                case QUADS -> List.of("Quads");
                case FUNNEL -> List.of("Funnel");
            };
        }
    }
}
