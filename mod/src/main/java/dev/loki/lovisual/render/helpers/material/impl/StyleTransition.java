package dev.loki.lovisual.render.helpers.material;

import dev.loki.lovisual.config.style.StyleConfig;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.style.SurfaceStyle;
import net.minecraft.util.Util;

/**
 * Cross-fade state for the global surface style. The material branch itself
 * must switch instantly (styles are not blendable), so the transition fades
 * the outgoing plate out during the first half of the interval and fades the
 * incoming plate in during the second half — a soft hand-off instead of a pop.
 * <p>
 * Lives in the {@code impl/} sibling folder to keep {@code render.helpers.material}
 * within the four-file-per-directory limit; package stays shared.
 */
final class StyleTransition {

    /** Resolved plate state: style to draw and the alpha multiplier (0..1). */
    record Display(SurfaceStyle style, float fade) {
    }

    private static boolean initialised;
    private static SurfaceStyle from = SurfaceStyle.GLASS;
    private static SurfaceStyle to = SurfaceStyle.GLASS;
    private static long startMs;
    private static boolean active;

    private StyleTransition() {
    }

    /** Current display state; cheap enough to call every frame. */
    static Display display() {
        SurfaceStyle now = StyleConfig.get().style();
        if (!initialised) {
            initialised = true;
            from = now;
            to = now;
        }
        if (now != to) {
            from = displayedStyle();
            to = now;
            startMs = Util.getMillis();
            active = StyleConfig.get().animateSwitch() && StyleConfig.get().switchMs() > 0;
        }
        if (!active) {
            return new Display(now, 1.0f);
        }
        int duration = StyleConfig.get().switchMs();
        long elapsed = Util.getMillis() - startMs;
        if (elapsed >= duration) {
            active = false;
            return new Display(to, 1.0f);
        }
        float p = progress(elapsed, duration);
        if (p < 0.5f) {
            return new Display(from, 1.0f - p * 2.0f);
        }
        return new Display(to, p * 2.0f - 1.0f);
    }

    /** Style visible at the current transition moment without consuming it. */
    private static SurfaceStyle displayedStyle() {
        if (!active) {
            return to;
        }
        int duration = StyleConfig.get().switchMs();
        long elapsed = Util.getMillis() - startMs;
        if (elapsed >= duration) {
            return to;
        }
        return progress(elapsed, duration) < 0.5f ? from : to;
    }

    private static float progress(long elapsed, int duration) {
        float linear = duration <= 0 ? 1.0f : (float) elapsed / (float) duration;
        return AnimationUtility.easeOutCubic(AnimationUtility.clamp01(linear));
    }
}
