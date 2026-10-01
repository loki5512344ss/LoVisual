package dev.loki.lovisual.features.module.modules.visuals.player;

import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.CircleAnimation;
import dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.CircleColorMode;
import dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.CircleRenderer;
import dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.JumpCircle;
import dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.shape.Shape;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

/** Draws an animated shape on the ground where a player left it by jumping. */
@ModuleInfo(
        id = "jumpcircles",
        displayName = "JumpCircles",
        category = ModuleCategory.VISUALS
)
public class JumpCircles extends Module {
    private static final double SURFACE_LIFT = 0.02;

    private final EnumValue<CircleMode> mode =
            enumSetting("jumpCirclesMode", "mode", CircleMode.CIRCLE);
    private final NumberValue<Float> radius = common(
            num("jumpCirclesCircleScale", "circle_scale", 1.5f, 0.5f, 5f),
            CommonSettingSchemas.RENDER_SCALE.commonI18nKey()
    );
    private final NumberValue<Float> speed = num("jumpCirclesSpeed", "speed", 1f, 0.5f, 3f);
    private final NumberValue<Float> fadeSpeed = num("jumpCirclesFadeSpeed", "fade_speed", 1f, 0.5f, 3f);
    private final NumberValue<Float> rotateSpeed = num("jumpCirclesRotateSpeed", "rotate_speed", 1f, 0f, 5f);
    private final NumberValue<Float> thickness = visibleWhen(
            num("jumpCirclesThickness", "thickness", 0.25f, 0.08f, 0.5f), this::usesThickness);
    private final NumberValue<Integer> segments = visibleWhen(
            num("jumpCirclesSegments", "segments", 32, 12, 96), this::usesSegments);
    private final NumberValue<Integer> maxCircles = num("jumpCirclesMax", "max_circles", 12, 1, 32);
    private final BooleanValue onlySelf = bool("jumpCirclesOnlySelf", "only_self", false);
    private final BooleanValue depthTest = common(
            bool("jumpCirclesDepthTest", "depth_test", true),
            CommonSettingSchemas.RENDER_DEPTH_TEST.commonI18nKey()
    );
    private final EnumValue<CircleColorMode> colorMode =
            enumCommon("jumpCirclesCircleColorMode", "color_mode",
                    CommonSettingSchemas.RENDER_COLOR_MODE.commonI18nKey(), CircleColorMode.STATIC);
    private final NumberValue<Integer> colorSpeed = common(
            num("jumpCirclesColorSpeed", "color_speed", 18, 2, 54),
            CommonSettingSchemas.RENDER_COLOR_SPEED.commonI18nKey()
    );
    private final RGBAColorValue colorValue = common(
            color("jumpCirclesColor", "color", "#FF55FFFF"),
            CommonSettingSchemas.RENDER_PRIMARY_COLOR.commonI18nKey()
    );
    private final RGBAColorValue colorValue2 = visibleWhen(common(
            color("jumpCirclesColor2", "color2", "#FFFF5555"),
            CommonSettingSchemas.RENDER_SECONDARY_COLOR.commonI18nKey()
    ), () -> AnimatedRenderColors.usesSecondary(colorMode.get().animated()));

    private final Deque<JumpCircle> circles = new ArrayDeque<>();
    private final Map<UUID, Double> lastGroundY = new HashMap<>();
    private final Set<UUID> seen = new HashSet<>();
    private final CircleRenderer circleRenderer = new CircleRenderer();

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        seen.clear();
        for (Player pl : mc.level.players()) {
            if (onlySelf.get() && pl != mc.player) continue;
            UUID id = pl.getUUID();
            seen.add(id);
            if (pl.onGround()) {
                lastGroundY.put(id, pl.getY());
            } else {
                Double groundY = lastGroundY.remove(id);
                if (groundY != null) spawn(pl.getX(), groundY, pl.getZ());
            }
        }
        lastGroundY.keySet().retainAll(seen);
        long now = GameClock.millis();
        float lifetime = CircleAnimation.lifetimeMs(speed.get());
        circles.removeIf(c -> now - c.spawnMs() >= lifetime);
    }

    @Override
    public void onDisable() {
        circles.clear();
        lastGroundY.clear();
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null || circles.isEmpty()) return;

        CircleMode circleMode = mode.get();
        Shape shape = circleMode.shape;
        MeshBuilder mesh = circleRenderer.openMesh(renderer, shape, circleMode.texture, depthTest.get());
        if (mesh == null) return;
        float nowMs = GameClock.millis() + tickDelta * GameClock.TICK_MS;
        float lifetime = CircleAnimation.lifetimeMs(speed.get());
        IntUnaryOperator palette = this::colorAt;
        for (JumpCircle c : circles) {
            float age = Math.max(0f, nowMs - c.spawnMs());
            float progress = CircleAnimation.progress(age, lifetime);
            float alpha = CircleAnimation.alpha(progress, fadeSpeed.get());
            if (progress >= 1f || alpha <= 0f) continue;

            CircleRenderer.Pose pose = new CircleRenderer.Pose(c.x(), c.y(), c.z(),
                    CircleAnimation.radius(progress, radius.get()),
                    CircleAnimation.angle(age / 1000f, rotateSpeed.get(), progress),
                    alpha, CircleAnimation.wavePhase(age / 1000f));
            if (shape == null) {
                circleRenderer.texturedQuad(mesh, pose, palette);
            } else {
                circleRenderer.shape(mesh, pose, shape, segments.get(), thickness.get(), palette);
            }
        }
    }

    private void spawn(double x, double groundY, double z) {
        circles.addLast(new JumpCircle(x, groundY + SURFACE_LIFT, z, GameClock.millis()));
        while (circles.size() > maxCircles.get()) {
            circles.removeFirst();
        }
    }

    private boolean usesThickness() {
        return mode.get().shape != null && mode.get().shape.usesThickness();
    }

    private boolean usesSegments() {
        return mode.get().shape != null && mode.get().shape.usesSegments();
    }

    private int colorAt(int phase) {
        return AnimatedRenderColors.resolve(
                colorMode.get().animated(), colorSpeed.get(), phase, colorValue.getArgb(), colorValue2.getArgb());
    }

    private enum CircleMode implements EnumValue.AliasProvider {
        CIRCLE(TextureStorage.DEFAULT_CIRCLE, null),
        RING(TextureStorage.JUMP_CIRCLE, null),
        BUBBLE(TextureStorage.BUBBLE, null),
        STAR(null, Shape.STAR),
        DIAMOND(null, Shape.DIAMOND),
        SPIRAL(null, Shape.SPIRAL),
        HEXAGON(null, Shape.HEXAGON),
        WAVE(null, Shape.WAVE),
        CROSS(null, Shape.CROSS),
        PETALS(null, Shape.PETALS);

        private final Identifier texture;
        private final Shape shape;

        CircleMode(Identifier texture, Shape shape) {
            this.texture = texture;
            this.shape = shape;
        }

        @Override
        public List<String> aliases() {
            return switch (this) {
                case CIRCLE -> List.of("Default");
                case RING -> List.of("Pulse");
                default -> List.of();
            };
        }
    }
}
