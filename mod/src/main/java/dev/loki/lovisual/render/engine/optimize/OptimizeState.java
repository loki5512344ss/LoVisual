package dev.loki.lovisual.render.engine.optimize;

/**
 * Plain boolean holder for the manual {@code Optimize} module knobs.
 *
 * <p>Render code reads these getters instead of poking the module class, so the engine
 * never depends on {@code features.module}: the module pushes its state here on
 * enable/disable and re-syncs while it stays on.
 *
 * <p>Shape follows the house idiom of enum-utility state holders
 * ({@code PreTranslucentDepth}, {@code RenderResourceReadiness}).
 */
public enum OptimizeState {
    ;

    private static volatile boolean noGlass;
    private static volatile boolean litePost;
    private static volatile boolean liteSky;
    private static volatile boolean leanHud;

    public static boolean isNoGlass() {
        return noGlass;
    }

    public static boolean isLitePost() {
        return litePost;
    }

    public static boolean isLiteSky() {
        return liteSky;
    }

    public static boolean isLeanHud() {
        return leanHud;
    }

    public static void apply(boolean glass, boolean post, boolean sky, boolean hud) {
        noGlass = glass;
        litePost = post;
        liteSky = sky;
        leanHud = hud;
    }

    public static void reset() {
        apply(false, false, false, false);
    }
}
