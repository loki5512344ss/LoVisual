package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.mixininterface.entity.ILocalPlayer;
import dev.loki.lovisual.render.engine.animation.AnimatedColorModeOptions;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.helpers.particle.OrientedQuadRenderer;
import dev.loki.lovisual.util.animation.ThreeStageProgress;
import dev.loki.lovisual.util.combat.hit.AttackHitPoint;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Пузыри-разлёт в точке попадания по цели — отдельный от {@link HitEffect}
 * модуль (референс: Soup HitBubbles, идея переиспользована, код не копировался).
 * В отличие от режима {@code EffectMode.BUBBLES} в HitEffect (простой fade-out
 * за lifeTime), тут трёхфазная анимация появление/жизнь/исчезновение
 * ({@link ThreeStageProgress}) и независимое от HitEffect включение —
 * оба модуля можно использовать одновременно или по отдельности.
 */
@ModuleInfo(
        id = "hitbubbles",
        displayName = "HitBubbles",
        category = ModuleCategory.VISUALS,
        description = "Пузыри в точке попадания с трёхфазной анимацией появления/жизни/исчезновения"
)
public final class HitBubbles extends Module {

    private static final float CHARGED_THRESHOLD = 0.9f;

    private final ModeValue colorMode = modeSetting("hitBubblesColorMode", "color_mode", "Static", AnimatedColorModeOptions.OPTIONS);
    private final NumberValue<Integer> colorSpeed = num("hitBubblesColorSpeed", "color_speed", 18, 2, 54);
    private final RGBAColorValue colorValue = color("hitBubblesColor", "color", "#FF55FFFF");
    private final RGBAColorValue colorValue2 = visibleWhen(color("hitBubblesColor2", "color2", "#FFFF5555"), this::usesSecondaryColor);
    private final NumberValue<Float> bubbleScale = num("hitBubblesScale2", "scale", 1.0f, 0.4f, 2.0f);
    private final NumberValue<Float> rotateSpeed = num("hitBubblesRotateSpeed", "rotate_speed", 2.0f, 0.5f, 5.0f);
    private final NumberValue<Integer> appearMs = num("hitBubblesAppearMs", "appear_duration_ms", 150, 20, 1500);
    private final NumberValue<Integer> existMs = num("hitBubblesExistMs", "exist_duration_ms", 300, 20, 2000);
    private final NumberValue<Integer> disappearMs = num("hitBubblesDisappearMs", "disappear_duration_ms", 300, 20, 2000);
    private final BooleanValue onlyPlayers = bool("hitBubblesOnlyPlayers2", "only_players", true);
    private final BooleanValue depthTest = bool("hitBubblesDepthTest2", "depth_test", true);

    private final List<Bubble> bubbles = new ArrayList<>();

    @Override
    public void onDisable() {
        bubbles.clear();
    }

    public void handleHit(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        if (!(target instanceof LivingEntity)) return;
        if (onlyPlayers.get() && !(target instanceof Player)) return;

        ILocalPlayer access = (ILocalPlayer) mc.player;
        float yaw = access.lovisual$getLastYaw();
        float pitch = access.lovisual$getLastPitch();

        Vec3 point = AttackHitPoint.resolve(target, yaw, pitch);
        if (point == null) point = target.position();

        bubbles.add(new Bubble(point, -yaw, pitch, GameClock.millis()));
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null || bubbles.isEmpty()) return;

        long totalMs = totalDurationMs();
        bubbles.removeIf(b -> GameClock.millis() - b.startMs > totalMs);
        if (bubbles.isEmpty()) return;

        boolean useDepth = depthTest.get();
        RenderPipeline pipeline = useDepth
                ? LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE_LIQUID_IGNORE
                : LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE;
        Renderer3D.DepthMode depthMode = useDepth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN;

        MeshBuilder mesh = renderer.batchTextured(pipeline, TextureStorage.BUBBLE, depthMode);
        if (mesh == null) return;

        long now = GameClock.millis();
        for (Bubble b : bubbles) {
            long elapsed = now - b.startMs;
            float visibility = ThreeStageProgress.visibility(elapsed, appearMs.get(), existMs.get(), disappearMs.get());
            if (visibility <= 0.01f) continue;

            b.rotationDeg -= b.angularVelocity * rotateSpeed.get();
            float scale = visibility * 2f * bubbleScale.get();

            int c1 = applyOpacity(getColorArgb(270), visibility);
            int c2 = applyOpacity(getColorArgb(0), visibility);
            int c3 = applyOpacity(getColorArgb(180), visibility);
            int c4 = applyOpacity(getColorArgb(90), visibility);

            OrientedQuadRenderer.addOrientedQuad(mesh, b.pos, b.yaw, b.pitch, b.rotationDeg, scale, c1, c2, c3, c4);
        }
    }

    private long totalDurationMs() {
        return (long) appearMs.get() + existMs.get() + disappearMs.get();
    }

    private boolean usesSecondaryColor() {
        return AnimatedRenderColors.usesSecondary(AnimatedColorModeOptions.parse(colorMode.get()));
    }

    private int getColorArgb(int phase) {
        return AnimatedRenderColors.resolve(
                AnimatedColorModeOptions.parse(colorMode.get()),
                colorSpeed.get(),
                phase,
                colorValue.getArgb(),
                colorValue2.getArgb()
        );
    }

    private static int applyOpacity(int argb, float opacity) {
        float clamped = Math.min(1f, Math.max(0f, opacity));
        int a = (argb >>> 24) & 0xFF;
        int na = Mth.clamp((int) (a * clamped), 0, 255);
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    private static final class Bubble {
        private final Vec3 pos;
        private final float yaw;
        private final float pitch;
        private final long startMs;
        private final float angularVelocity;
        private float rotationDeg;

        private Bubble(Vec3 pos, float yaw, float pitch, long startMs) {
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
            this.startMs = startMs;
            this.angularVelocity = 0.5f + (float) Math.random() * 1.5f;
        }
    }
}
