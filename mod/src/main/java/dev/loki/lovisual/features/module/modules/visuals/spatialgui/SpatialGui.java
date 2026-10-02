/*
 * Ported/adapted from Spatial GUI (https://github.com/tastytrash/Spatial-GUI), MIT License.
 * Copyright (c) tastytrash and Spatial GUI contributors.
 *
 * This file is part of LoVisual and is distributed under LoVisual's own GPLv3 license terms
 * for the modifications made here (see mod/LICENSE and mod/THIRD_PARTY_NOTICES.md); the MIT-licensed
 * portions ported from Spatial GUI remain available under the MIT terms above.
 */
package dev.loki.lovisual.features.module.modules.visuals.spatialgui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.spatialgui.SpatialGuiCapture;
import dev.loki.lovisual.render.spatialgui.SpatialGuiPlane;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Renders the currently open {@link Screen} as a plane floating in the 3D world instead of a flat HUD overlay,
 * porting the core idea of Spatial GUI (https://github.com/tastytrash/Spatial-GUI, MIT) to LoVisual's own
 * module/config system and its deferred GUI-render pipeline.
 * <p>
 * The actual off-screen capture happens in {@code GuiRendererCaptureMixin} (which redirects the vanilla
 * {@code GuiRenderer.draw()} pass into {@link SpatialGuiCapture} instead of the main render target while this
 * module is active), and mouse routing happens in {@code MouseHandlerSpatialGuiMixin} (which reprojects the
 * real cursor position onto the plane published every frame via {@link SpatialGuiPlane}).
 */
@ModuleInfo(
        id = "spatialgui",
        displayName = "SpatialGui",
        category = ModuleCategory.VISUALS,
        description = "Renders the open screen as a 3D plane in the world instead of a flat overlay."
)
public class SpatialGui extends Module {

    private static SpatialGui instance;

    private final BooleanValue containerOnly = bool("spatialGuiContainerOnly", "container_only", true);
    private final BooleanValue thirdPerson = bool("spatialGuiThirdPerson", "third_person_screen", false);

    private final NumberValue<Float> distance = num("spatialGuiDistance", "distance", 1.5f, 0.3f, 6.0f);
    private final NumberValue<Float> sideOffset = num("spatialGuiSideOffset", "side_offset", 0.0f, -3.0f, 3.0f);
    private final NumberValue<Float> heightOffset = num("spatialGuiHeightOffset", "height_offset", 0.0f, -3.0f, 3.0f);
    private final NumberValue<Float> yawOffset = num("spatialGuiYawOffset", "yaw_offset", 180.0f, 0.0f, 360.0f);
    private final NumberValue<Float> pitchOffset = num("spatialGuiPitchOffset", "pitch_offset", 0.0f, -90.0f, 90.0f);
    private final NumberValue<Float> scale = num("spatialGuiScale", "scale", 1.8f, 0.3f, 6.0f);
    private final NumberValue<Integer> alpha = num("spatialGuiAlpha", "alpha", 255, 0, 255);

    private final BooleanValue parallax = bool("spatialGuiParallax", "parallax", true);
    private final NumberValue<Float> parallaxStrength = visibleWhen(
            num("spatialGuiParallaxStrength", "parallax_strength", 0.05f, 0.0f, 0.3f), parallax::get);

    private final BooleanValue animate = bool("spatialGuiAnimate", "animate", true);
    private final ModeValue animationStyle = visibleWhen(
            mode("spatialGuiAnimationStyle", "Scale", "Scale", "Slide", "Fade"), animate::get);
    private final NumberValue<Integer> animationDurationMs = visibleWhen(
            num("spatialGuiAnimationDuration", "animation_duration_ms", 220, 40, 2000), animate::get);

    private Screen trackedScreen;
    private long openTimeMs;
    private long closeTimeMs = -1;
    private GpuSampler cachedSampler;
    private float openYaw;
    private float openPitch;
    private CameraType previousCameraType;

    public SpatialGui() {
        instance = this;
    }

    public static SpatialGui getInstance() {
        return instance;
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    /** Whether the currently open screen (if any) should be captured and floated in 3D this frame. */
    public boolean isCapturing() {
        if (!isEnabled()) return false;
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen == null) return false;
        return !containerOnly.get() || screen instanceof AbstractContainerScreen<?>;
    }

    public boolean isThirdPerson() {
        return thirdPerson.get();
    }

    @Override
    public void onFrame(float tickDelta) {
        Screen current = isEnabled() ? Minecraft.getInstance().gui.screen() : null;
        boolean eligible = current != null && (!containerOnly.get() || current instanceof AbstractContainerScreen<?>);
        Screen effective = eligible ? current : null;

        if (effective != trackedScreen) {
            Options options = Minecraft.getInstance().options;
            if (effective != null) {
                openTimeMs = System.currentTimeMillis();
                closeTimeMs = -1;
                Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
                openYaw = camera.yRot();
                openPitch = camera.xRot();
                if (thirdPerson.get() && options.getCameraType().isFirstPerson()) {
                    previousCameraType = options.getCameraType();
                    options.setCameraType(CameraType.THIRD_PERSON_BACK);
                } else {
                    previousCameraType = null;
                }
            } else {
                if (trackedScreen != null) {
                    closeTimeMs = System.currentTimeMillis();
                }
                if (previousCameraType != null) {
                    options.setCameraType(previousCameraType);
                    previousCameraType = null;
                }
            }
            trackedScreen = effective;
        }
    }

    @Override
    public void onDisable() {
        if (previousCameraType != null) {
            Minecraft.getInstance().options.setCameraType(previousCameraType);
            previousCameraType = null;
        }
        trackedScreen = null;
        closeTimeMs = -1;
        SpatialGuiPlane.clear();
    }

    private float animationProgress() {
        if (!animate.get()) return 1.0F;
        long duration = Math.max(1, animationDurationMs.get());
        if (trackedScreen != null) {
            long elapsed = System.currentTimeMillis() - openTimeMs;
            return Math.min(1.0F, (float) elapsed / duration);
        }
        if (closeTimeMs >= 0) {
            long elapsed = System.currentTimeMillis() - closeTimeMs;
            return Math.max(0.0F, 1.0F - (float) elapsed / duration);
        }
        return 0.0F;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean liveScreen = trackedScreen != null;
        float progress = animationProgress();
        if (progress <= 0.0F && !liveScreen) {
            SpatialGuiPlane.clear();
            return;
        }

        SpatialGuiCapture capture = SpatialGuiCapture.get();
        if (!capture.isReady()) {
            SpatialGuiPlane.clear();
            return;
        }
        GpuTextureView textureView = capture.colorTextureView();
        if (textureView == null) {
            SpatialGuiPlane.clear();
            return;
        }

        Camera camera = mc.gameRenderer.mainCamera();
        Vector3fc forward = camera.forwardVector();
        Vector3f camPos = new Vector3f((float) camera.position().x, (float) camera.position().y, (float) camera.position().z);

        // Build the plane basis from the player's flat (yaw-only) facing direction so it stays upright,
        // then apply the configurable yaw/pitch offsets on top (yaw ~ 180 faces the plane back at the player,
        // like Spatial-GUI's default "screen held up in front of you" orientation).
        Vector3f worldUp = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f flatForward = new Vector3f(forward.x(), 0.0F, forward.z());
        if (flatForward.lengthSquared() < 1.0e-6F) flatForward = new Vector3f(0.0F, 0.0F, 1.0F);
        flatForward.normalize();

        Vector3f planeForward = new Vector3f(flatForward)
                .rotateAxis((float) Math.toRadians(yawOffset.get()), worldUp.x, worldUp.y, worldUp.z);
        Vector3f planeRight = new Vector3f(planeForward).cross(worldUp, new Vector3f()).normalize().mul(-1.0F);
        Vector3f planeUp = new Vector3f(planeRight).cross(planeForward, new Vector3f()).normalize();

        float pitchRadians = (float) Math.toRadians(pitchOffset.get());
        if (pitchRadians != 0.0F) {
            planeForward = planeForward.rotateAxis(pitchRadians, planeRight.x, planeRight.y, planeRight.z);
            planeUp = new Vector3f(planeRight).cross(planeForward, new Vector3f()).normalize();
        }

        float dist = distance.get();
        float parallaxRight = 0.0F;
        float parallaxUp = 0.0F;
        if (parallax.get()) {
            // Subtle depth-parallax: as the player turns their head away from the angle the screen opened at,
            // nudge the plane opposite to that turn so it reads as a physical object floating in the world
            // rather than a sprite glued to the camera (mirrors Spatial-GUI's head-tracked parallax).
            float yawDelta = wrapDegrees(camera.yRot() - openYaw);
            float pitchDelta = camera.xRot() - openPitch;
            float strength = parallaxStrength.get();
            parallaxRight = -yawDelta * strength * 0.05F;
            parallaxUp = pitchDelta * strength * 0.05F;
        }

        Vector3f center = new Vector3f(camPos)
                .add(new Vector3f(planeForward).mul(dist))
                .add(new Vector3f(planeRight).mul(sideOffset.get() + parallaxRight))
                .add(new Vector3f(planeUp).mul(heightOffset.get() + parallaxUp));

        int guiWidth = Math.max(1, capture.width());
        int guiHeight = Math.max(1, capture.height());
        float aspect = (float) guiWidth / (float) guiHeight;

        float animScale = scale.get();
        float baseAlpha = alpha.get() / 255.0F;
        String style = animationStyle.get();
        if ("Scale".equals(style)) {
            animScale *= 0.6F + 0.4F * ease(progress);
        } else if ("Slide".equals(style)) {
            center = new Vector3f(center).add(new Vector3f(planeUp).mul((1.0F - ease(progress)) * -0.6F));
        }
        float drawAlpha = "Fade".equals(style) ? baseAlpha * ease(progress) : baseAlpha;
        if (drawAlpha <= 0.001F) {
            SpatialGuiPlane.clear();
            return;
        }

        float halfHeight = animScale * 0.5F;
        float halfWidth = halfHeight * aspect;

        Vector3f p1 = new Vector3f(center).sub(new Vector3f(planeRight).mul(halfWidth)).sub(new Vector3f(planeUp).mul(halfHeight));
        Vector3f p2 = new Vector3f(center).add(new Vector3f(planeRight).mul(halfWidth)).sub(new Vector3f(planeUp).mul(halfHeight));
        Vector3f p3 = new Vector3f(center).add(new Vector3f(planeRight).mul(halfWidth)).add(new Vector3f(planeUp).mul(halfHeight));
        Vector3f p4 = new Vector3f(center).sub(new Vector3f(planeRight).mul(halfWidth)).add(new Vector3f(planeUp).mul(halfHeight));

        if (cachedSampler == null) {
            cachedSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }

        MeshBuilder mesh = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_PREMULTIPLIED, textureView, cachedSampler, Renderer3D.DepthMode.NONE);
        if (mesh != null) {
            // The capture holds premultiplied colour (GUI draws blended onto a transparent target), so the fade
            // factor must scale RGB together with alpha.
            int a = (int) (drawAlpha * 255.0F);
            mesh.ensureQuadCapacity();
            int i1 = mesh.vec3(p1.x, p1.y, p1.z).vec2(0, 1).color(a, a, a, a).next();
            int i2 = mesh.vec3(p2.x, p2.y, p2.z).vec2(1, 1).color(a, a, a, a).next();
            int i3 = mesh.vec3(p3.x, p3.y, p3.z).vec2(1, 0).color(a, a, a, a).next();
            int i4 = mesh.vec3(p4.x, p4.y, p4.z).vec2(0, 0).color(a, a, a, a).next();
            mesh.quad(i1, i2, i3, i4);
        }

        if (liveScreen) {
            SpatialGuiPlane.publish(new SpatialGuiPlane(center, planeRight, planeUp, halfWidth, halfHeight, guiWidth, guiHeight));
        } else {
            SpatialGuiPlane.clear();
        }
    }

    private static float ease(float t) {
        float t1 = t - 1.0F;
        return t1 * t1 * t1 + 1.0F;
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }
}
