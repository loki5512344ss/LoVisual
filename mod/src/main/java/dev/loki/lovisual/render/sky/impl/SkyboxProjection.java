package dev.loki.lovisual.render.sky;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.camera.Freecam;
import dev.loki.lovisual.features.module.modules.visuals.visibility.NoRender;
import dev.loki.lovisual.mixins.accessors.renderer.GameRendererAccessor;
import dev.loki.lovisual.mixins.accessors.entity.LocalPlayerAccessor;
import dev.loki.lovisual.render.engine.core.system.LoVisualWorldMatrices;

/**
 * World projection helpers for {@link CustomSkyboxRenderer}: builds the camera projection matrix
 * (reusing the captured frame projection when available, else re-deriving hurt-tilt / view-bob /
 * nausea effects) and extracts the vertical FOV from a projection matrix.
 */
final class SkyboxProjection {
    private SkyboxProjection() {
    }

    static Matrix4f build(Minecraft mc, Camera camera, float fov, float tickDelta) {
        Matrix4f capturedProjection = LoVisualWorldMatrices.renderProjectionMatrix();
        if (capturedProjection != null) {
            return capturedProjection;
        }

        GameRenderer renderer = mc.gameRenderer;
        GameRendererAccessor accessor = (GameRendererAccessor) renderer;
        CameraRenderState cameraRenderState = new CameraRenderState();
        camera.extractRenderState(cameraRenderState, fov);
        Matrix4f projection = new Matrix4f(cameraRenderState.projectionMatrix);

        PoseStack viewEffects = new PoseStack();
        NoRender noRender = Modules.get(NoRender.class);
        if (noRender == null || !noRender.off("camera_shake")) {
            accessor.invokeTiltViewWhenHurt(cameraRenderState, viewEffects);
        }

        boolean freecamEnabled = Modules.get(Freecam.class) != null && Modules.get(Freecam.class).isEnabled();
        if (mc.options.bobView().get()
                && !freecamEnabled
                && (noRender == null || !noRender.off("view_bob"))) {
            accessor.invokeBobView(cameraRenderState, viewEffects);
        }

        projection.mul(viewEffects.last().pose());
        applyNauseaProjection(mc, accessor, projection, tickDelta);
        return projection;
    }

    static float extractFov(Matrix4f projection, float fallbackFov) {
        if (projection == null) {
            return fallbackFov;
        }
        float m11 = Math.abs(projection.m11());
        if (!Float.isFinite(m11) || m11 <= 1.0e-4f) {
            return fallbackFov;
        }
        float fov = (float) Math.toDegrees(2.0 * Math.atan(1.0 / m11));
        return Float.isFinite(fov) ? Mth.clamp(fov, 1.0f, 179.0f) : fallbackFov;
    }

    private static void applyNauseaProjection(Minecraft mc,
                                              GameRendererAccessor accessor,
                                              Matrix4f projection,
                                              float tickDelta) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        float distortionScale = mc.options.screenEffectScale().get().floatValue();
        if (distortionScale <= 0.0f) return;

        float nauseaIntensity = 0.0f;
        if (player instanceof LocalPlayerAccessor playerAccessor) {
            nauseaIntensity = Mth.lerp(
                    tickDelta,
                    playerAccessor.lovisual$getLastNauseaIntensity(),
                    playerAccessor.lovisual$getNauseaIntensity()
            );
        }

        float nauseaEffect = player.getEffectBlendFactor(MobEffects.NAUSEA, tickDelta);
        float strength = Math.max(nauseaIntensity, nauseaEffect) * (distortionScale * distortionScale);
        if (strength <= 0.0f) return;

        float scale = 5.0f / (strength * strength + 5.0f) - strength * 0.04f;
        scale *= scale;
        Vector3f axis = new Vector3f(0.0f, Mth.SQRT_OF_TWO / 2.0f, Mth.SQRT_OF_TWO / 2.0f);
        float angle = (accessor.lovisual$getNauseaEffectTime() + tickDelta * accessor.lovisual$getNauseaEffectSpeed())
                * Mth.DEG_TO_RAD;
        projection.rotate(angle, axis);
        projection.scale(1.0f / scale, 1.0f, 1.0f);
        projection.rotate(-angle, axis);
    }
}
