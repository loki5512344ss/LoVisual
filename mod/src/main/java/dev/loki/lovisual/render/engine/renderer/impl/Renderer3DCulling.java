package dev.loki.lovisual.render.engine.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.core.policy.VisibilityProvider;
import dev.loki.lovisual.render.engine.core.policy.VisibilityQuery;
import dev.loki.lovisual.render.engine.math.RenderMath;

/**
 * World-primitive visibility/culling helpers for {@link Renderer3D}: frustum, section
 * and look-at tests plus line-of-sight, driven by {@link Renderer3DCullOptions}.
 * Extracted from {@code Renderer3DCulling}; Physically under {@code impl/} but
 * declares the parent package for direct access.
 */
public enum Renderer3DCulling {
    ;

    public static boolean shouldRender(Entity target, float tickDelta, Renderer3DCullOptions options) {
        if (target == null) return false;
        if (options == null || options == Renderer3DCullOptions.NONE) return true;

        AABB visibilityBox = interpolatedBox(target, tickDelta);
        if (options.frustum() && !isInFrustum(visibilityBox)) return false;
        if (options.sectionVisibility() && !isSectionVisible(visibilityBox)) return false;
        if (options.lookAtTarget() && !isInFront(target, tickDelta, options.minLookDot())) return false;

        if (options.visibleOnly()) {
            if (!(options.skipVisibilityWhenTranslucent() && RenderState.worldTranslucent)) {
                Minecraft mc = Minecraft.getInstance();
                return mc == null || mc.player == null || mc.player.hasLineOfSight(target);
            }
        }

        return true;
    }

    public static boolean isInFrustum(Entity target, float tickDelta) {
        return isInFrustum(interpolatedBox(target, tickDelta));
    }

    public static boolean isInFrustum(AABB box) {
        if (RenderState.frustum == null || box == null) return true;
        return RenderState.frustum.isVisible(box);
    }

    public static boolean isSectionVisible(AABB box) {
        if (box == null) return true;
        RenderFrameContext ctx = LoVisualRenderSystem.currentContext();
        VisibilityProvider provider = ctx != null ? ctx.visibilityProvider() : LoVisualRenderSystem.sodium().visibilityProvider();
        if (provider == null) return true;
        return provider.isBoxVisible(box, VisibilityQuery.worldOverlay(box));
    }

    private static AABB interpolatedBox(Entity target, float tickDelta) {
        AABB box = target.getBoundingBox();
        Vec3 pos = RenderMath.getLerpedPos(target, tickDelta);
        Vec3 offset = pos.subtract(target.position());
        return box.move(offset);
    }

    public static boolean isInFront(Entity target, float tickDelta, double minDot) {
        Vec3 center = getTargetCenter(target, tickDelta);
        Vec3 camPos = RenderState.cameraPos;
        Vec3 to = center.subtract(camPos);
        if (to.lengthSqr() <= 1.0e-6) return true;
        Vec3 look = RenderState.cameraLook;
        if (look.lengthSqr() <= 1.0e-6) {
            look = Vec3.directionFromRotation(RenderState.cameraPitch, RenderState.cameraYaw).normalize();
        }
        return look.dot(to.normalize()) > minDot;
    }

    private static Vec3 getTargetCenter(Entity target, float tickDelta) {
        Vec3 pos = RenderMath.getLerpedPos(target, tickDelta);
        return pos.add(0.0, target.getBbHeight() * 0.5, 0.0);
    }
}
