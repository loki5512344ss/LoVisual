/*
 * Ported/adapted from Spatial GUI (https://github.com/tastytrash/Spatial-GUI), MIT License.
 * Copyright (c) tastytrash and Spatial GUI contributors.
 *
 * This file is part of LoVisual and is distributed under LoVisual's own GPLv3 license terms
 * for the modifications made here (see mod/LICENSE and mod/THIRD_PARTY_NOTICES.md); the MIT-licensed
 * portions ported from Spatial GUI remain available under the MIT terms above.
 */
package dev.loki.lovisual.mixins.input.spatialgui;

import com.mojang.blaze3d.platform.Window;
import dev.loki.lovisual.features.module.modules.visuals.spatialgui.SpatialGui;
import dev.loki.lovisual.render.spatialgui.SpatialGuiPlane;
import net.minecraft.client.Camera;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adapted from Spatial-GUI's {@code MouseHandlerUtil}/{@code MouseHandlerMixin}: reprojects the real window
 * cursor onto the plane the open screen is currently drawn on (published every frame by {@link SpatialGuiPlane})
 * so that clicking/dragging/scrolling on the 3D screen still resolves to the right 2D GUI coordinate.
 * <p>
 * {@link MouseHandler#getScaledXPos(Window)} and {@link MouseHandler#getScaledYPos(Window)} are the single choke
 * point every mouse event (click, release, move, drag, scroll) goes through to turn the raw cursor position into
 * the coordinate handed to {@code Screen}, so overriding just these two covers all of them.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerSpatialGuiMixin {

    @Shadow
    public abstract double xpos();

    @Shadow
    public abstract double ypos();

    private final float[] lovisual$hit = new float[2];
    private long lovisual$hitStampNanos = Long.MIN_VALUE;
    private boolean lovisual$hitValid;

    @Inject(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("RETURN"), cancellable = true)
    private void lovisual$overrideScaledX(Window window, CallbackInfoReturnable<Double> cir) {
        if (lovisual$resolveHit(window)) {
            cir.setReturnValue((double) lovisual$hit[0]);
        }
    }

    @Inject(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;)D", at = @At("RETURN"), cancellable = true)
    private void lovisual$overrideScaledY(Window window, CallbackInfoReturnable<Double> cir) {
        if (lovisual$resolveHit(window)) {
            cir.setReturnValue((double) lovisual$hit[1]);
        }
    }

    /**
     * Casts a ray from the camera through the real cursor pixel and intersects it with the published spatial
     * GUI plane. Cached per-frame since X and Y are queried back-to-back for the same event.
     */
    private boolean lovisual$resolveHit(Window window) {
        SpatialGui module = SpatialGui.getInstance();
        if (module == null || !module.isCapturing()) return false;
        SpatialGuiPlane plane = SpatialGuiPlane.current();
        if (plane == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return false;

        long now = System.nanoTime();
        // getScaledXPos/getScaledYPos are always called back-to-back for the same mouse event; a ~2ms window
        // is enough to reuse one raycast for both without needing a real per-frame counter.
        if (lovisual$hitStampNanos != Long.MIN_VALUE && now - lovisual$hitStampNanos < 2_000_000L) {
            return lovisual$hitValid;
        }
        lovisual$hitStampNanos = now;

        int screenWidth = window.getScreenWidth();
        int screenHeight = window.getScreenHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            lovisual$hitValid = false;
            return false;
        }

        double pixelX = xpos();
        double pixelY = ypos();
        float ndcX = (float) (2.0 * pixelX / screenWidth - 1.0);
        float ndcY = (float) (1.0 - 2.0 * pixelY / screenHeight);

        Camera camera = mc.gameRenderer.mainCamera();
        Options options = mc.options;
        double fovDeg = options != null ? options.fov().get() : 70.0;
        float aspect = (float) screenWidth / (float) screenHeight;
        float tanHalfFov = (float) Math.tan(Math.toRadians(fovDeg) * 0.5);

        Vector3fc forward = camera.forwardVector();
        Vector3fc left = camera.leftVector();
        Vector3fc up = camera.upVector();

        Vector3f direction = new Vector3f(forward)
                .add(new Vector3f(left).mul(-ndcX * tanHalfFov * aspect))
                .add(new Vector3f(up).mul(ndcY * tanHalfFov))
                .normalize();

        Vector3f origin = new Vector3f((float) camera.position().x, (float) camera.position().y, (float) camera.position().z);

        lovisual$hitValid = plane.rayToGuiCoords(origin, direction, lovisual$hit);
        return lovisual$hitValid;
    }
}
