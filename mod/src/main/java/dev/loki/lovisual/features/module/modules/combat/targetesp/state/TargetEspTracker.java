package dev.loki.lovisual.features.module.modules.combat.targetesp.state;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import net.minecraft.world.entity.LivingEntity;

/** Follows the current TargetESP target and fades its alpha in and out. */
public final class TargetEspTracker {
    private static final float TICK_DT = 1.0f / 20.0f;
    private static final float SNAP_EPSILON = 0.02f;

    private LivingEntity renderTarget;
    private float alpha;

    public LivingEntity renderTarget() {
        return renderTarget;
    }

    public float alpha() {
        return alpha;
    }

    public boolean isVisible() {
        return renderTarget != null && alpha > 0.001f;
    }

    public void reset() {
        renderTarget = null;
        alpha = 0.0f;
    }

    /**
     * @return true when the render target switched or vanished, so per-target
     * caches must be dropped
     */
    public boolean update(LivingEntity next, float animationSpeed) {
        float dt = Math.max(AnimationUtility.deltaTime(), TICK_DT);
        if (next == null) {
            alpha = approach(0.0f, dt, animationSpeed);
            if (alpha > 0.0f) return false;
            boolean had = renderTarget != null;
            renderTarget = null;
            return had;
        }
        boolean switched = renderTarget != null && renderTarget != next;
        renderTarget = next;
        alpha = switched ? 1.0f : approach(1.0f, dt, animationSpeed);
        return switched;
    }

    private float approach(float to, float dt, float speed) {
        return AnimationUtility.snap(AnimationUtility.approach(alpha, to, dt, speed), to, SNAP_EPSILON);
    }
}
