package dev.loki.lovisual.render.gamerenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.camera.AspectRatio;
import dev.loki.lovisual.features.module.modules.visuals.camera.Freecam;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.ReimaginedVisual;
import dev.loki.lovisual.features.module.modules.visuals.visibility.NoRender;
import dev.loki.lovisual.mixins.accessors.entity.LocalPlayerAccessor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Projection math extracted from GameRendererMixin: aspect ratio, view bobbing,
 * nausea and MSAA depth queries. Holds per-frame scratch matrices only.
 */
public final class GameRendererProjectionHelper {

    private final Matrix4f inverseBobScratch = new Matrix4f();
    private final org.joml.Quaternionf conjugateScratch = new org.joml.Quaternionf();

    public interface BobInvoker {
        void lovisual$bobHurt(CameraRenderState cameraRenderState, PoseStack matrices);

        void lovisual$bobView(CameraRenderState cameraRenderState, PoseStack matrices);
    }

    public void applyAspect(CameraRenderState state) {
        AspectRatio ar = Modules.get(AspectRatio.class);
        if (ar == null || !ar.isEnabled()) return;
        float mul = ar.getMultiplier();
        if (mul == 1.0f || state == null || state.projectionMatrix == null) return;
        // horizontal aspect stretch
        state.projectionMatrix.m00(state.projectionMatrix.m00() * mul);
    }

    public Matrix4f buildWorldProjection(Camera camera, float fov, PoseStack viewEffects, float tickDelta,
                                  float spinningTime, float spinningSpeed,
                                  Minecraft minecraft, BobInvoker bobInvoker) {
        CameraRenderState tmp = new CameraRenderState();
        camera.extractRenderState(tmp, fov);
        Matrix4f projection = new Matrix4f(tmp.projectionMatrix);
        applyViewEffects(viewEffects, tickDelta, camera, minecraft, bobInvoker);
        projection.mul(viewEffects.last().pose());
        applyNausea(projection, tickDelta, minecraft, spinningTime, spinningSpeed);
        return projection;
    }

    private void applyViewEffects(PoseStack matrices, float tickDelta, Camera camera,
                                  Minecraft minecraft, BobInvoker bobInvoker) {
        NoRender noRender = Modules.get(NoRender.class);
        CameraRenderState state = new CameraRenderState();
        camera.extractRenderState(state, camera.getFov());
        if (noRender == null || !noRender.off("camera_shake")) {
            bobInvoker.lovisual$bobHurt(state, matrices);
        }
        boolean freecam = Modules.get(Freecam.class) != null && Modules.get(Freecam.class).isEnabled();
        if (minecraft.options.bobView().get() && !freecam
                && (noRender == null || !noRender.off("view_bob"))) {
            bobInvoker.lovisual$bobView(state, matrices);
        }
    }

    private void applyNausea(Matrix4f projection, float tickDelta, Minecraft minecraft,
                             float spinningTime, float spinningSpeed) {
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        float scale = minecraft.options.screenEffectScale().get().floatValue();
        if (scale <= 0.0f) return;
        float intensity = 0.0f;
        if (player instanceof LocalPlayerAccessor acc) {
            intensity = Mth.lerp(tickDelta, acc.lovisual$getLastNauseaIntensity(), acc.lovisual$getNauseaIntensity());
        }
        float effect = player.getEffectBlendFactor(MobEffects.NAUSEA, tickDelta);
        float strength = Math.max(intensity, effect) * (scale * scale);
        if (strength <= 0.0f) return;
        float s = 5.0f / (strength * strength + 5.0f) - strength * 0.04f;
        s *= s;
        Vector3f axis = new Vector3f(0.0f, Mth.SQRT_OF_TWO / 2.0f, Mth.SQRT_OF_TWO / 2.0f);
        float angle = (spinningTime + tickDelta * spinningSpeed) * Mth.DEG_TO_RAD;
        projection.rotate(angle, axis);
        projection.scale(1.0f / s, 1.0f, 1.0f);
        projection.rotate(-angle, axis);
    }

    public boolean isWorldTranslucent(FogType type) {
        return type == FogType.WATER || type == FogType.LAVA || type == FogType.POWDER_SNOW;
    }

    public boolean needsResolvedMainDepth() {
        ReimaginedVisual module = Modules.get(ReimaginedVisual.class);
        if (module == null) return false;
        if (module.needsResolvedMainDepthCapture()) return true;
        return MainConfig.get().getMsaa3dSamples() > 1 && module.isActive();
    }

    public Matrix4f inverseBobScratch() {
        return inverseBobScratch;
    }

    public org.joml.Quaternionf conjugateScratch() {
        return conjugateScratch;
    }
}
