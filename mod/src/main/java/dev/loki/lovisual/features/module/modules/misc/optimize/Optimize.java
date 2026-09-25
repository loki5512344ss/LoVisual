package dev.loki.lovisual.features.module.modules.misc.optimize;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.Notifier;
import dev.loki.lovisual.render.engine.optimize.OptimizeState;

/**
 * Manual FPS rubilnik: one switch plus four knobs, each cutting a named cost.
 *
 * <p>No auto-degradation by FPS — the owner's rule. Render code reads
 * {@link OptimizeState}, never this class; this module only pushes state into it.
 */
@ModuleInfo(
        id = "optimize",
        displayName = "Optimize",
        category = ModuleCategory.MISC
)
public final class Optimize extends Module {

    private final BooleanValue noGlass = bool("no_glass", true);
    private final BooleanValue litePost = bool("lite_post", true);
    private final BooleanValue liteSky = bool("lite_sky", true);
    private final BooleanValue leanHud = bool("lean_hud", true);

    private boolean notified;

    @Override
    public void onEnable() {
        pushState();
        if (!notified) {
            notified = true;
            Notifier.info(
                    "Optimize: "
                            + summaryOf(noGlass.get(), litePost.get(), liteSky.get(), leanHud.get()));
        }
    }

    @Override
    public void onDisable() {
        OptimizeState.reset();
    }

    /** Picks up knob changes made while the module stays on. */
    @Override
    public void onFrame(float d) {
        pushState();
    }

    private void pushState() {
        boolean on = isEnabled();
        OptimizeState.apply(
                on && noGlass.get(),
                on && litePost.get(),
                on && liteSky.get(),
                on && leanHud.get());
    }

    /** Compact list of what is being cut, for the first-enable notice. */
    static String summaryOf(boolean glass, boolean post, boolean sky, boolean hud) {
        StringBuilder out = new StringBuilder();
        if (glass) {
            append(out, "no glass");
        }
        if (post) {
            append(out, "light post");
        }
        if (sky) {
            append(out, "simple sky");
        }
        if (hud) {
            append(out, "lean HUD");
        }
        if (out.length() == 0) {
            return "no knobs active";
        }
        return out.toString();
    }

    private static void append(StringBuilder out, String knob) {
        if (out.length() > 0) {
            out.append(", ");
        }
        out.append(knob);
    }
}
